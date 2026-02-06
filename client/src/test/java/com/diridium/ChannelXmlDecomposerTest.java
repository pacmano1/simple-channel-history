package com.diridium;

/*
   Copyright [2025-2026] [Diridium Technologies Inc.]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

public class ChannelXmlDecomposerTest {

    private String loadResource(String name) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull("Resource not found: " + name, is);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testDecomposeVersion1() throws Exception {
        String xml = loadResource("channel-for-diffing-version1.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Channel scripts
        assertNotNull(components.get("Channel Scripts/Preprocessing Script"));
        assertNotNull(components.get("Channel Scripts/Postprocessing Script"));
        assertNotNull(components.get("Channel Scripts/Deploy Script"));
        assertNotNull(components.get("Channel Scripts/Undeploy Script"));

        // Destination 1 has a JavaScript Writer with a script
        assertNotNull(components.get("Destination: Destination 1 [1]/Script"));
        assertEquals("logger.info(\"destination one\");",
                components.get("Destination: Destination 1 [1]/Script").getContent());

        // Destination 2 (HTTP Sender) has no script element
        assertNull(components.get("Destination: Destination 2 [2]/Script"));

        // No source connector script (VmReceiverProperties has no <script>)
        assertNull(components.get("Source Connector/Script"));

        // Connector configurations
        assertNotNull(components.get("Source Connector/Configuration"));
        assertNotNull(components.get("Destination: Destination 1 [1]/Configuration"));
        assertNotNull(components.get("Destination: Destination 2 [2]/Configuration"));

        // All filter/transformer/responseTransformer have empty <elements/> in version 1,
        // so no step components are extracted — no Filter/Transformer/Response Transformer sub-groups
        assertNull(components.get("Source Connector/Filter"));
        assertNull(components.get("Source Connector/Transformer"));
        assertNull(components.get("Destination: Destination 1 [1]/Filter"));
        assertNull(components.get("Destination: Destination 1 [1]/Transformer"));
        assertNull(components.get("Destination: Destination 1 [1]/Response Transformer"));
        assertNull(components.get("Destination: Destination 2 [2]/Filter"));
        assertNull(components.get("Destination: Destination 2 [2]/Transformer"));
        assertNull(components.get("Destination: Destination 2 [2]/Response Transformer"));

        // Channel Properties remainder
        assertNotNull(components.get("Channel Properties"));
    }

    @Test
    public void testDecomposeVersion2() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Deploy script changed in version 2
        String deployScript = components.get("Channel Scripts/Deploy Script").getContent();
        assertTrue(deployScript.contains("added to just check how diffing works"));

        // Destination 1 still has JavaScript Writer script
        assertNotNull(components.get("Destination: Destination 1 [1]/Script"));

        // Version 2 Destination 2 has a JavaScriptStep transformer — extracted as Step 0
        String stepKey = "Destination: Destination 2 [2]/Transformer/Step 0";
        assertNotNull(components.get(stepKey));
        String stepContent = components.get(stepKey).getContent();
        assertTrue(stepContent.contains("JavaScriptStep"));
        assertTrue(stepContent.contains("dummy transformer"));

        // Channel Properties
        assertNotNull(components.get("Channel Properties"));
    }

    @Test
    public void testChangeDetectionBetweenVersions() throws Exception {
        String xml1 = loadResource("channel-for-diffing-version1.xml");
        String xml2 = loadResource("channel-for-diffing-version2.xml");

        Map<String, DecomposedComponent> comp1 = ChannelXmlDecomposer.decompose(xml1);
        Map<String, DecomposedComponent> comp2 = ChannelXmlDecomposer.decompose(xml2);

        // Deploy script should be different
        assertNotEquals(
                comp1.get("Channel Scripts/Deploy Script").getContent(),
                comp2.get("Channel Scripts/Deploy Script").getContent());

        // Preprocessing script should be the same
        assertEquals(
                comp1.get("Channel Scripts/Preprocessing Script").getContent(),
                comp2.get("Channel Scripts/Preprocessing Script").getContent());

        // Destination 2 transformer: version 1 has no steps, version 2 has Step 0 (RIGHT_ONLY)
        String stepKey = "Destination: Destination 2 [2]/Transformer/Step 0";
        assertNull(comp1.get(stepKey));
        assertNotNull(comp2.get(stepKey));

        // Destination 2 config changed (HTTP Sender -> TCP Sender)
        assertNotEquals(
                comp1.get("Destination: Destination 2 [2]/Configuration").getContent(),
                comp2.get("Destination: Destination 2 [2]/Configuration").getContent());
    }

    @Test
    public void testComponentCategories() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        assertEquals(DecomposedComponent.Category.CHANNEL_SCRIPT,
                components.get("Channel Scripts/Deploy Script").getCategory());
        assertEquals(DecomposedComponent.Category.CONNECTOR_CONFIGURATION,
                components.get("Source Connector/Configuration").getCategory());
        assertEquals(DecomposedComponent.Category.CONNECTOR_CONFIGURATION,
                components.get("Destination: Destination 1 [1]/Configuration").getCategory());
        assertEquals(DecomposedComponent.Category.CONNECTOR_SCRIPT,
                components.get("Destination: Destination 1 [1]/Script").getCategory());
        assertEquals(DecomposedComponent.Category.CHANNEL_PROPERTIES,
                components.get("Channel Properties").getCategory());

        // Step inherits its parent type's category
        assertEquals(DecomposedComponent.Category.TRANSFORMER,
                components.get("Destination: Destination 2 [2]/Transformer/Step 0").getCategory());
    }

    @Test
    public void testParentGroups() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        assertEquals("Channel Scripts",
                components.get("Channel Scripts/Deploy Script").getParentGroup());
        assertEquals("Destination: Destination 1 [1]",
                components.get("Destination: Destination 1 [1]/Script").getParentGroup());

        // Step's parent group is the sub-group (connector/elementType)
        assertEquals("Destination: Destination 2 [2]/Transformer",
                components.get("Destination: Destination 2 [2]/Transformer/Step 0").getParentGroup());
    }

    @Test
    public void testChannelPropertiesDoesNotContainExtractedContent() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        String channelProps = components.get("Channel Properties").getContent();
        // Extracted scripts should not appear in the remainder
        assertFalse(channelProps.contains("<preprocessingScript>"));
        assertFalse(channelProps.contains("<postprocessingScript>"));
        assertFalse(channelProps.contains("<deployScript>"));
        assertFalse(channelProps.contains("<undeployScript>"));
        // Connectors should not appear in the remainder
        assertFalse(channelProps.contains("<sourceConnector"));
        assertFalse(channelProps.contains("destinationConnectors"));
    }

    @Test
    public void testConnectorConfigDoesNotContainExtractedSteps() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Destination 2 config should not contain the JavaScriptStep (it was extracted)
        String dest2Config = components.get("Destination: Destination 2 [2]/Configuration").getContent();
        assertFalse(dest2Config.contains("JavaScriptStep"));
        assertFalse(dest2Config.contains("dummy transformer"));

        // The transformer element itself (with data types) should remain in config
        assertTrue(dest2Config.contains("<transformer"));

        // Source config should still have filter/transformer shells (empty elements)
        String srcConfig = components.get("Source Connector/Configuration").getContent();
        assertTrue(srcConfig.contains("<filter"));
        assertTrue(srcConfig.contains("<transformer"));
    }

    @Test
    public void testConnectorConfigChangeDetection() throws Exception {
        String xml1 = loadResource("channel-for-diffing-version1.xml");
        String xml2 = loadResource("channel-for-diffing-version2.xml");

        Map<String, DecomposedComponent> comp1 = ChannelXmlDecomposer.decompose(xml1);
        Map<String, DecomposedComponent> comp2 = ChannelXmlDecomposer.decompose(xml2);

        // Destination 2 config changed between versions (HTTP Sender -> TCP Sender)
        assertNotEquals(
                comp1.get("Destination: Destination 2 [2]/Configuration").getContent(),
                comp2.get("Destination: Destination 2 [2]/Configuration").getContent());

        // Both versions should have the destination configuration
        assertNotNull(comp1.get("Destination: Destination 2 [2]/Configuration"));
        assertNotNull(comp2.get("Destination: Destination 2 [2]/Configuration"));
    }

    @Test
    public void testMalformedXmlThrowsException() {
        try {
            ChannelXmlDecomposer.decompose("not valid xml <>");
            fail("Should have thrown an exception for malformed XML");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testStepExtraction() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Destination 2 has one JavaScriptStep in its transformer
        String stepKey = "Destination: Destination 2 [2]/Transformer/Step 0";
        DecomposedComponent step = components.get(stepKey);
        assertNotNull(step);

        // Display name includes sequence and type
        assertEquals("Step 0: JavaScriptStep", step.getDisplayName());

        // Category is TRANSFORMER
        assertEquals(DecomposedComponent.Category.TRANSFORMER, step.getCategory());

        // Parent group is the sub-group
        assertEquals("Destination: Destination 2 [2]/Transformer", step.getParentGroup());

        // Content is the serialized step XML
        assertTrue(step.getContent().contains("<sequenceNumber>0</sequenceNumber>"));
        assertTrue(step.getContent().contains("dummy transformer"));
    }

    @Test
    public void testGetStepTypeName() {
        assertEquals("JavaScriptStep",
                ChannelXmlDecomposer.getStepTypeName("com.mirth.connect.plugins.javascriptstep.JavaScriptStep"));
        assertEquals("MapperStep",
                ChannelXmlDecomposer.getStepTypeName("com.mirth.connect.plugins.mapper.MapperStep"));
        assertEquals("simpleTag",
                ChannelXmlDecomposer.getStepTypeName("simpleTag"));
    }
}
