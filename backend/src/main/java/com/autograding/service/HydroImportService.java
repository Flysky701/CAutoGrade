package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.hydro.HydroImportResult;
import com.autograding.dto.hydro.HydroImportResult.ProblemImportDetail;
import com.autograding.entity.Problem;
import com.autograding.entity.TestCase;
import com.autograding.mapper.ProblemMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class HydroImportService {

    private static final Logger log = LoggerFactory.getLogger(HydroImportService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ProblemService problemService;
    private final TestCaseService testCaseService;
    private final FileService fileService;
    private final ProblemMapper problemMapper;
    private final HydroImportService self;

    public HydroImportService(ProblemService problemService, TestCaseService testCaseService,
                              FileService fileService, ProblemMapper problemMapper,
                              @Lazy HydroImportService self) {
        this.problemService = problemService;
        this.testCaseService = testCaseService;
        this.fileService = fileService;
        this.problemMapper = problemMapper;
        this.self = self;
    }

    // ─── Public Entry ──────────────────────────────────────────────

    public HydroImportResult importFromZip(MultipartFile zipFile, Long creatorId) {
        HydroImportResult result = new HydroImportResult();
        Path tempDir = null;
        Path tempZip = null;

        try {
            tempDir = Files.createTempDirectory("hydro-import-");
            tempZip = Files.createTempFile("hydro-import-", ".zip");
            zipFile.transferTo(tempZip.toFile());
            extractZip(tempZip, tempDir);

            List<Path> problemDirs = findProblemDirs(tempDir);
            result.setTotalFound(problemDirs.size());
            result.setDetails(new ArrayList<>());

            for (Path dir : problemDirs) {
                try {
                    ProblemImportDetail detail = self.importSingleProblem(dir, creatorId);
                    result.getDetails().add(detail);
                    if (detail.isSuccess()) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else {
                        result.setFailCount(result.getFailCount() + 1);
                    }
                } catch (Exception e) {
                    log.error("Failed to import problem from {}", dir, e);
                    ProblemImportDetail detail = new ProblemImportDetail();
                    detail.setTitle(dir.getFileName().toString());
                    detail.setSuccess(false);
                    detail.setErrorMessage("导入异常: " + e.getMessage());
                    result.getDetails().add(detail);
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
        } catch (IOException e) {
            throw new BusinessException(500, "ZIP 文件解压失败: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
            if (tempZip != null) {
                try { Files.deleteIfExists(tempZip); } catch (Exception ignored) {}
            }
        }

        return result;
    }

    // ─── ZIP Extraction ────────────────────────────────────────────

    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipFile zf = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir)) {
                    throw new BusinessException(400, "ZIP 文件包含非法路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (InputStream is = zf.getInputStream(entry)) {
                        Files.copy(is, resolved, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    // ─── Directory Discovery ───────────────────────────────────────

    private List<Path> findProblemDirs(Path root) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 4, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().equals("problem.yaml")) {
                    result.add(file.getParent());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    // ─── Single Problem Import (Transactional) ─────────────────────

    @Transactional
    public ProblemImportDetail importSingleProblem(Path versionDir, Long creatorId) {
        ProblemImportDetail detail = new ProblemImportDetail();

        try {
            // 1. Parse problem.yaml
            Path yamlPath = versionDir.resolve("problem.yaml");
            Map<String, Object> yamlData = parseYaml(yamlPath);
            String title = (String) yamlData.getOrDefault("title", versionDir.getFileName().toString());
            detail.setTitle(title);

            // 2. Parse problem_zh.md
            Path mdPath = versionDir.resolve("problem_zh.md");
            Path additionalDir = versionDir.resolve("additional_file");
            HydroContent content = parseProblemMd(mdPath, additionalDir);

            // 3. Process images
            int imageCount = 0;
            Map<String, String> imageMapping = new HashMap<>();
            if (Files.isDirectory(additionalDir)) {
                imageMapping = processImages(additionalDir);
                imageCount = imageMapping.size();
                // Replace file:// references in markdown
                content.description = replaceImageRefs(content.description, imageMapping);
                content.inputDesc = replaceImageRefs(content.inputDesc, imageMapping);
                content.outputDesc = replaceImageRefs(content.outputDesc, imageMapping);
            }

            // 4. Duplicate detection — append suffix if needed
            title = deduplicateTitle(title, creatorId);

            // 5. Build Problem entity
            Problem problem = new Problem();
            problem.setTitle(title);
            problem.setDescription(content.description);
            problem.setInputDesc(content.inputDesc);
            problem.setOutputDesc(content.outputDesc);
            problem.setDifficulty(1);

            // Knowledge tags from yaml
            Object tagObj = yamlData.get("tag");
            List<String> tags = parseTagList(tagObj);
            try {
                problem.setKnowledgeTags(objectMapper.writeValueAsString(tags));
            } catch (JsonProcessingException e) {
                problem.setKnowledgeTags("[]");
            }

            Problem saved = problemService.createProblem(problem, creatorId);

            // 6. Parse and save test cases
            Path testdataDir = versionDir.resolve("testdata");
            List<TestCasePair> testCases = parseTestCases(testdataDir);
            int tcCount = 0;
            for (int i = 0; i < testCases.size(); i++) {
                TestCasePair pair = testCases.get(i);
                TestCase tc = new TestCase();
                tc.setProblemId(saved.getId());
                tc.setInputData(pair.input);
                tc.setExpectedOutput(pair.output);
                tc.setIsHidden(1);
                tc.setWeight(10);
                tc.setSortOrder(i);
                testCaseService.createTestCase(tc);
                tcCount++;
            }

            detail.setSuccess(true);
            detail.setProblemId(saved.getId());
            detail.setTestCaseCount(tcCount);
            detail.setImageCount(imageCount);

        } catch (Exception e) {
            log.error("Import single problem failed: {}", versionDir, e);
            detail.setSuccess(false);
            detail.setErrorMessage(e.getMessage());
        }

        return detail;
    }

    // ─── YAML Parsing ──────────────────────────────────────────────

    private Map<String, Object> parseYaml(Path yamlPath) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = Files.newInputStream(yamlPath)) {
            Map<String, Object> data = yaml.load(is);
            return data != null ? data : Collections.emptyMap();
        }
    }

    private List<String> parseTagList(Object tagObj) {
        if (tagObj instanceof List) {
            return (List<String>) tagObj;
        }
        if (tagObj instanceof String) {
            String s = ((String) tagObj).trim();
            if (s.isEmpty() || s.equals("[]")) return Collections.emptyList();
            return Arrays.stream(s.split("[,，]")).map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // ─── Markdown Parsing ──────────────────────────────────────────

    private static class HydroContent {
        String description = "";
        String inputDesc = "";
        String outputDesc = "";
    }

    private HydroContent parseProblemMd(Path mdPath, Path additionalDir) throws IOException {
        HydroContent content = new HydroContent();
        if (!Files.exists(mdPath)) {
            content.description = "（无题目描述）";
            return content;
        }

        String raw = Files.readString(mdPath, StandardCharsets.UTF_8);
        // Remove Hydro comment lines: [^_^]:...
        raw = raw.replaceAll("(?m)^\\[\\^_\\^\\]:.*$", "");

        // Split by section headers (# or ##)
        Map<String, StringBuilder> sections = splitIntoSections(raw);

        content.description = sections.containsKey("题目描述") ? sections.get("题目描述").toString().trim() : "";
        content.inputDesc = sections.containsKey("输入格式") ? sections.get("输入格式").toString().trim() : "";
        content.outputDesc = sections.containsKey("输出格式") ? sections.get("输出格式").toString().trim() : "";

        // Append sample + hints to description
        StringBuilder fullDesc = new StringBuilder(content.description);
        if (sections.containsKey("样例")) {
            fullDesc.append("\n\n## 样例\n\n").append(sections.get("样例").toString().trim());
        }
        if (sections.containsKey("数据范围与提示")) {
            fullDesc.append("\n\n## 数据范围与提示\n\n").append(sections.get("数据范围与提示").toString().trim());
        }
        if (sections.containsKey("提示")) {
            fullDesc.append("\n\n## 提示\n\n").append(sections.get("提示").toString().trim());
        }

        // If no structured sections found, use entire content as description
        if (content.description.isEmpty() && content.inputDesc.isEmpty() && content.outputDesc.isEmpty()) {
            content.description = raw.trim();
        } else {
            content.description = fullDesc.toString().trim();
        }

        // Convert Hydro code blocks
        content.description = convertHydroCodeBlocks(content.description);
        content.inputDesc = convertHydroCodeBlocks(content.inputDesc);
        content.outputDesc = convertHydroCodeBlocks(content.outputDesc);

        return content;
    }

    private Map<String, StringBuilder> splitIntoSections(String markdown) {
        Map<String, StringBuilder> sections = new LinkedHashMap<>();
        // Match # Title or ## Title (1-2 hashes)
        Pattern headerPattern = Pattern.compile("(?m)^#{1,2}\\s+(.+)$");
        String[] lines = markdown.split("\\n");
        String currentKey = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            Matcher m = headerPattern.matcher(line);
            if (m.matches()) {
                // Save previous section
                if (currentKey != null) {
                    sections.put(currentKey, currentContent);
                }
                String headerText = m.group(1).trim();
                // Normalize section name
                currentKey = normalizeSectionName(headerText);
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }
        // Save last section
        if (currentKey != null) {
            sections.put(currentKey, currentContent);
        }
        return sections;
    }

    private String normalizeSectionName(String header) {
        if (header.contains("题目描述") || header.contains("Description")) return "题目描述";
        if (header.contains("输入格式") || header.contains("输入") || header.contains("Input")) return "输入格式";
        if (header.contains("输出格式") || header.contains("输出") || header.contains("Output")) return "输出格式";
        if (header.contains("样例") || header.contains("Sample") || header.contains("示例")) return "样例";
        if (header.contains("数据范围") || header.contains("提示") || header.contains("Hint")) return "数据范围与提示";
        return header; // Return original for unknown sections
    }

    /**
     * Convert Hydro ```inputN / ```outputN code blocks to standard Markdown.
     */
    private String convertHydroCodeBlocks(String markdown) {
        if (markdown == null || markdown.isEmpty()) return markdown;

        // Match ```inputN or ```outputN
        Pattern blockStart = Pattern.compile("(?m)^```(input|output)(\\d+)\\s*$");
        // Match closing ```
        Pattern blockEnd = Pattern.compile("(?m)^```\\s*$");

        StringBuilder result = new StringBuilder();
        String[] lines = markdown.split("\\n");
        int i = 0;

        while (i < lines.length) {
            Matcher startMatcher = blockStart.matcher(lines[i]);
            if (startMatcher.matches()) {
                String type = startMatcher.group(1); // "input" or "output"
                int num = Integer.parseInt(startMatcher.group(2));

                // Collect block content until closing ``` or next input/output block
                StringBuilder blockContent = new StringBuilder();
                i++;
                while (i < lines.length) {
                    Matcher endMatcher = blockEnd.matcher(lines[i]);
                    Matcher nextStart = blockStart.matcher(lines[i]);
                    if (endMatcher.matches() || nextStart.matches()) {
                        if (endMatcher.matches()) i++; // skip closing ```
                        break;
                    }
                    blockContent.append(lines[i]).append("\n");
                    i++;
                }

                // Convert to standard format
                String label = type.equals("input") ? "样例输入 " + num : "样例输出 " + num;
                result.append("**").append(label).append("：**\n");
                result.append("```text\n");
                result.append(blockContent);
                if (!blockContent.toString().endsWith("\n")) {
                    result.append("\n");
                }
                result.append("```\n");
            } else {
                result.append(lines[i]).append("\n");
                i++;
            }
        }

        return result.toString().stripTrailing();
    }

    // ─── Test Data Parsing ─────────────────────────────────────────

    private static class TestCasePair {
        String input;
        String output;

        TestCasePair(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    private List<TestCasePair> parseTestCases(Path testdataDir) throws IOException {
        List<TestCasePair> pairs = new ArrayList<>();
        if (!Files.isDirectory(testdataDir)) return pairs;

        // Collect all .in files
        Map<String, Path> inFiles = new HashMap<>();
        Map<String, Path> outFiles = new HashMap<>();

        try (var stream = Files.list(testdataDir)) {
            stream.forEach(path -> {
                String name = path.getFileName().toString();
                if (name.endsWith(".in")) {
                    inFiles.put(name.substring(0, name.length() - 3), path);
                } else if (name.endsWith(".out")) {
                    outFiles.put(name.substring(0, name.length() - 4), path);
                }
            });
        }

        // Match .in/.out pairs and sort
        List<String> baseNames = new ArrayList<>(inFiles.keySet());
        baseNames.sort((a, b) -> {
            // Try numeric sort first
            try {
                int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });

        for (String baseName : baseNames) {
            Path inPath = inFiles.get(baseName);
            Path outPath = outFiles.get(baseName);
            if (outPath == null) {
                log.warn("Missing .out file for test case: {}", baseName);
                continue;
            }
            String input = Files.readString(inPath, StandardCharsets.UTF_8);
            String output = Files.readString(outPath, StandardCharsets.UTF_8);
            pairs.add(new TestCasePair(input, output));
        }

        return pairs;
    }

    // ─── Image Processing ──────────────────────────────────────────

    private Map<String, String> processImages(Path additionalDir) throws IOException {
        Map<String, String> mapping = new HashMap<>();
        if (!Files.isDirectory(additionalDir)) return mapping;

        Set<String> imageExtensions = Set.of(".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp");

        try (var stream = Files.list(additionalDir)) {
            stream.forEach(path -> {
                String name = path.getFileName().toString().toLowerCase();
                boolean isImage = imageExtensions.stream().anyMatch(name::endsWith);
                if (!isImage) return;

                try {
                    byte[] bytes = Files.readAllBytes(path);
                    String originalName = path.getFileName().toString();
                    String storedName = fileService.storeFileBytes(originalName, bytes);
                    mapping.put(originalName, storedName);
                } catch (Exception e) {
                    log.warn("Failed to process image: {}", path, e);
                }
            });
        }
        return mapping;
    }

    private String replaceImageRefs(String markdown, Map<String, String> imageMapping) {
        if (markdown == null || imageMapping.isEmpty()) return markdown;
        String result = markdown;
        for (Map.Entry<String, String> entry : imageMapping.entrySet()) {
            // Replace file://xxx with /api/files/storedName
            result = result.replace("file://" + entry.getKey(),
                    "/api/files/" + entry.getValue());
        }
        return result;
    }

    // ─── Duplicate Detection ───────────────────────────────────────

    private String deduplicateTitle(String title, Long creatorId) {
        String baseTitle = title;
        int suffix = 2;
        while (true) {
            Long count = problemMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Problem>()
                            .eq(Problem::getCreatorId, creatorId)
                            .eq(Problem::getTitle, title)
                            .eq(Problem::getDeleted, 0));
            if (count == 0) return title;
            title = baseTitle + "（导入-" + suffix + "）";
            suffix++;
        }
    }

    // ─── Utility ───────────────────────────────────────────────────

    private void deleteDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory: {}", dir, e);
        }
    }
}
