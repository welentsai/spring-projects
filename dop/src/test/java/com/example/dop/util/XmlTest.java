package com.example.dop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

public class XmlTest {

    @Test
    void test_success() throws JsonProcessingException {
        String sampleXmlString = """
                <person id="100">
                    <firstName>Alice</firstName>
                    <lastName>Johnson</lastName>
                    <age>28</age>
                    <birthDate>1995-03-20</birthDate>
                    <email>alice.johnson@example.com</email>
                </person>
                """;

        // Configure mapper for records
        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(new ParameterNamesModule());

        // Parse to record
        QuickPerson person = mapper.readValue(sampleXmlString, QuickPerson.class);

        System.out.println(person);
    }

    @Test
    void test_large_xml() throws Exception {
        String sampleXml = getLargeXmlString();
        List<Order> allOrders = XmlUtil.extractNodes(sampleXml, "//order", Order.class);
        System.out.println(allOrders);

        XmlUtil.extractNode(sampleXml, "//order[@id='O001']", Order.class)
                .ifPresent(System.out::println);

        // Get order total as Double
        XmlUtil.extractElement(sampleXml, "//order[@id='O001']/total", Double.class)
                .ifPresent(System.out::println);

        // Get employee name as String
        XmlUtil.extractElement(sampleXml, "//employee[@id='E001']/name", String.class)
                .ifPresent(System.out::println);

        // Get birth date as LocalDate
        XmlUtil.extractElement(sampleXml, "//employee[@id='E001']/birthDate", LocalDate.class)
                .ifPresent(System.out::println);

        // Get all employee names
        List<String> names = XmlUtil.extractElements(sampleXml, "//employee/name", String.class);
        System.out.println(names);

        // Get all product prices
        List<Double> prices = XmlUtil.extractElements(sampleXml, "//product/price", Double.class);
        System.out.println(prices);
    }

    private String getLargeXmlString() {
        return """
                <company>
                    <metadata>
                        <name>TechCorp Inc.</name>
                        <founded>2010</founded>
                        <industry>Technology</industry>
                    </metadata>
                
                    <employees>
                        <employee id="E001">
                            <name>John Smith</name>
                            <department>Engineering</department>
                            <salary>75000.0</salary>
                            <position>Software Engineer</position>
                            <manager>Jane Doe</manager>
                        </employee>
                        <employee id="E002">
                            <name>Jane Doe</name>
                            <department>Engineering</department>
                            <salary>95000.0</salary>
                            <position>Senior Engineer</position>
                            <manager>Bob Wilson</manager>
                        </employee>
                        <employee id="E003">
                            <name>Alice Johnson</name>
                            <department>Marketing</department>
                            <salary>65000.0</salary>
                            <position>Marketing Manager</position>
                            <manager>Carol Brown</manager>
                        </employee>
                        <employee id="E004">
                            <name>Bob Wilson</name>
                            <department>Engineering</department>
                            <salary>120000.0</salary>
                            <position>Engineering Manager</position>
                            <manager>CEO</manager>
                        </employee>
                    </employees>
                
                    <products>
                        <product sku="P001">
                            <name>Laptop Pro</name>
                            <category>Electronics</category>
                            <price>1299.99</price>
                            <inStock>true</inStock>
                            <description>High-performance laptop</description>
                        </product>
                        <product sku="P002">
                            <name>Wireless Mouse</name>
                            <category>Electronics</category>
                            <price>29.99</price>
                            <inStock>true</inStock>
                            <description>Ergonomic wireless mouse</description>
                        </product>
                        <product sku="P003">
                            <name>Office Chair</name>
                            <category>Furniture</category>
                            <price>199.99</price>
                            <inStock>false</inStock>
                            <description>Comfortable office chair</description>
                        </product>
                    </products>
                
                    <orders>
                        <order id="O001">
                            <customerId>C001</customerId>
                            <orderDate>2024-01-15</orderDate>
                            <total>1329.98</total>
                            <status>completed</status>
                            <items>
                                <item>
                                    <productId>P001</productId>
                                    <quantity>1</quantity>
                                    <unitPrice>1299.99</unitPrice>
                                </item>
                                <item>
                                    <productId>P002</productId>
                                    <quantity>1</quantity>
                                    <unitPrice>29.99</unitPrice>
                                </item>
                            </items>
                        </order>
                        <order id="O002">
                            <customerId>C001</customerId>
                            <orderDate>2024-01-20</orderDate>
                            <total>199.99</total>
                            <status>pending</status>
                            <items>
                                <item>
                                    <productId>P003</productId>
                                    <quantity>1</quantity>
                                    <unitPrice>199.99</unitPrice>
                                </item>
                            </items>
                        </order>
                    </orders>
                </company>
                """;
    }
}
