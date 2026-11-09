package com.musomi.manager.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.awt.Color;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.musomi.manager.entity.Comment;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.SchoolSettings;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Generates student report card PDFs. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private static final Color PRIMARY = new Color(0x4F, 0x46, 0xE5);
    private static final Color SLATE_900 = new Color(0x0F, 0x17, 0x2A);
    private static final Color SLATE_50 = new Color(0xF8, 0xFA, 0xFC);
    private static final Color SLATE_200 = new Color(0xE2, 0xE8, 0xF0);
    private static final Color SLATE_500 = new Color(0x64, 0x74, 0x8B);

    /** PDF subject summary data. */
    public record SubjectAverage(String subjectName, BigDecimal average, String grade,
                                 int assessmentCount, int expectedAssessmentCount) {
        public SubjectAverage(String subjectName, BigDecimal average, String grade, int assessmentCount) {
            this(subjectName, average, grade, assessmentCount, assessmentCount);
        }
    }

    /** Generates a one-page A4 report card PDF for a student and term. */
    public byte[] generateReportCard(Student student, Term term, School school, SchoolSettings settings,
                                     List<SubjectAverage> subjects, BigDecimal overallAverage,
                                     String overallGrade, int position, int classSize, List<Comment> comments) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();
            addReportContents(document, student, term, school, settings, subjects, overallAverage, overallGrade,
                    position, classSize, comments);
            document.close();
            log.info("Generated report card for student {} in term {}",
                    value(student == null ? null : student.getFullName(), "unknown"),
                    value(term == null ? null : term.getName(), "unknown"));
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Unable to generate student report card PDF", exception);
        }
    }

    private void addReportContents(Document document, Student student, Term term, School school,
                                   SchoolSettings settings,
                                   List<SubjectAverage> subjects, BigDecimal overallAverage,
                                   String overallGrade, int position, int classSize, List<Comment> comments)
            throws DocumentException {
        Font schoolFont = FontFactory.getFont(FontFactory.HELVETICA, 17, Font.BOLD, PRIMARY);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, SLATE_900);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, SLATE_900);
        Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, SLATE_500);
        String schoolName = value(school == null ? null : school.getName(), "School");
        Paragraph schoolHeading = new Paragraph(schoolName, schoolFont);
        schoolHeading.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolHeading);
        addBrandDivider(document);
        if (settings != null && hasText(settings.getReportHeader())) {
            Paragraph header = new Paragraph(settings.getReportHeader(), mutedFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
        }
        document.add(new Paragraph(" "));
        String termName = value(term == null ? null : term.getName(), "Term");
        Integer academicYear = term == null || term.getAcademicYear() == null
                ? null
                : term.getAcademicYear().getYear();
        document.add(new Paragraph("Report Card — " + termName
                + (academicYear == null ? "" : " " + academicYear), titleFont));
        document.add(new Paragraph("Student: " + value(student == null ? null : student.getFullName(), ""), bodyFont));
        document.add(new Paragraph("Admission Number: "
                + value(student == null ? null : student.getAdmissionNumber(), ""), bodyFont));
        document.add(new Paragraph("Class: " + className(student), bodyFont));
        document.add(new Paragraph("Stream: " + streamName(student), bodyFont));
        document.add(new Paragraph(" "));
        addSubjectTable(document, subjects);

        String reportStatus = subjects == null || subjects.isEmpty()
                ? "NO PUBLISHED RESULTS"
                : position > 0 ? "COMPLETE" : "PARTIAL - MARKS INCOMPLETE";
        String positionText = position > 0 ? position + "/" + classSize : "Not ranked";
        document.add(new Paragraph("Report Status: " + reportStatus, bodyFont));
        document.add(new Paragraph("Overall Average: " + format(overallAverage)
                + "   Overall Grade: " + value(overallGrade, "N/A")
                + "   Position: " + positionText, bodyFont));
        addComments(document, comments);
        if (settings != null && hasText(settings.getReportFooter())) {
            document.add(new Paragraph(settings.getReportFooter(), mutedFont));
        }
        document.add(new Paragraph("Generated by Musomi Manager on " + LocalDate.now(), mutedFont));
    }

    private void addSubjectTable(Document document, List<SubjectAverage> subjects) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {4, 2, 1, 1, 1});
        addHeaderCell(table, "Subject");
        addHeaderCell(table, "Average");
        addHeaderCell(table, "Grade");
        addHeaderCell(table, "Recorded");
        addHeaderCell(table, "Expected");
        boolean alternate = false;
        if (subjects != null) {
            for (SubjectAverage subject : subjects) {
                if (subject == null) {
                    continue;
                }

                Color rowColor = alternate ? SLATE_50 : Color.WHITE;
                addCell(table, value(subject.subjectName(), ""), rowColor, Element.ALIGN_LEFT);
                addCell(table, format(subject.average()), rowColor, Element.ALIGN_RIGHT);
                addGradeCell(table, value(subject.grade(), "N/A"));
                addCell(table, Integer.toString(subject.assessmentCount()), rowColor, Element.ALIGN_RIGHT);
                addCell(table, Integer.toString(subject.expectedAssessmentCount()), rowColor, Element.ALIGN_RIGHT);
                alternate = !alternate;
            }
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addBrandDivider(Document document) throws DocumentException {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PRIMARY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(3);
        divider.addCell(cell);
        document.add(divider);
    }

    private void addComments(Document document, List<Comment> comments) throws DocumentException {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        document.add(new Paragraph("Comments"));
        for (Comment comment : comments) {
            if (comment == null) {
                continue;
            }
            String label = commentLabel(comment);
            document.add(new Paragraph("[" + label + "] " + value(comment.getCommentText(), "")));
        }
    }

    private String commentLabel(Comment comment) {
        String type = comment.getCommentType();
        if ("SUBJECT".equalsIgnoreCase(type)) {
            return comment.getSubject() == null ? "Subject" : value(comment.getSubject().getName(), "Subject");
        }
        if ("HEAD_TEACHER".equalsIgnoreCase(type)) {
            return "Head Teacher";
        }
        return "Class Teacher";
    }

    private String className(Student student) {
        return student == null || student.getCurrentClass() == null
                ? ""
                : value(student.getCurrentClass().getName(), "");
    }

    private String streamName(Student student) {
        return student == null || student.getCurrentStream() == null
                ? ""
                : value(student.getCurrentStream().getName(), "");
    }

    private void addCell(PdfPTable table, String text) {
        addCell(table, text, Color.WHITE, Element.ALIGN_LEFT);
    }

    private void addCell(PdfPTable table, String text, Color background, int alignment) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, SLATE_900);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(SLATE_200);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY);
        cell.setBorderColor(SLATE_200);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addGradeCell(PdfPTable table, String grade) {
        Color gradeColor = gradeTextColor(grade);
        Color gradeBackground = gradeBackgroundColor(grade);
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, gradeColor);
        PdfPCell cell = new PdfPCell(new Phrase(grade, font));
        cell.setBackgroundColor(gradeBackground);
        cell.setBorderColor(SLATE_200);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private Color gradeTextColor(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> new Color(0x04, 0x78, 0x57);
            case "B" -> new Color(0x1D, 0x4E, 0xD8);
            case "C" -> new Color(0xB4, 0x53, 0x09);
            case "D" -> new Color(0xC2, 0x41, 0x0C);
            case "E" -> new Color(0xB9, 0x1C, 0x1C);
            case "F" -> new Color(0x99, 0x1B, 0x1B);
            default -> SLATE_900;
        };
    }

    private Color gradeBackgroundColor(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> new Color(0xEC, 0xFD, 0xF5);
            case "B" -> new Color(0xEF, 0xF6, 0xFF);
            case "C" -> new Color(0xFF, 0xFB, 0xEB);
            case "D" -> new Color(0xFF, 0xF7, 0xED);
            case "E" -> new Color(0xFE, 0xF2, 0xF2);
            case "F" -> new Color(0xFE, 0xE2, 0xE2);
            default -> SLATE_50;
        };
    }

    private String format(BigDecimal value) {
        return value == null ? "N/A" : value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
