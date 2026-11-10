package com.musomi.desktop.controller.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class ReportPreviewFxmlContractTest {

    @Test
    void reportsScreenDefinesEveryRequiredControllerControl() throws Exception {
        try (InputStream fxml = getClass().getResourceAsStream("/fxml/report/reports.fxml")) {
            assertNotNull(fxml, "Reports FXML must be available on the classpath");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(fxml);

            String[] requiredIds = {
                    "classComboBox", "streamComboBox", "termComboBox", "generateButton",
                    "studentCountLabel", "completeCountLabel", "partialCountLabel",
                    "noResultsCountLabel", "classAverageLabel", "gradeDistributionLabel",
                    "reportTable", "studentNameCol", "statusCol", "completenessCol",
                    "averageCol", "gradeCol", "positionCol", "downloadCol",
                    "subjectTable", "subjectNameCol", "subjectAverageCol",
                    "studentsWithMarksCol", "studentsMissingMarksCol", "subjectCompletenessCol",
                    "emptyStateTitle", "emptyStateMessage"
            };

            for (String requiredId : requiredIds) {
                assertTrue(hasFxId(document, requiredId),
                        "ReportsController requires FXML fx:id=" + requiredId);
            }
            assertTrue(document.getDocumentElement().getAttribute("fx:controller")
                    .endsWith("ReportPreviewController"));
        }
    }

    @Test
    void reportsScreenDoesNotAdvertisePlaceholderLocalData() throws Exception {
        try (InputStream fxml = getClass().getResourceAsStream("/fxml/report/reports.fxml")) {
            assertNotNull(fxml);
            String xml = new String(fxml.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(!xml.contains("LOCAL DATA"),
                    "Reports screen must no longer present the local-data placeholder");
        }
    }

    @Test
    void adminNavigationProvidesAccessToReports() throws Exception {
        try (InputStream fxml = getClass().getResourceAsStream("/fxml/main-layout.fxml")) {
            assertNotNull(fxml, "Main layout FXML must be available on the classpath");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(fxml);
            assertTrue(hasFxId(document, "adminReportsButton"),
                    "Administrators must have a Reports navigation entry");

            Element reportsButton = findByFxId(document, "adminReportsButton");
            assertNotNull(reportsButton);
            assertTrue("Button".equals(reportsButton.getTagName()),
                    "The admin Reports navigation entry must be a real JavaFX Button");
            assertTrue(reportsButton.getAttribute("styleClass").contains("fxml-inline-014"),
                    "The admin Reports button must use the visible sidebar navigation style");
            assertTrue("#onAdminReportsClicked".equals(reportsButton.getAttribute("onAction")),
                    "The admin Reports button must open the Reports screen");
        }
    }

    private static Element findByFxId(Document document, String id) {
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttributeNS("http://javafx.com/fxml", "id"))
                    || id.equals(element.getAttributeNS("http://javafx.com/fxml/1", "id"))) {
                return element;
            }
        }
        return null;
    }

    private static boolean hasFxId(Document document, String id) {
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttributeNS("http://javafx.com/fxml", "id"))
                    || id.equals(element.getAttributeNS("http://javafx.com/fxml/1", "id"))) {
                return true;
            }
        }
        return false;
    }
}
