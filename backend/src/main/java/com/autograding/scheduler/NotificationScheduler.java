package com.autograding.scheduler;

import com.autograding.entity.Assignment;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Notification;
import com.autograding.entity.Submission;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.SubmissionMapper;
import com.autograding.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final AssignmentMapper assignmentMapper;
    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final SubmissionMapper submissionMapper;
    private final GradingResultMapper gradingResultMapper;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    public NotificationScheduler(AssignmentMapper assignmentMapper,
                                 ClassMapper classMapper,
                                 ClassStudentMapper classStudentMapper,
                                 SubmissionMapper submissionMapper,
                                 GradingResultMapper gradingResultMapper,
                                 NotificationService notificationService,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.assignmentMapper = assignmentMapper;
        this.classMapper = classMapper;
        this.classStudentMapper = classStudentMapper;
        this.submissionMapper = submissionMapper;
        this.gradingResultMapper = gradingResultMapper;
        this.notificationService = notificationService;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 900_000, initialDelay = 30_000) // every 15 min
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24h = now.plusHours(24);
        LocalDateTime in1h = now.plusHours(1);
        LocalDateTime fiveMinAgo = now.minusMinutes(5);

        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assignment::getStatus, Assignment.Status.PUBLISHED)
               .between(Assignment::getEndTime, fiveMinAgo, in24h);
        List<Assignment> assignments = assignmentMapper.selectList(wrapper);

        for (Assignment assignment : assignments) {
            boolean isUrgent = assignment.getEndTime().isBefore(in1h) || assignment.getEndTime().isEqual(in1h);
            String redisKey = isUrgent
                    ? "reminder_1h:assignment_" + assignment.getId()
                    : "reminder_24h:assignment_" + assignment.getId();

            Boolean notified = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(notified)) continue;

            notifyStudentsInCourse(assignment, isUrgent);
            redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
        }
    }

    private void notifyStudentsInCourse(Assignment assignment, boolean urgent) {
        Long courseId = assignment.getCourseId();
        List<com.autograding.entity.Class> classes = classMapper.selectList(
                new LambdaQueryWrapper<com.autograding.entity.Class>()
                        .eq(com.autograding.entity.Class::getCourseId, courseId));

        for (com.autograding.entity.Class clazz : classes) {
            List<ClassStudent> enrollments = classStudentMapper.selectList(
                    new LambdaQueryWrapper<ClassStudent>()
                            .eq(ClassStudent::getClassId, clazz.getId()));

            for (ClassStudent cs : enrollments) {
                String title = urgent ? "作业即将截止" : "作业截止提醒";
                String content = urgent
                        ? String.format("作业「%s」将在1小时内截止，请尽快提交！", assignment.getTitle())
                        : String.format("作业「%s」将在24小时内截止，请安排好时间完成。截止时间：%s",
                                assignment.getTitle(), assignment.getEndTime().toString());
                notificationService.createNotification(cs.getStudentId(), title, content,
                        Notification.Type.ASSIGNMENT, assignment.getId());
            }
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000) // every 5 min
    public void markLateSubmissions() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assignment::getStatus, Assignment.Status.PUBLISHED)
               .le(Assignment::getEndTime, now);
        List<Assignment> ended = assignmentMapper.selectList(wrapper);

        for (Assignment assignment : ended) {
            LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.eq(Submission::getAssignmentId, assignment.getId())
                      .eq(Submission::getIsLate, 0);
            List<Submission> submissions = submissionMapper.selectList(subWrapper);

            for (Submission sub : submissions) {
                if (sub.getSubmittedAt() != null && sub.getSubmittedAt().isAfter(assignment.getEndTime())) {
                    sub.setIsLate(1);
                    submissionMapper.updateById(sub);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1_800_000, initialDelay = 120_000)
    public void checkStuckGradingTasks() {
        LocalDateTime thirtyMinAgo = LocalDateTime.now().minusMinutes(30);

        LambdaQueryWrapper<GradingResult> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.eq(GradingResult::getGradingStatus, GradingResult.GradingStatus.PROCESSING);
        List<GradingResult> stuckProcessing = gradingResultMapper.selectList(processingWrapper);

        for (GradingResult gr : stuckProcessing) {
            gr.setGradingStatus(GradingResult.GradingStatus.PENDING);
            gradingResultMapper.updateById(gr);
            log.info("Reset stuck PROCESSING task id={} back to PENDING", gr.getId());
        }

        LambdaQueryWrapper<GradingResult> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(GradingResult::getGradingStatus, GradingResult.GradingStatus.PENDING)
                      .isNotNull(GradingResult::getGradedAt);
        List<GradingResult> stuckPending = gradingResultMapper.selectList(pendingWrapper);

        long stuckCount = stuckPending.stream()
                .filter(g -> g.getGradedAt() != null && g.getGradedAt().isBefore(thirtyMinAgo))
                .count();

        if (stuckCount > 0) {
            log.warn("Found {} stuck grading tasks (PENDING > 30 min)", stuckCount);
        }

        if (!stuckProcessing.isEmpty() || stuckCount > 0) {
            log.info("Stuck task recovery: reset {} PROCESSING tasks, found {} stale PENDING tasks",
                    stuckProcessing.size(), stuckCount);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // daily at 2am
    public void archiveExpiredAssignments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assignment::getStatus, Assignment.Status.PUBLISHED)
               .le(Assignment::getEndTime, sevenDaysAgo);
        List<Assignment> expired = assignmentMapper.selectList(wrapper);

        for (Assignment a : expired) {
            a.setStatus(Assignment.Status.ARCHIVED);
            a.setUpdatedAt(now);
            assignmentMapper.updateById(a);
        }

        if (!expired.isEmpty()) {
            log.info("Archived {} expired assignments", expired.size());
        }
    }
}
