package com.musomi.desktop.controller.teacher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.musomi.desktop.model.dto.TeacherClassResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassRosterControllerTest {

    @Test
    @DisplayName("resolveClassIdsFromList resolves whole-class assignments where streamId is null")
    void resolveClassIdsFromList_handlesWholeClassAssignments() {
        TeacherClassResponse[] classes = new TeacherClassResponse[] {
                new TeacherClassResponse(1L, "S3", null, null, 1L, "Mathematics", 50, 1, 1),
                new TeacherClassResponse(1L, "S3", null, null, 2L, "Physics", 50, 0, 0)
        };

        ClassRosterController.ClassIds ids =
                ClassRosterController.resolveClassIdsFromList(classes, "S3");

        assertEquals(1L, ids.classId());
        assertNull(ids.streamId());
    }

    @Test
    @DisplayName("resolveClassIdsFromList resolves stream-specific assignments")
    void resolveClassIdsFromList_handlesStreamSpecificAssignments() {
        TeacherClassResponse[] classes = new TeacherClassResponse[] {
                new TeacherClassResponse(1L, "S3", 2L, "Blue", 1L, "Mathematics", 25, 1, 0),
                new TeacherClassResponse(2L, "S4", 3L, "Red", 3L, "Chemistry", 30, 2, 1)
        };

        ClassRosterController.ClassIds ids =
                ClassRosterController.resolveClassIdsFromList(classes, "S3 Blue");

        assertEquals(1L, ids.classId());
        assertEquals(2L, ids.streamId());
    }

    @Test
    @DisplayName("buildRosterEndpointPath builds stream or whole-class endpoint path appropriately")
    void buildRosterEndpointPath_buildsExpectedPath() {
        assertEquals("/teacher/classes/1/streams/2/students",
                ClassRosterController.buildRosterEndpointPath(1L, 2L));
        assertEquals("/teacher/classes/1/students",
                ClassRosterController.buildRosterEndpointPath(1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> ClassRosterController.buildRosterEndpointPath(null, 2L));
    }
}

