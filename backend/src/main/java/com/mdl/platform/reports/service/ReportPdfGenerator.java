package com.mdl.platform.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ReportPdfGenerator {

    private static final Color BRAND_ORANGE = new Color(245, 166, 35);
    private static final Color HEADER_BG = new Color(24, 32, 44);
    private static final Color MUTED = new Color(120, 130, 145);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    public byte[] salesSummaryPdf(
            String businessName,
            String currencyCode,
            Map<String, String> metrics,
            Instant generatedAt) {
        return buildReport(
                businessName,
                "Sales Summary Report",
                "Currency: " + currencyCode,
                generatedAt,
                List.of("Metric", "Value"),
                metrics.entrySet().stream()
                        .map(entry -> List.of(formatMetricLabel(entry.getKey()), entry.getValue()))
                        .toList());
    }

    public byte[] tabularPdf(
            String businessName,
            String title,
            String subtitle,
            Instant generatedAt,
            List<String> headers,
            List<List<String>> rows) {
        return buildReport(businessName, title, subtitle, generatedAt, headers, rows);
    }

    private byte[] buildReport(
            String businessName,
            String title,
            String subtitle,
            Instant generatedAt,
            List<String> headers,
            List<List<String>> rows) {
        Document document = new Document(PageSize.A4, 36, 36, 48, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_ORANGE);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(210, 215, 222));
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 35, 42));

            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(HEADER_BG);
            bannerCell.setBorder(PdfPCell.NO_BORDER);
            bannerCell.setPadding(14);
            bannerCell.addElement(new Paragraph("modern DL", brandFont));
            bannerCell.addElement(new Paragraph(businessName, subtitleFont));
            bannerCell.addElement(new Paragraph(title, titleFont));
            bannerCell.addElement(new Paragraph(subtitle, subtitleFont));
            banner.addCell(bannerCell);
            document.add(banner);

            document.add(new Paragraph("Generated " + DISPLAY.format(generatedAt), metaFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setHeaderRows(1);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(45, 55, 72));
                cell.setPadding(8);
                cell.setBorderColor(new Color(60, 70, 86));
                table.addCell(cell);
            }

            boolean stripe = false;
            for (List<String> row : rows) {
                Color rowBg = stripe ? new Color(245, 247, 250) : Color.WHITE;
                stripe = !stripe;
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", cellFont));
                    cell.setBackgroundColor(rowBg);
                    cell.setPadding(7);
                    cell.setBorderColor(new Color(220, 225, 232));
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate PDF report", ex);
        }
    }

    private String formatMetricLabel(String key) {
        return key.replace('_', ' ');
    }
}
