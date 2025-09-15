package com.example.dop.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class XmlUtil {
    private static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    private static final XmlMapper xmlMapper;
    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    // Static initialization block
    static {
        xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new ParameterNamesModule());
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        xmlMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
        xmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    public static void register(Consumer<XmlMapper> consumer) {
        consumer.accept(xmlMapper);
    }

    /**
     * Extract multiple nodes and convert to list of objects
     *
     * @param xmlDoc    the XML document as string
     * @param xpathExpr XPath expression to find nodes
     * @param clazz     target class type
     * @return List of objects
     */
    public static <T> List<T> extractNodes(String xmlDoc, String xpathExpr, Class<T> clazz) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlDoc.getBytes()));

            NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);

            List<T> results = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                String nodeXml = nodeToString(nodes.item(i));
                results.add(xmlMapper.readValue(nodeXml, clazz));
            }
            return results;
        } catch (Exception e) {
            logger.warn("Failed to extract nodes with XPath: " + xpathExpr, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract single node and convert to object
     *
     * @param xmlDoc    the XML document as string
     * @param xpathExpr XPath expression to find single node
     * @param clazz     target class type
     * @return Optional containing converted value, empty if not found or error occurs
     */
    public static <T> Optional<T> extractNode(String xmlDoc, String xpathExpr, Class<T> clazz) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlDoc.getBytes()));

            Node node = (Node) xpath.evaluate(xpathExpr, doc, XPathConstants.NODE);

            if (node == null) {
                return Optional.empty();
            }

            String nodeXml = nodeToString(node);
            T result = xmlMapper.readValue(nodeXml, clazz);
            return Optional.of(result);

        } catch (Exception e) {
            logger.warn("Failed to extract node with XPath: " + xpathExpr, e);
            return Optional.empty();
        }
    }

    /**
     * Extract multiple element values and convert to specified Java type
     *
     * @param xmlDoc     the XML document as string
     * @param xpathExpr  XPath expression to find elements
     * @param targetType target Java type
     * @return List of converted values
     */
    public static <T> List<T> extractElements(String xmlDoc, String xpathExpr, Class<T> targetType) throws Exception {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlDoc.getBytes()));

            NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);

            List<T> results = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                String value = nodes.item(i).getTextContent();
                if (value != null && !value.trim().isEmpty()) {
                    T convertedValue = convertToType(value.trim(), targetType);
                    if (convertedValue != null) {
                        results.add(convertedValue);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.warn("Failed to extract elements with XPath: " + xpathExpr, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract single element value and convert to specified Java type
     *
     * @param xmlDoc     the XML document as string
     * @param xpathExpr  XPath expression to find element
     * @param targetType target Java type
     * @return Optional containing converted value, empty if not found or error occurs
     */
    public static <T> Optional<T> extractElement(String xmlDoc, String xpathExpr, Class<T> targetType) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlDoc.getBytes()));

            String value = (String) xpath.evaluate(xpathExpr, doc, XPathConstants.STRING);

            if (value == null || value.trim().isEmpty()) {
                return Optional.empty();
            }

            T convertedValue = convertToType(value.trim(), targetType);
            return Optional.of(convertedValue);

        } catch (Exception e) {
            // Log the exception if needed
            // logger.warn("Failed to extract element with XPath: " + xpathExpr, e);
            return Optional.empty();
        }
    }

    /**
     * Convert string value to target type
     */
    @SuppressWarnings("unchecked")
    private static <T> T convertToType(String value, Class<T> targetType) throws Exception {
        if (targetType == String.class) {
            return (T) value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(value);
        } else if (targetType == Float.class || targetType == float.class) {
            return (T) Float.valueOf(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (targetType == LocalDate.class) {
            return (T) LocalDate.parse(value);
        } else if (targetType == LocalDateTime.class) {
            return (T) LocalDateTime.parse(value);
        } else {
            // For other types, try to use Jackson for conversion
            return xmlMapper.readValue("\"" + value + "\"", targetType);
        }
    }

    private static String nodeToString(Node node) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Check if XPath expression matches any nodes
     */
    public static boolean exists(String xmlDoc, String xpathExpr) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlDoc.getBytes()));

        NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);
        return nodes.getLength() > 0;
    }

    /**
     * Convert Java object to XML string
     *
     * @param object the object to convert
     * @return Optional containing XML string, empty if conversion fails
     */
    public static <T> Optional<String> objectToXml(T object) {
        try {
            String xmlString = xmlMapper.writeValueAsString(object);
            // Format the XML to match the expected indentation
            return Optional.of(xmlString);
        } catch (Exception e) {
            logger.warn("Failed to convert object to XML: " + object.getClass().getSimpleName(), e);
            return Optional.empty();
        }
    }
}
