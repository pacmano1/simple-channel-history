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

        // Filter/transformer/responseTransformer are extracted as whole blocks
        assertNotNull(components.get("Source Connector/Filter"));
        assertNotNull(components.get("Source Connector/Transformer"));
        assertNotNull(components.get("Destination: Destination 1 [1]/Filter"));
        assertNotNull(components.get("Destination: Destination 1 [1]/Transformer"));
        assertNotNull(components.get("Destination: Destination 1 [1]/Response Transformer"));
        assertNotNull(components.get("Destination: Destination 2 [2]/Filter"));
        assertNotNull(components.get("Destination: Destination 2 [2]/Transformer"));
        assertNotNull(components.get("Destination: Destination 2 [2]/Response Transformer"));

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

        // Version 2 Destination 2 has a transformer with a JavaScriptStep
        String transformerKey = "Destination: Destination 2 [2]/Transformer";
        assertNotNull(components.get(transformerKey));
        String transformerContent = components.get(transformerKey).getContent();
        assertTrue(transformerContent.contains("JavaScriptStep"));
        assertTrue(transformerContent.contains("dummy transformer"));

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

        // Destination 2 transformer: both versions have it, but content differs (version 2 has a JavaScriptStep)
        String transformerKey = "Destination: Destination 2 [2]/Transformer";
        assertNotNull(comp1.get(transformerKey));
        assertNotNull(comp2.get(transformerKey));
        assertNotEquals(comp1.get(transformerKey).getContent(), comp2.get(transformerKey).getContent());

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

        // Transformer block has TRANSFORMER category
        assertEquals(DecomposedComponent.Category.TRANSFORMER,
                components.get("Destination: Destination 2 [2]/Transformer").getCategory());
    }

    @Test
    public void testParentGroups() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        assertEquals("Channel Scripts",
                components.get("Channel Scripts/Deploy Script").getParentGroup());
        assertEquals("Destination: Destination 1 [1]",
                components.get("Destination: Destination 1 [1]/Script").getParentGroup());

        // Transformer's parent group is the connector
        assertEquals("Destination: Destination 2 [2]",
                components.get("Destination: Destination 2 [2]/Transformer").getParentGroup());
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

        // Destination 2 config should not contain transformer or filter (they were extracted)
        String dest2Config = components.get("Destination: Destination 2 [2]/Configuration").getContent();
        assertFalse(dest2Config.contains("<transformer"));
        assertFalse(dest2Config.contains("<filter"));
        assertFalse(dest2Config.contains("JavaScriptStep"));
        assertFalse(dest2Config.contains("dummy transformer"));

        // Source config should not contain filter/transformer (they were extracted)
        String srcConfig = components.get("Source Connector/Configuration").getContent();
        assertFalse(srcConfig.contains("<filter"));
        assertFalse(srcConfig.contains("<transformer"));
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
    public void testTransformerExtraction() throws Exception {
        String xml = loadResource("channel-for-diffing-version2.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Destination 2 transformer extracted as a whole block
        String transformerKey = "Destination: Destination 2 [2]/Transformer";
        DecomposedComponent transformer = components.get(transformerKey);
        assertNotNull(transformer);

        // Display name is the element type
        assertEquals("Transformer", transformer.getDisplayName());

        // Category is TRANSFORMER
        assertEquals(DecomposedComponent.Category.TRANSFORMER, transformer.getCategory());

        // Parent group is the connector
        assertEquals("Destination: Destination 2 [2]", transformer.getParentGroup());

        // Content contains the full transformer block including the step
        assertTrue(transformer.getContent().contains("<sequenceNumber>0</sequenceNumber>"));
        assertTrue(transformer.getContent().contains("dummy transformer"));
    }

    @Test
    public void testPluginPropertiesExtraction() throws Exception {
        String xml = "<channel version=\"3.9.1\">"
                + "<id>test-id</id><name>test</name><revision>1</revision>"
                + "<sourceConnector version=\"3.9.1\">"
                + "  <metaDataId>0</metaDataId><name>sourceConnector</name>"
                + "  <properties class=\"com.mirth.connect.connectors.tcp.TcpReceiverProperties\">"
                + "    <pluginProperties>"
                + "      <org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties version=\"4.5.2\">"
                + "        <isTlsManagerEnabled>true</isTlsManagerEnabled>"
                + "        <clientCertificateAlias>test-cert</clientCertificateAlias>"
                + "      </org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties>"
                + "    </pluginProperties>"
                + "  </properties>"
                + "  <transformer version=\"3.9.1\"><elements/></transformer>"
                + "  <filter version=\"3.9.1\"><elements/></filter>"
                + "</sourceConnector>"
                + "<destinationConnectors>"
                + "  <connector version=\"3.9.1\">"
                + "    <metaDataId>1</metaDataId><name>Dest1</name>"
                + "    <properties class=\"com.mirth.connect.connectors.tcp.TcpDispatcherProperties\">"
                + "      <pluginProperties>"
                + "        <org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties version=\"4.5.2\">"
                + "          <isTlsManagerEnabled>false</isTlsManagerEnabled>"
                + "        </org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties>"
                + "        <com.example.another.PluginProperties>"
                + "          <setting>value</setting>"
                + "        </com.example.another.PluginProperties>"
                + "      </pluginProperties>"
                + "    </properties>"
                + "    <transformer version=\"3.9.1\"><elements/></transformer>"
                + "    <filter version=\"3.9.1\"><elements/></filter>"
                + "  </connector>"
                + "</destinationConnectors>"
                + "</channel>";

        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // Source connector has one plugin
        String srcPluginKey = "Source Connector/Plugin: TLSConnectorProperties";
        DecomposedComponent srcPlugin = components.get(srcPluginKey);
        assertNotNull("Source TLS plugin should be extracted", srcPlugin);
        assertEquals(DecomposedComponent.Category.CONNECTOR_PLUGIN, srcPlugin.getCategory());
        assertEquals("Source Connector", srcPlugin.getParentGroup());
        assertTrue(srcPlugin.getContent().contains("test-cert"));

        // Destination has two plugins
        String destTlsKey = "Destination: Dest1 [1]/Plugin: TLSConnectorProperties";
        DecomposedComponent destTls = components.get(destTlsKey);
        assertNotNull("Dest TLS plugin should be extracted", destTls);
        assertTrue(destTls.getContent().contains("isTlsManagerEnabled"));

        String destOtherKey = "Destination: Dest1 [1]/Plugin: PluginProperties";
        DecomposedComponent destOther = components.get(destOtherKey);
        assertNotNull("Dest second plugin should be extracted", destOther);
        assertTrue(destOther.getContent().contains("value"));

        // Plugins should NOT appear in the connector Configuration
        String srcConfig = components.get("Source Connector/Configuration").getContent();
        assertFalse(srcConfig.contains("TLSConnectorProperties"));
        assertFalse(srcConfig.contains("test-cert"));

        String destConfig = components.get("Destination: Dest1 [1]/Configuration").getContent();
        assertFalse(destConfig.contains("TLSConnectorProperties"));
        assertFalse(destConfig.contains("PluginProperties"));
    }

    @Test
    public void testEmptyPluginPropertiesProducesNoComponents() throws Exception {
        // The standard test XML has empty <pluginProperties/> — no plugin components should be created
        String xml = loadResource("channel-for-diffing-version1.xml");
        Map<String, DecomposedComponent> components = ChannelXmlDecomposer.decompose(xml);

        // No plugin components for any connector
        for (Map.Entry<String, DecomposedComponent> entry : components.entrySet()) {
            assertNotEquals("No plugin components expected, but found: " + entry.getKey(),
                    DecomposedComponent.Category.CONNECTOR_PLUGIN, entry.getValue().getCategory());
        }
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
