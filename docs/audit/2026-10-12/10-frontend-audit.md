# 10. Frontend state

## JavaFX

- **FXML files:** 26 under `desktop/src/main/resources/fxml/`.
- **JavaFX controller files:** 41.
- **Files under 20 lines:** 16 controller files.
- **FXML references to missing controller classes:** none found.
- One FXML has no `fx:controller` declaration: `desktop/src/main/resources/fxml/admin/school-settings.fxml`. **Evidence:** the file contains no `fx:controller` attribute. **Should be:** assign the corresponding controller if the view is intended to be interactive, or document that it is a passive view.
- Console output remains in controller source. Example: `desktop/src/main/java/com/musomi/desktop/controller/MainLayoutController.java` includes `System.out.println(...)` for view-loading diagnostics. **Should be:** use the application logger.

The 16 short controllers are counted by source-file length; length alone does not prove each is incomplete. Their exact production behavior was not exhaustively assessed here.

## Web templates and controller data

There are 19 Thymeleaf HTML templates. Student-facing dashboard and marks pages return templates but populate their models with hardcoded sample data rather than services.

**Evidence:** `backend/src/main/java/com/musomi/manager/controller/web/StudentDashboardWebController.java` sets `model.addAttribute("firstName", "Sarah")` and a fixed `"average", "82.5"`; `backend/src/main/java/com/musomi/manager/controller/web/StudentMarksWebController.java` constructs `List<Map<String, Object>> mathMarks` with `"Mid-term Test"` and `"score", 85.0`. **Should be:** load the logged-in student's real records through services and authorize each requested record against that student.

`StudentReportsWebController` and `StudentResultsWebController` are present, but whether their complete data is service-backed was not verified beyond the inspected page flows. The student pages' readiness for pilot use is therefore **unknown** beyond the directly evidenced mock dashboard/marks data.
