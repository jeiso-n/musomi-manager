package com.musomi.manager.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Immutable-at-generation-time subject result snapshot for a student report. */
@Entity
@Table(name = "generated_report_subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "generatedReport")
@EqualsAndHashCode(exclude = "generatedReport")
public class GeneratedReportSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_report_id", nullable = false)
    private GeneratedReport generatedReport;

    @Column(name = "subject_name", nullable = false, length = 200)
    private String subjectName;

    @Column(name = "average", precision = 6, scale = 2)
    private BigDecimal average;

    @Column(name = "grade", length = 2)
    private String grade;

    @Column(name = "expected_assessment_count", nullable = false)
    private Integer expectedAssessmentCount;

    @Column(name = "recorded_assessment_count", nullable = false)
    private Integer recordedAssessmentCount;
}
