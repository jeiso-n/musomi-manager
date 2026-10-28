package com.musomi.desktop.controller.teacher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared, in-process store for assessments and their student mark data.
 *
 * This is the single source of truth for all assessment data while the backend
 * is not yet integrated. Replace the static fields with API calls once ready.
 *
 * Thread-safety: not needed — JavaFX is single-threaded on the UI thread.
 */
public final class AssessmentStore {

    // =========================================================================
    // Assessment model
    // =========================================================================

    public static class Assessment {
        private String name;
        private String className;
        private String subject;
        private String topic;
        private String date;        // display string e.g. "15 Sep 2026"
        private int    maxMarks;
        private String status;      // "Draft" | "Published"

        public Assessment(String name, String className, String subject,
                          String topic, String date, int maxMarks, String status) {
            this.name      = name;
            this.className = className;
            this.subject   = subject;
            this.topic     = topic;
            this.date      = date;
            this.maxMarks  = maxMarks;
            this.status    = status;
        }

        public String getName()      { return name; }
        public String getClassName() { return className; }
        public String getSubject()   { return subject; }
        public String getTopic()     { return topic; }
        public String getDate()      { return date; }
        public int    getMaxMarks()  { return maxMarks; }
        public String getStatus()    { return status; }

        public void setName(String n)      { this.name = n; }
        public void setClassName(String c) { this.className = c; }
        public void setSubject(String s)   { this.subject = s; }
        public void setTopic(String t)     { this.topic = t; }
        public void setDate(String d)      { this.date = d; }
        public void setMaxMarks(int m)     { this.maxMarks = m; }
        public void setStatus(String st)   { this.status = st; }
    }

    // =========================================================================
    // Student mark model
    // =========================================================================

    public static class StudentMark {
        private final String number;
        private final String student;
        private       String mark;    // empty string = not entered

        public StudentMark(String number, String student, String mark) {
            this.number  = number;
            this.student = student;
            this.mark    = mark;
        }

        public String getNumber()  { return number; }
        public String getStudent() { return student; }
        public String getMark()    { return mark; }
        public void   setMark(String m) { this.mark = m; }

        public boolean hasValidMark(int maxMarks) {
            return hasValidMark(mark, maxMarks);
        }

        public static boolean hasValidMark(String mark, int maxMarks) {
            if (mark == null || mark.trim().isEmpty() || maxMarks <= 0) return false;
            try {
                double v = Double.parseDouble(mark.trim());
                return v >= 0 && v <= maxMarks;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public String computeGrade(int maxMarks) {
            return gradeFor(mark, maxMarks);
        }

        public static String gradeFor(String mark, int maxMarks) {
            if (!hasValidMark(mark, maxMarks)) return "";
            double pct = (Double.parseDouble(mark.trim()) / maxMarks) * 100.0;
            if (pct >= 80) return "A";
            if (pct >= 70) return "B";
            if (pct >= 60) return "C";
            if (pct >= 50) return "D";
            if (pct >= 40) return "E";
            return "F";
        }

        public String computeRemark(int maxMarks) {
            return remarkFor(computeGrade(maxMarks));
        }

        public static String remarkFor(String grade) {
            switch (grade) {
                case "A": return "Excellent";
                case "B": return "Very Good";
                case "C": return "Good";
                case "D": return "Satisfactory";
                case "E": return "Needs Improvement";
                case "F": return "Fail";
                default:  return "";
            }
        }
    }

    public static class RosterStudent {
        private final String number;
        private final String admissionNumber;
        private final String name;
        private final String gender;
        private final String status;

        public RosterStudent(String number, String admissionNumber,
                             String name, String gender, String status) {
            this.number = number;
            this.admissionNumber = admissionNumber;
            this.name = name;
            this.gender = gender;
            this.status = status;
        }

        public String getNumber() { return number; }
        public String getAdmissionNumber() { return admissionNumber; }
        public String getName() { return name; }
        public String getGender() { return gender; }
        public String getStatus() { return status; }
    }

    // =========================================================================
    // Singleton store
    // =========================================================================

    /** All assessments, in insertion order. */
    private static final List<Assessment> ASSESSMENTS = new ArrayList<>(List.of(
        new Assessment("Mathematics Test 1", "S3 Blue", "Mathematics",
                       "Algebra",      "15 Sep 2026", 40, "Draft"),
        new Assessment("Mathematics Test 2", "S3 Red",  "Mathematics",
                       "Geometry",     "20 Sep 2026", 40, "Published"),
        new Assessment("Biology Assignment", "S4 Blue", "Biology",
                       "Cell Biology", "25 Sep 2026", 30, "Draft")
    ));

    /**
     * Student marks per assessment, keyed by Assessment identity (object ref).
     * Populated lazily on first access.
     */
    private static final Map<Assessment, List<StudentMark>> MARKS =
            new LinkedHashMap<>();

    private static boolean loadedFromDisk;

    /** Default mock student roster — shared across all classes for now. */
    private static final String[][] MOCK_STUDENTS = {
        {"1",  "John Mugisha"},
        {"2",  "Sarah Namukasa"},
        {"3",  "Brian Okello"},
        {"4",  "Mary Achieng"},
        {"5",  "David Kato"},
        {"6",  "Grace Nakato"},
        {"7",  "Daniel Ouma"},
        {"8",  "Rebecca Atim"},
        {"9",  "Peter Ssekandi"},
        {"10", "Esther Nankya"},
        {"11", "Samuel Tumusiime"},
        {"12", "Angela Nabirye"}
    };

    private static final Map<String, List<RosterStudent>> CLASS_ROSTERS =
            new LinkedHashMap<>();

    static {
        CLASS_ROSTERS.put("S3 Blue", List.of(
                new RosterStudent("1", "ADM-2024-001", "John Mugisha", "Male", "Active"),
                new RosterStudent("2", "ADM-2024-002", "Sarah Namukasa", "Female", "Active"),
                new RosterStudent("3", "ADM-2024-003", "Brian Okello", "Male", "Active"),
                new RosterStudent("4", "ADM-2024-004", "Mary Achieng", "Female", "Active"),
                new RosterStudent("5", "ADM-2024-005", "David Kato", "Male", "Active"),
                new RosterStudent("6", "ADM-2024-006", "Grace Nakato", "Female", "Active"),
                new RosterStudent("7", "ADM-2024-007", "Daniel Ouma", "Male", "Active"),
                new RosterStudent("8", "ADM-2024-008", "Rebecca Atim", "Female", "Active"),
                new RosterStudent("9", "ADM-2024-009", "Peter Ssekandi", "Male", "Active"),
                new RosterStudent("10", "ADM-2024-010", "Esther Nankya", "Female", "Active"),
                new RosterStudent("11", "ADM-2024-011", "Samuel Tumusiime", "Male", "Inactive"),
                new RosterStudent("12", "ADM-2024-012", "Angela Nabirye", "Female", "Active")
        ));
        CLASS_ROSTERS.put("S3 Red", List.of(
                new RosterStudent("1", "ADM-2024-101", "Michael Ochieng", "Male", "Active"),
                new RosterStudent("2", "ADM-2024-102", "Winnie Akello", "Female", "Active"),
                new RosterStudent("3", "ADM-2024-103", "Charles Otim", "Male", "Active"),
                new RosterStudent("4", "ADM-2024-104", "Betty Aciro", "Female", "Active"),
                new RosterStudent("5", "ADM-2024-105", "Patrick Ojok", "Male", "Active"),
                new RosterStudent("6", "ADM-2024-106", "Sylvia Adong", "Female", "Active"),
                new RosterStudent("7", "ADM-2024-107", "Moses Odongo", "Male", "Active"),
                new RosterStudent("8", "ADM-2024-108", "Juliet Auma", "Female", "Active"),
                new RosterStudent("9", "ADM-2024-109", "Simon Okot", "Male", "Active"),
                new RosterStudent("10", "ADM-2024-110", "Prossy Ayo", "Female", "Inactive")
        ));
        CLASS_ROSTERS.put("S4 Blue", List.of(
                new RosterStudent("1", "ADM-2023-201", "Kevin Ssemakula", "Male", "Active"),
                new RosterStudent("2", "ADM-2023-202", "Diana Nakalema", "Female", "Active"),
                new RosterStudent("3", "ADM-2023-203", "Ronald Mukasa", "Male", "Active"),
                new RosterStudent("4", "ADM-2023-204", "Sharon Nakintu", "Female", "Active"),
                new RosterStudent("5", "ADM-2023-205", "Ivan Kawooya", "Male", "Active"),
                new RosterStudent("6", "ADM-2023-206", "Mercy Nabukenya", "Female", "Active"),
                new RosterStudent("7", "ADM-2023-207", "Fred Ssempala", "Male", "Active"),
                new RosterStudent("8", "ADM-2023-208", "Joan Nalubega", "Female", "Active"),
                new RosterStudent("9", "ADM-2023-209", "Tonny Kigozi", "Male", "Active"),
                new RosterStudent("10", "ADM-2023-210", "Racheal Nabbosa", "Female", "Active"),
                new RosterStudent("11", "ADM-2023-211", "Dennis Musoke", "Male", "Active"),
                new RosterStudent("12", "ADM-2023-212", "Sarah Nansubuga", "Female", "Active"),
                new RosterStudent("13", "ADM-2023-213", "Arnold Kizza", "Male", "Active"),
                new RosterStudent("14", "ADM-2023-214", "Lydia Namuli", "Female", "Active")
        ));
        CLASS_ROSTERS.put("S4 Red", List.of(
                new RosterStudent("1", "ADM-2023-301", "Andrew Byaruhanga", "Male", "Active"),
                new RosterStudent("2", "ADM-2023-302", "Josephine Kyomuhendo", "Female", "Active"),
                new RosterStudent("3", "ADM-2023-303", "Robert Tumwebaze", "Male", "Active"),
                new RosterStudent("4", "ADM-2023-304", "Annet Kembabazi", "Female", "Active"),
                new RosterStudent("5", "ADM-2023-305", "Julius Ainembabazi", "Male", "Active"),
                new RosterStudent("6", "ADM-2023-306", "Justine Atuhaire", "Female", "Active"),
                new RosterStudent("7", "ADM-2023-307", "Emmanuel Baguma", "Male", "Active"),
                new RosterStudent("8", "ADM-2023-308", "Doreen Kobusingye", "Female", "Active"),
                new RosterStudent("9", "ADM-2023-309", "Godfrey Turyasingura", "Male", "Active"),
                new RosterStudent("10", "ADM-2023-310", "Prossy Kembabazi", "Female", "Active"),
                new RosterStudent("11", "ADM-2023-311", "Bosco Mugisha", "Male", "Inactive")
        ));
        CLASS_ROSTERS.put("S5", List.of(
                new RosterStudent("1", "ADM-2022-401", "Timothy Ssentongo", "Male", "Active"),
                new RosterStudent("2", "ADM-2022-402", "Margaret Nakawuki", "Female", "Active"),
                new RosterStudent("3", "ADM-2022-403", "Vincent Lubega", "Male", "Active"),
                new RosterStudent("4", "ADM-2022-404", "Rebecca Nalwoga", "Female", "Active"),
                new RosterStudent("5", "ADM-2022-405", "Joseph Mukiibi", "Male", "Active"),
                new RosterStudent("6", "ADM-2022-406", "Sarah Nakayiza", "Female", "Active"),
                new RosterStudent("7", "ADM-2022-407", "Paul Kaggwa", "Male", "Active"),
                new RosterStudent("8", "ADM-2022-408", "Teddy Nalubwama", "Female", "Active")
        ));
    }

    // Pre-populate marks for the already-Published assessment so it isn't empty
    static {
        Assessment published = ASSESSMENTS.get(1); // "Mathematics Test 2"
        String[] pubMarks = {"35","29","32","38","25","36","21","34","28","33","20","37"};
        List<StudentMark> list = new ArrayList<>();
        for (int i = 0; i < MOCK_STUDENTS.length; i++) {
            list.add(new StudentMark(MOCK_STUDENTS[i][0], MOCK_STUDENTS[i][1],
                                     pubMarks[i]));
        }
        MARKS.put(published, list);

        // "Mathematics Test 1" — partially filled (8 of 12)
        Assessment draft1 = ASSESSMENTS.get(0);
        String[] draft1Marks = {"35","32","28","30","24","38","","34","27","31","","36"};
        List<StudentMark> d1list = new ArrayList<>();
        for (int i = 0; i < MOCK_STUDENTS.length; i++) {
            d1list.add(new StudentMark(MOCK_STUDENTS[i][0], MOCK_STUDENTS[i][1],
                                       draft1Marks[i]));
        }
        MARKS.put(draft1, d1list);
    }

    private AssessmentStore() {} // static-only

    // =========================================================================
    // Public API
    // =========================================================================

    /** Loads persisted assessment data, retaining the seed state when no document is available. */
    public static void initialize(AssessmentRepository repository) {
        Logger log = LoggerFactory.getLogger(AssessmentStore.class);
        if (loadedFromDisk) {
            return;
        }

        final Optional<AssessmentsDocument> loadedDocument;
        try {
            loadedDocument = repository.load();
        } catch (RuntimeException ex) {
            log.error("Failed to load persisted assessment data; retaining current state", ex);
            return;
        }

        if (loadedDocument.isEmpty()) {
            log.info("No persisted data found; using seed data");
            return;
        }

        try {
            AssessmentsDocument document = loadedDocument.get();
            List<Assessment> assessments = new ArrayList<>();
            Map<Assessment, List<StudentMark>> marksByAssessment = new LinkedHashMap<>();

            for (AssessmentDto dto : document.assessments()) {
                Assessment assessment = new Assessment(
                        dto.name(), dto.className(), dto.subject(), dto.topic(),
                        dto.date(), dto.maxMarks(), dto.status());
                assessments.add(assessment);

                if (dto.marks() != null && !dto.marks().isEmpty()) {
                    List<StudentMark> marks = new ArrayList<>();
                    for (StudentMarkDto markDto : dto.marks()) {
                        marks.add(new StudentMark(
                                markDto.number(), markDto.student(), markDto.mark()));
                    }
                    marksByAssessment.put(assessment, marks);
                }
            }

            ASSESSMENTS.clear();
            ASSESSMENTS.addAll(assessments);
            MARKS.clear();
            MARKS.putAll(marksByAssessment);
            loadedFromDisk = true;
            log.info("Loaded {} assessments from persisted data", assessments.size());
        } catch (RuntimeException ex) {
            log.error("Failed to apply persisted assessment data; retaining current state", ex);
        }
    }

    /** Saves a snapshot of the current assessment state without propagating I/O failures. */
    public static void save(AssessmentRepository repository) {
        Logger log = LoggerFactory.getLogger(AssessmentStore.class);
        List<AssessmentDto> assessmentDtos = new ArrayList<>();

        for (Assessment assessment : ASSESSMENTS) {
            List<StudentMarkDto> markDtos = new ArrayList<>();
            List<StudentMark> marks = MARKS.get(assessment);
            if (marks != null) {
                for (StudentMark mark : marks) {
                    markDtos.add(new StudentMarkDto(mark.getNumber(), mark.getStudent(), mark.getMark()));
                }
            }

            assessmentDtos.add(new AssessmentDto(
                    assessment.getName(), assessment.getClassName(), assessment.getSubject(),
                    assessment.getTopic(), assessment.getDate(), assessment.getMaxMarks(),
                    assessment.getStatus(), markDtos));
        }

        AssessmentsDocument document = new AssessmentsDocument(
                AssessmentRepository.CURRENT_VERSION, assessmentDtos);
        try {
            repository.save(document);
            log.info("Saved {} assessments to persisted data", assessmentDtos.size());
        } catch (IOException ex) {
            log.error("Failed to save assessment data to persisted storage", ex);
        }
    }

    /** Saves the current state using the default user-home repository. */
    public static void saveDefault() {
        LoggerFactory.getLogger(AssessmentStore.class).debug("saveDefault triggered");
        save(new AssessmentRepository());
    }

    /**
     * Resets store state for test isolation only.
     */
    static void resetForTesting(List<Assessment> assessments) {
        ASSESSMENTS.clear();
        MARKS.clear();
        ASSESSMENTS.addAll(assessments);
        loadedFromDisk = false;
    }

    /** Returns the live mutable assessment list. */
    public static List<Assessment> getAssessments() {
        return ASSESSMENTS;
    }

    /**
     * Returns an immutable copy of the roster for the given class name.
     * Returns an empty list if the class has no configured roster.
     */
    public static List<RosterStudent> getClassRoster(String className) {
        if (className == null) return List.of();
        List<RosterStudent> roster = CLASS_ROSTERS.get(className);
        return roster == null ? List.of() : List.copyOf(roster);
    }

    public static int getClassRosterSize(String className) {
        return getClassRoster(className).size();
    }

    /** Returns assessments that are incomplete or have not yet been published. */
    public static List<Assessment> getPendingAssessments() {
        return ASSESSMENTS.stream()
                .filter(AssessmentStore::isPending)
                .toList();
    }

    /** An assessment remains pending until it is complete and published. */
    public static boolean isPending(Assessment assessment) {
        return assessment != null
                && (!"Published".equals(assessment.getStatus()) || !allMarksComplete(assessment));
    }

    /**
     * Returns the student mark list for the given assessment.
     * If no list exists yet, one is created with blank marks.
     */
    public static List<StudentMark> getMarks(Assessment assessment) {
        return MARKS.computeIfAbsent(assessment, a -> {
            List<StudentMark> list = new ArrayList<>();
            List<RosterStudent> roster = getClassRoster(a.getClassName());

            if (!roster.isEmpty()) {
                for (RosterStudent rs : roster) {
                    list.add(new StudentMark(rs.getNumber(), rs.getName(), ""));
                }
            } else {
                // Fallback for unknown classes — preserve existing behaviour
                for (String[] s : MOCK_STUDENTS) {
                    list.add(new StudentMark(s[0], s[1], ""));
                }
            }
            return list;
        });
    }

    /** Adds a new assessment and returns it. */
    public static Assessment addAssessment(String name, String className,
                                           String subject, String topic,
                                           String date, int maxMarks) {
        Assessment a = new Assessment(name, className, subject, topic,
                                      date, maxMarks, "Draft");
        ASSESSMENTS.add(a);
        return a;
    }

    /** Publishes the given assessment (must be Draft with all marks filled). */
    public static void publish(Assessment assessment) {
        if (!allMarksComplete(assessment)) {
            throw new IllegalStateException("All students must have a valid mark before publishing.");
        }
        assessment.setStatus("Published");
    }

    /** How many students have valid marks for this assessment. */
    public static int countCompleted(Assessment assessment) {
        List<StudentMark> marks = getMarks(assessment);
        int count = 0;
        for (StudentMark sm : marks) {
            if (sm.hasValidMark(assessment.getMaxMarks())) count++;
        }
        return count;
    }

    /** Returns the number of assessments with the exact status "Published". */
    public static int countPublished() {
        return (int) getAssessments().stream()
                .filter(assessment -> "Published".equals(assessment.getStatus()))
                .count();
    }

    /** Returns the number of distinct, non-blank class names across all assessments. */
    public static int countDistinctClasses() {
        return (int) getAssessments().stream()
                .map(Assessment::getClassName)
                .filter(className -> className != null && !className.isBlank())
                .map(String::trim)
                .distinct()
                .count();
    }

    /**
     * Returns the size of the mock student roster only.
     * This mock-only count will be replaced by a real endpoint when the backend is available.
     */
    public static int mockRosterSize() {
        return MOCK_STUDENTS.length;
    }

    /** True if every student has a valid mark. */
    public static boolean allMarksComplete(Assessment assessment) {
        List<StudentMark> marks = getMarks(assessment);
        if (marks.isEmpty()) return false;
        for (StudentMark sm : marks) {
            if (!sm.hasValidMark(assessment.getMaxMarks())) return false;
        }
        return true;
    }
}
