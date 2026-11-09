package com.musomi.desktop.controller.teacher;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class AssessmentsFxmlContractTest {

    @Test
    void assessmentFormDefinesTermComboRequiredByController() throws Exception {
        try (InputStream fxml = getClass().getResourceAsStream("/fxml/teacher/assessments.fxml")) {
            assertNotNull(fxml, "Teacher Assessments FXML must be available on the classpath");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder().parse(fxml);
            NodeList comboBoxes = document.getElementsByTagName("ComboBox");
            boolean hasTermCombo = false;

            for (int i = 0; i < comboBoxes.getLength(); i++) {
                Element comboBox = (Element) comboBoxes.item(i);
                if ("termCombo".equals(comboBox.getAttributeNS(
                        "http://javafx.com/fxml", "id"))) {
                    hasTermCombo = true;
                    assertTrue("Select term".equals(comboBox.getAttribute("promptText")),
                            "Term selector should have a clear prompt");
                    break;
                }
            }

            assertTrue(hasTermCombo,
                    "AssessmentsController injects termCombo, so the FXML must define fx:id=termCombo");
        }
    }
}
