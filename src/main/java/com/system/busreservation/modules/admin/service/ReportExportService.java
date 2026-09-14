package com.system.busreservation.modules.admin.service;

import com.system.busreservation.modules.admin.dto.RevenueSummaryResponse;

public interface ReportExportService {

    byte[] exportRevenueCsv(RevenueSummaryResponse summary);

    byte[] exportRevenuePdf(RevenueSummaryResponse summary);
}