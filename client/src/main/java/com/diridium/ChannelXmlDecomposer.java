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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.mirth.connect.util.MirthXmlUtil;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ChannelXmlDecomposer {

    public static Map<String, DecomposedComponent> decompose(String channelXml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
        Document doc = db.parse(new InputSource(new StringReader(channelXml)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        Map<String, DecomposedComponent> components = new LinkedHashMap<>();

        // Channel-level scripts
        extractChannelScript(doc, xpath, components, "preprocessingScript", "Preprocessing Script");
        extractChannelScript(doc, xpath, components, "postprocessingScript", "Postprocessing Script");
        extractChannelScript(doc, xpath, components, "deployScript", "Deploy Script");
        extractChannelScript(doc, xpath, components, "undeployScript", "Undeploy Script");

        // Source connector
        Node sourceConnector = (Node) xpath.evaluate("/channel/sourceConnector", doc, XPathConstants.NODE);
        if (sourceConnector != null) {
            String srcGroup = "Source Connector";
            extractConnectorScript(xpath, components, (Element) sourceConnector, srcGroup);
            extractPluginProperties(xpath, components, (Element) sourceConnector, srcGroup);
            extractSubElement(xpath, components, (Element) sourceConnector, srcGroup,
                    "filter", "Filter", DecomposedComponent.Category.FILTER);
            extractSubElement(xpath, components, (Element) sourceConnector, srcGroup,
                    "transformer", "Transformer", DecomposedComponent.Category.TRANSFORMER);

            // Serialize remaining source connector as Configuration
            String srcConfigXml = serializeNode(sourceConnector);
            String srcKey = srcGroup + "/Configuration";
            components.put(srcKey, new DecomposedComponent(srcKey, "Configuration", srcConfigXml,
                    DecomposedComponent.Category.CONNECTOR_CONFIGURATION, srcGroup));
            sourceConnector.getParentNode().removeChild(sourceConnector);
        }

        // Destination connectors
        NodeList destConnectors = (NodeList) xpath.evaluate(
                "/channel/destinationConnectors/connector", doc, XPathConstants.NODESET);
        List<Element> destElements = new ArrayList<>();
        List<String> destGroupNames = new ArrayList<>();
        for (int i = 0; i < destConnectors.getLength(); i++) {
            Element connector = (Element) destConnectors.item(i);
            String connName = getDirectChildText(connector, "name");
            String metaDataId = getDirectChildText(connector, "metaDataId");
            String groupName = "Destination: " + connName + " [" + metaDataId + "]";
            destElements.add(connector);
            destGroupNames.add(groupName);

            extractConnectorScript(xpath, components, connector, groupName);
            extractPluginProperties(xpath, components, connector, groupName);
            extractSubElement(xpath, components, connector, groupName,
                    "filter", "Filter", DecomposedComponent.Category.FILTER);
            extractSubElement(xpath, components, connector, groupName,
                    "transformer", "Transformer", DecomposedComponent.Category.TRANSFORMER);
            extractSubElement(xpath, components, connector, groupName,
                    "responseTransformer", "Response Transformer", DecomposedComponent.Category.RESPONSE_TRANSFORMER);
        }

        // Extract each destination connector configuration (after sub-elements removed)
        for (int i = 0; i < destElements.size(); i++) {
            Element connector = destElements.get(i);
            String groupName = destGroupNames.get(i);
            if (connector.getParentNode() != null) {
                String configXml = serializeNode(connector);
                String configKey = groupName + "/Configuration";
                components.put(configKey, new DecomposedComponent(configKey, "Configuration", configXml,
                        DecomposedComponent.Category.CONNECTOR_CONFIGURATION, groupName));
                connector.getParentNode().removeChild(connector);
            }
        }

        // Remove the now-empty destinationConnectors wrapper element
        Node destConnectorsWrapper = (Node) xpath.evaluate("/channel/destinationConnectors", doc, XPathConstants.NODE);
        if (destConnectorsWrapper != null) {
            destConnectorsWrapper.getParentNode().removeChild(destConnectorsWrapper);
        }

        // Channel Properties = the remainder XML (without scripts or connectors)
        String remainderXml = serializeDocument(doc);
        String key = "Channel Properties";

        // Reorder so Channel Properties appears first in the tree
        Map<String, DecomposedComponent> ordered = new LinkedHashMap<>();
        ordered.put(key, new DecomposedComponent(key, "Channel Properties", remainderXml,
                DecomposedComponent.Category.CHANNEL_PROPERTIES, key));
        ordered.putAll(components);

        return ordered;
    }

    private static void extractChannelScript(Document doc, XPath xpath,
            Map<String, DecomposedComponent> components,
            String elementName, String displayName) throws Exception {
        Node node = (Node) xpath.evaluate("/channel/" + elementName, doc, XPathConstants.NODE);
        if (node != null) {
            String content = node.getTextContent();
            String group = "Channel Scripts";
            String compKey = group + "/" + displayName;
            components.put(compKey, new DecomposedComponent(compKey, displayName, content,
                    DecomposedComponent.Category.CHANNEL_SCRIPT, group));
            node.getParentNode().removeChild(node);
        }
    }

    private static void extractConnectorScript(XPath xpath,
            Map<String, DecomposedComponent> components,
            Element connector, String groupName) throws Exception {
        Node node = (Node) xpath.evaluate("properties/script", connector, XPathConstants.NODE);
        if (node != null) {
            String content = node.getTextContent();
            String compKey = groupName + "/Script";
            components.put(compKey, new DecomposedComponent(compKey, "Script", content,
                    DecomposedComponent.Category.CONNECTOR_SCRIPT, groupName));
            node.getParentNode().removeChild(node);
        }
    }

    private static void extractPluginProperties(XPath xpath,
            Map<String, DecomposedComponent> components,
            Element connector, String groupName) throws Exception {
        Node pluginProps = (Node) xpath.evaluate("properties/pluginProperties", connector, XPathConstants.NODE);
        if (pluginProps == null) {
            return;
        }

        List<Element> plugins = new ArrayList<>();
        NodeList children = pluginProps.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                plugins.add((Element) children.item(i));
            }
        }

        for (Element plugin : plugins) {
            String pluginName = getStepTypeName(plugin.getTagName());
            String compKey = groupName + "/Plugin: " + pluginName;
            String content = serializeNode(plugin);
            components.put(compKey, new DecomposedComponent(compKey, "Plugin: " + pluginName, content,
                    DecomposedComponent.Category.CONNECTOR_PLUGIN, groupName));
            pluginProps.removeChild(plugin);
        }
    }

    private static void extractSubElement(XPath xpath,
            Map<String, DecomposedComponent> components,
            Element connector, String connectorGroup,
            String elementName, String displayName,
            DecomposedComponent.Category category) throws Exception {
        Node node = (Node) xpath.evaluate(elementName, connector, XPathConstants.NODE);
        if (node == null) {
            return;
        }

        String compKey = connectorGroup + "/" + displayName;
        String content = serializeNode(node);
        components.put(compKey, new DecomposedComponent(compKey, displayName, content,
                category, connectorGroup));
        connector.removeChild(node);
    }

    static String getStepTypeName(String tagName) {
        int lastDot = tagName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < tagName.length() - 1) {
            return tagName.substring(lastDot + 1);
        }
        return tagName;
    }

    private static String getDirectChildText(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && child.getNodeName().equals(childName)) {
                return child.getTextContent();
            }
        }
        return null;
    }

    private static String serializeNode(Node node) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return MirthXmlUtil.prettyPrint(writer.toString().trim()).trim();
    }

    private static String serializeDocument(Document doc) throws Exception {
        return serializeNode(doc);
    }
}
