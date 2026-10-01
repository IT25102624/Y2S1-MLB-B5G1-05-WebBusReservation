package com.system.busreservation.modules.admin.service.impl;

import com.system.busreservation.modules.admin.dto.DailyRevenuePoint;
import com.system.busreservation.modules.admin.dto.RevenueSummaryResponse;
import com.system.busreservation.modules.admin.service.ReportExportService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    @Override
    public byte[] exportRevenueCsv(RevenueSummaryResponse summary) {

        StringBuilder csv = new StringBuilder();

        csv.append("Revenue & Occupancy Report\n");
        csv.append("\n");

        csv.append("Total Revenue,")
                .append(summary.getTotalRevenue())
                .append("\n");

        csv.append("Total Bookings,")
                .append(summary.getTotalBookings())
                .append("\n");

        csv.append("Total Seats Sold,")
                .append(summary.getTotalSeatsSold())
                .append("\n");

        csv.append("Occupancy Rate (%),")
                .append(summary.getOccupancyRatePercent())
                .append("\n");

        csv.append("\n");
        csv.append("Date,Revenue,Bookings\n");

        List<DailyRevenuePoint> dailyTrend =
                summary.getDailyTrend();

        if (dailyTrend != null) {
            for (DailyRevenuePoint point : dailyTrend) {

                csv.append(point.getDate())
                        .append(",")
                        .append(point.getRevenue())
                        .append(",")
                        .append(point.getBookingCount())
                        .append("\n");
            }
        }

        return csv.toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportRevenuePdf(RevenueSummaryResponse summary) {

        StringBuilder content = new StringBuilder();

        content.append("Revenue & Occupancy Report\n");
        content.append("===========================\n\n");

        content.append("Total Revenue: ")
                .append(summary.getTotalRevenue())
                .append("\n");

        content.append("Total Bookings: ")
                .append(summary.getTotalBookings())
                .append("\n");

        content.append("Total Seats Sold: ")
                .append(summary.getTotalSeatsSold())
                .append("\n");

        content.append("Occupancy Rate: ")
                .append(summary.getOccupancyRatePercent())
                .append("%\n\n");

        content.append("Daily Revenue\n");
        content.append("---------------------------\n");

        List<DailyRevenuePoint> dailyTrend =
                summary.getDailyTrend();

        if (dailyTrend != null) {

            for (DailyRevenuePoint point : dailyTrend) {

                content.append("Date: ")
                        .append(point.getDate())
                        .append(" | Revenue: ")
                        .append(point.getRevenue())
                        .append(" | Bookings: ")
                        .append(point.getBookingCount())
                        .append("\n");
            }
        }

        return createSimplePdf(content.toString());
    }

    /**
     * Creates a simple valid PDF without using
     * any external PDF library.
     */
    private byte[] createSimplePdf(String text) {

        String[] lines = text.split("\n");

        StringBuilder pdfContent = new StringBuilder();

        pdfContent.append("BT\n");
        pdfContent.append("/F1 11 Tf\n");
        pdfContent.append("50 750 Td\n");

        for (String line : lines) {

            String safeLine = line
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");

            pdfContent.append("(")
                    .append(safeLine)
                    .append(") Tj\n");

            pdfContent.append("0 -16 Td\n");
        }

        pdfContent.append("ET\n");

        byte[] stream =
                pdfContent.toString()
                        .getBytes(StandardCharsets.ISO_8859_1);

        StringBuilder pdf = new StringBuilder();

        pdf.append("%PDF-1.4\n");

        // FIX:
        // We have PDF objects 1, 2, 3, 4 and 5.
        // Index 0 is unused.
        // Therefore array size must be 6.
        int[] offsets = new int[6];

        offsets[1] = pdf.length();
        pdf.append("1 0 obj\n");
        pdf.append("<< /Type /Catalog /Pages 2 0 R >>\n");
        pdf.append("endobj\n");

        offsets[2] = pdf.length();
        pdf.append("2 0 obj\n");
        pdf.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
        pdf.append("endobj\n");

        offsets[3] = pdf.length();
        pdf.append("3 0 obj\n");
        pdf.append("<< /Type /Page /Parent 2 0 R ");
        pdf.append("/MediaBox [0 0 595 842] ");
        pdf.append("/Resources << /Font << /F1 4 0 R >> >> ");
        pdf.append("/Contents 5 0 R >>\n");
        pdf.append("endobj\n");

        offsets[4] = pdf.length();
        pdf.append("4 0 obj\n");
        pdf.append("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n");
        pdf.append("endobj\n");

        offsets[5] = pdf.length();
        pdf.append("5 0 obj\n");
        pdf.append("<< /Length ")
                .append(stream.length)
                .append(" >>\n");
        pdf.append("stream\n");

        String header = pdf.toString();

        byte[] headerBytes =
                header.getBytes(StandardCharsets.ISO_8859_1);

        byte[] streamStart =
                "\n".getBytes(StandardCharsets.ISO_8859_1);

        byte[] streamEnd =
                "\nendstream\nendobj\n"
                        .getBytes(StandardCharsets.ISO_8859_1);

        byte[] xrefAndTrailer =
                createXrefAndTrailer(
                        offsets,
                        headerBytes.length
                                + streamStart.length
                                + stream.length
                                + streamEnd.length
                );

        byte[] result =
                new byte[
                        headerBytes.length
                                + streamStart.length
                                + stream.length
                                + streamEnd.length
                                + xrefAndTrailer.length
                        ];

        int position = 0;

        System.arraycopy(
                headerBytes,
                0,
                result,
                position,
                headerBytes.length
        );

        position += headerBytes.length;

        System.arraycopy(
                streamStart,
                0,
                result,
                position,
                streamStart.length
        );

        position += streamStart.length;

        System.arraycopy(
                stream,
                0,
                result,
                position,
                stream.length
        );

        position += stream.length;

        System.arraycopy(
                streamEnd,
                0,
                result,
                position,
                streamEnd.length
        );

        position += streamEnd.length;

        System.arraycopy(
                xrefAndTrailer,
                0,
                result,
                position,
                xrefAndTrailer.length
        );

        return result;
    }

    private byte[] createXrefAndTrailer(
            int[] offsets,
            int xrefPosition) {

        StringBuilder result = new StringBuilder();

        result.append("xref\n");
        result.append("0 6\n");

        result.append(
                "0000000000 65535 f \n"
        );

        for (int i = 1; i <= 5; i++) {

            result.append(
                    String.format(
                            "%010d 00000 n \n",
                            offsets[i]
                    )
            );
        }

        result.append("trailer\n");
        result.append(
                "<< /Size 6 /Root 1 0 R >>\n"
        );

        result.append("startxref\n");
        result.append(xrefPosition);
        result.append("\n%%EOF\n");

        return result.toString()
                .getBytes(StandardCharsets.ISO_8859_1);
    }
}