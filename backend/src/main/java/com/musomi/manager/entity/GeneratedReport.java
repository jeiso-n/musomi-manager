package com.musomi.manager.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Metadata for a generated student report. */
@Entity
@Table(name = "generated_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"school", "student", "term", "generatedBy", "subjectResults"})
@EqualsAndHashCode(exclude = {"school", "student", "term", "generatedBy", "subjectResults"})
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = true)
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "stream_name", length = 50)
    private String streamName;

    @Column(name = "report_status", length = 20)
    private String reportStatus;

    @Column(name = "expected_assessment_count")
    private Integer expectedAssessmentCount;

    @Column(name = "recorded_assessment_count")
    private Integer recordedAssessmentCount;

    @Column(name = "overall_average", precision = 6, scale = 2)
    private java.math.BigDecimal overallAverage;

    @Column(name = "overall_grade", length = 2)
    private String overallGrade;

    @Column(name = "class_position")
    private Integer classPosition;

    @Column(name = "ranking_cohort_size")
    private Integer rankingCohortSize;

    @Builder.Default
    @OneToMany(mappedBy = "generatedReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratedReportSubject> subjectResults = new ArrayList<>();
}
