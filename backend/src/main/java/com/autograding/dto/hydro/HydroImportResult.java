package com.autograding.dto.hydro;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HydroImportResult {

    private int totalFound;
    private int successCount;
    private int failCount;
    private List<ProblemImportDetail> details = new ArrayList<>();

    @Data
    public static class ProblemImportDetail {
        private String title;
        private boolean success;
        private String errorMessage;
        private Long problemId;
        private int testCaseCount;
        private int imageCount;
    }
}
