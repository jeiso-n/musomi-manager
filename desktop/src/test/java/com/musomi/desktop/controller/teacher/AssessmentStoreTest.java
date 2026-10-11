package com.musomi.desktop.controller.teacher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.controller.teacher.AssessmentStore.RosterStudent;
import com.musomi.desktop.controller.teacher.AssessmentStore.StudentMark;

class AssessmentStoreTest {

    @Nested
    class PersistenceTests {

        @TempDir
        Path temporaryDirectory;

        private List<Assessment> originalAssessments;

        @BeforeEach
        void setUp() {
            originalAssessments = List.copyOf(AssessmentStore.getAssessments());
            resetToSeedState(originalAssessments);
        }

        @AfterEach
        void tearDown() {
            resetToSeedState(originalAssessments);
        }

        @Test
        void shouldSeedWhenNoPersistedFileExists() throws ReflectiveOperationException {
            AssessmentRepository repository = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments.json"));

            AssessmentStore.initialize(repository);

            assertEquals(3, AssessmentStore.getAssessments().size());
            Field loadedFromDisk = AssessmentStore.class.getDeclaredField("loadedFromDisk");
            loadedFromDisk.setAccessible(true);
            assertFalse(loadedFromDisk.getBoolean(null));
        }

        @Test
        void shouldLoadPersistedAssessmentsAndReplaceSeed() throws IOException {
            AssessmentRepository repository = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments.json"));
            repository.save(new AssessmentsDocument(1, List.of(
                    new AssessmentDto("Persisted Test", "S3 Blue", "Mathematics", "Algebra",
                            "15 Sep 2026", 40, "Draft", List.of(
                            new StudentMarkDto("1", "John Mugisha", "35"),
                            new StudentMarkDto("2", "Sarah Namukasa", "32")
                    ))
            )));

            AssessmentStore.initialize(repository);

            assertEquals(1, AssessmentStore.getAssessments().size());
            Assessment assessment = AssessmentStore.getAssessments().get(0);
            assertEquals("Persisted Test", assessment.getName());
            assertEquals(List.of(
                    new StudentMarkDto("1", "John Mugisha", "35"),
                    new StudentMarkDto("2", "Sarah Namukasa", "32")
            ), AssessmentStore.getMarks(assessment).stream()
                    .map(mark -> new StudentMarkDto(mark.getNumber(), mark.getStudent(), mark.getMark()))
                    .toList());
        }

        @Test
        void shouldPreserveLazyMarksWhenSavedDocumentHasNoMarks() throws IOException {
            AssessmentRepository repository = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments.json"));
            repository.save(new AssessmentsDocument(1, List.of(
                    new AssessmentDto("No Marks", "S3 Blue", "Mathematics", "Algebra",
                            "15 Sep 2026", 40, "Draft", null)
            )));

            AssessmentStore.initialize(repository);

            assertEquals(1, AssessmentStore.getAssessments().size());
            List<StudentMark> marks = AssessmentStore.getMarks(AssessmentStore.getAssessments().get(0));
            assertEquals(12, marks.size());
            assertTrue(marks.stream().allMatch(mark -> mark.getMark().isEmpty()));
        }

        @Test
        void shouldNotReloadAfterFirstInitialize() throws IOException {
            AssessmentRepository repositoryA = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments-a.json"));
            AssessmentRepository repositoryB = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments-b.json"));
            repositoryA.save(new AssessmentsDocument(1, List.of(
                    new AssessmentDto("Assessment A", "S3 Blue", "Mathematics", "Algebra",
                            "15 Sep 2026", 40, "Draft", List.of())
            )));
            repositoryB.save(new AssessmentsDocument(1, List.of(
                    new AssessmentDto("Assessment B", "S3 Red", "Mathematics", "Geometry",
                            "20 Sep 2026", 40, "Draft", List.of())
            )));

            AssessmentStore.initialize(repositoryA);
            AssessmentStore.initialize(repositoryB);

            assertEquals(1, AssessmentStore.getAssessments().size());
            assertEquals("Assessment A", AssessmentStore.getAssessments().get(0).getName());
        }

        @Test
        void shouldRoundTripThroughSaveAndInitialize() {
            AssessmentRepository repository = new AssessmentRepository(
                    temporaryDirectory.resolve("assessments.json"));
            Assessment published = AssessmentStore.getAssessments().stream()
                    .filter(assessment -> "Published".equals(assessment.getStatus()))
                    .findFirst()
                    .orElseThrow();
            List<StudentMark> originalMarks = List.copyOf(AssessmentStore.getMarks(published));

            AssessmentStore.save(repository);
            AssessmentStore.resetForTesting(List.of());
            AssessmentStore.initialize(repository);

            assertEquals(3, AssessmentStore.getAssessments().size());
            assertEquals(1, AssessmentStore.countPublished());
            assertEquals(3, AssessmentStore.countDistinctClasses());
            Assessment loadedPublished = AssessmentStore.getAssessments().stream()
                    .filter(assessment -> "Published".equals(assessment.getStatus()))
                    .findFirst()
                    .orElseThrow();
            List<StudentMark> loadedMarks = AssessmentStore.getMarks(loadedPublished);
            assertEquals(originalMarks.size(), loadedMarks.size());
            assertEquals(originalMarks.get(0).getMark(), loadedMarks.get(0).getMark());
            assertEquals(originalMarks.get(originalMarks.size() - 1).getMark(),
                    loadedMarks.get(loadedMarks.size() - 1).getMark());
        }

        private void resetToSeedState(List<Assessment> assessments) {
            AssessmentStore.resetForTesting(assessments);
            if (assessments.size() < 2) {
                return;
            }

            String[] draftMarks = {"35", "32", "28", "30", "24", "38",
                    "", "34", "27", "31", "", "36"};
            String[] publishedMarks = {"35", "29", "32", "38", "25", "36",
                    "21", "34", "28", "33", "20", "37"};
            List<StudentMark> draftList = AssessmentStore.getMarks(assessments.get(0));
            for (int i = 0; i < draftList.size(); i++) {
                draftList.get(i).setMark(draftMarks[i]);
            }

            List<StudentMark> marks = AssessmentStore.getMarks(assessments.get(1));
            for (int i = 0; i < marks.size(); i++) {
                marks.get(i).setMark(publishedMarks[i]);
            }
        }
    }

    @Test
    void shouldReturnRosterForKnownClass() {
        assertEquals(12, AssessmentStore.getClassRoster("S3 Blue").size());
        assertEquals(10, AssessmentStore.getClassRoster("S3 Red").size());
        assertEquals(14, AssessmentStore.getClassRoster("S4 Blue").size());
        assertEquals(11, AssessmentStore.getClassRoster("S4 Red").size());
        assertEquals(8, AssessmentStore.getClassRoster("S5").size());
    }

    @Test
    void shouldReturnEmptyRosterForUnknownClass() {
        assertTrue(AssessmentStore.getClassRoster("Nonexistent").isEmpty());
        assertTrue(AssessmentStore.getClassRoster(null).isEmpty());
    }

    @Test
    void shouldReturnImmutableRosterCopy() {
        List<RosterStudent> roster = AssessmentStore.getClassRoster("S3 Blue");

        assertThrows(UnsupportedOperationException.class, () -> roster.add(
                new RosterStudent("13", "ADM-2024-013", "Test Student", "Male", "Active")));

        List<RosterStudent> secondCall = AssessmentStore.getClassRoster("S3 Blue");
        assertEquals(12, secondCall.size());
        assertEquals("John Mugisha", secondCall.get(0).getName());
    }

    @Test
    void shouldUseClassRosterForNewAssessmentMarks() throws ReflectiveOperationException {
        Assessment assessment = AssessmentStore.addAssessment(
                "Roster test", "S4 Blue", "Mathematics", "Algebra", "11 Oct 2026", 40);

        try {
            List<StudentMark> marks = AssessmentStore.getMarks(assessment);

            assertEquals(14, marks.size());
            assertEquals("1", marks.get(0).getNumber());
            assertEquals("Kevin Ssemakula", marks.get(0).getStudent());
        } finally {
            removeAssessmentAndMarks(assessment);
        }
    }

    @Test
    void shouldFallBackToGlobalRosterForUnknownClass() throws ReflectiveOperationException {
        Assessment assessment = AssessmentStore.addAssessment(
                "Fallback test", "Unknown Class", "Mathematics", "Algebra", "11 Oct 2026", 40);

        try {
            assertEquals(12, AssessmentStore.getMarks(assessment).size());
        } finally {
            removeAssessmentAndMarks(assessment);
        }
    }

    @Test
    void shouldNotChangeExistingSeededAssessmentMarks() {
        Assessment assessment = AssessmentStore.getAssessments().stream()
                .filter(candidate -> "Mathematics Test 1".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        List<StudentMark> marks = AssessmentStore.getMarks(assessment);

        assertEquals(12, marks.size());
        assertEquals("35", marks.get(0).getMark());
    }

    private static void removeAssessmentAndMarks(Assessment assessment)
            throws ReflectiveOperationException {
        AssessmentStore.getAssessments().remove(assessment);
        Field marksField = AssessmentStore.class.getDeclaredField("MARKS");
        marksField.setAccessible(true);
        ((Map<?, ?>) marksField.get(null)).remove(assessment);
    }

    @Test
    void calculatesGradeAgainstAssessmentMaximum() {
        StudentMark mark = new StudentMark("1", "Student", "32");

        assertEquals("A", mark.computeGrade(40));
        assertEquals("Excellent", mark.computeRemark(40));
        assertEquals("F", new StudentMark("2", "Student", "15").computeGrade(40));
    }

    @Test
    void rejectsMarksOutsideTheAssessmentRange() {
        assertFalse(StudentMark.hasValidMark("-1", 40));
        assertFalse(StudentMark.hasValidMark("41", 40));
        assertFalse(StudentMark.hasValidMark("", 40));
        assertTrue(StudentMark.hasValidMark("0", 40));
    }

    @Test
    void cannotPublishUntilEveryStudentHasAValidMark() {
        Assessment assessment = new Assessment(
                "Test", "S3 Blue", "Mathematics", "Algebra",
                "15 Sep 2026", 40, "Draft");

        assertThrows(IllegalStateException.class, () -> AssessmentStore.publish(assessment));

        List<StudentMark> marks = AssessmentStore.getMarks(assessment);
        marks.forEach(mark -> mark.setMark("0"));
        AssessmentStore.publish(assessment);

        assertEquals("Published", assessment.getStatus());
    }

    @Test
    void pendingAssessmentsAreThoseNotPublishedOrNotFullyMarked() {
        assertEquals(2, AssessmentStore.getPendingAssessments().size());
        assertTrue(AssessmentStore.getPendingAssessments().stream()
                .allMatch(AssessmentStore::isPending));

        Assessment assessment = new Assessment(
                "Complete draft", "S3 Blue", "Mathematics", "Algebra",
                "15 Sep 2026", 40, "Draft");
        List<StudentMark> marks = AssessmentStore.getMarks(assessment);
        marks.forEach(mark -> mark.setMark("0"));
        assertTrue(AssessmentStore.isPending(assessment));

        assessment.setStatus("Published");
        assertFalse(AssessmentStore.isPending(assessment));
    }

    @Test
    void countsPublishedAssessmentsFromSeedData() {
        assertTrue(AssessmentStore.countPublished() >= 1);
    }

    @Test
    void returnsZeroWhenNoAssessmentsArePublished() {
        List<Assessment> assessments = AssessmentStore.getAssessments();
        List<Assessment> originalAssessments = new ArrayList<>(assessments);

        try {
            assessments.clear();
            assessments.add(new Assessment(
                    "Draft test", "S3 Blue", "Mathematics", "Algebra",
                    "15 Sep 2026", 40, "Draft"));

            assertEquals(0, AssessmentStore.countPublished());
        } finally {
            assessments.clear();
            assessments.addAll(originalAssessments);
        }
    }

    @Test
    void countsThreeDistinctClassesInSeedData() {
        assertEquals(3, AssessmentStore.countDistinctClasses());
    }

    @Test
    void doesNotCountNullOrBlankClassNames() {
        List<Assessment> assessments = AssessmentStore.getAssessments();
        List<Assessment> originalAssessments = new ArrayList<>(assessments);

        try {
            assessments.clear();
            assessments.add(new Assessment(
                    "Class test", "S3 Blue", "Mathematics", "Algebra",
                    "15 Sep 2026", 40, "Draft"));
            assessments.add(new Assessment(
                    "Null class test", null, "Mathematics", "Algebra",
                    "15 Sep 2026", 40, "Draft"));
            assessments.add(new Assessment(
                    "Blank class test", "  ", "Mathematics", "Algebra",
                    "15 Sep 2026", 40, "Draft"));
            assessments.add(new Assessment(
                    "Duplicate class test", " S3 Blue ", "Mathematics", "Algebra",
                    "15 Sep 2026", 40, "Draft"));

            assertEquals(1, AssessmentStore.countDistinctClasses());
        } finally {
            assessments.clear();
            assessments.addAll(originalAssessments);
        }
    }

    @Test
    void returnsMockRosterSize() {
        assertEquals(12, AssessmentStore.mockRosterSize());
    }
}
