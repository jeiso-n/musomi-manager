package com.musomi.desktop.service;

import java.util.List;

import com.musomi.desktop.api.ReportApi;
import com.musomi.desktop.model.ClassPerformanceSummaryResponse;
import com.musomi.desktop.model.ReportResponse;

/** Coordinates report operations for the desktop presentation layer. */
public final class ReportService {

    private final ReportApi reportApi;

    public ReportService() {
        this(new ReportApi());
    }

    ReportService(ReportApi reportApi) {
        this.reportApi = reportApi;
    }

    public List<ReportResponse> generateReports(Long classId, Long streamId, Long termId) {
        return reportApi.generateReports(classId, streamId, termId);
    }

    public ClassPerformanceSummaryResponse getClassSummary(Long classId, Long streamId, Long termId) {
        return reportApi.getClassSummary(classId, streamId, termId);
    }

    public byte[] downloadPdf(Long reportId) {
        return reportApi.downloadPdf(reportId);
    }
}
