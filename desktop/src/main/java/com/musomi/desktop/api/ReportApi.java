package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import com.musomi.desktop.model.ClassPerformanceSummaryResponse;
import com.musomi.desktop.model.ReportResponse;

/** Authenticated desktop client for report generation, summaries and PDF downloads. */
public final class ReportApi {

    private final ApiClient client = ApiClient.getInstance();

    public List<ReportResponse> generateReports(Long classId, Long streamId, Long termId) {
        GenerateReportRequest request = new GenerateReportRequest(classId, streamId, termId);
        ReportResponse[] reports = client.post("/reports/generate", request, ReportResponse[].class);
        return reports == null ? List.of() : Arrays.asList(reports);
    }

    public ClassPerformanceSummaryResponse getClassSummary(Long classId, Long streamId, Long termId) {
        StringBuilder path = new StringBuilder("/reports/class-summary?classId=")
                .append(classId);
        if (streamId != null) {
            path.append("&streamId=").append(streamId);
        }
        path.append("&termId=").append(termId);
        return client.get(path.toString(), ClassPerformanceSummaryResponse.class);
    }

    public byte[] downloadPdf(Long reportId) {
        return client.getBytes("/reports/download/" + reportId);
    }

    private record GenerateReportRequest(Long classId, Long streamId, Long termId) {
    }
}
