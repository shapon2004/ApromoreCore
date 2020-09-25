/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.apromore.plugin.portal.bimp;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;

public class BIMPRequestBodyBuilderImpl extends BIMPRequestBodyBuilder {

    @Override
    public String createBIMPRequestBody(Document document) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        // Create StartSimulationRequest wrapper
        Element startSimulationRequest = document.createElement("StartSimulationRequest");
        startSimulationRequest.setAttribute("xmlns", "http://www.qbp-simulator.com/ApiSchema201212");
        startSimulationRequest.setAttribute("version", "1");
        startSimulationRequest.setAttribute("generateMXML", "true");

        // Create modelData that contains simulation CDATA
        Element modelData = document.createElement("modelData");
        startSimulationRequest.appendChild(modelData);

        // Define simulation scenario nodes
        Node definitionsNode = (Node) xPath.compile(definitionsPath).evaluate(document, XPathConstants.NODE);
        Node processNode = (Node) xPath.compile(processPath).evaluate(document, XPathConstants.NODE);
        Node extensionElementsNode = (Node) xPath.compile(extensionElementsPath).evaluate(document, XPathConstants.NODE);
        Node processSimulationInfoNode = (Node) xPath.compile(processSimulationInfoPath).evaluate(document, XPathConstants.NODE);

        // Move processSimulationInfo from extensionElements to the definitions
        definitionsNode.appendChild(processSimulationInfoNode);
        processNode.removeChild(extensionElementsNode);

        // Create simulation CDATA
        CDATASection scenarioData = document.createCDATASection(convertNodeToString(definitionsNode, false));
        modelData.appendChild(scenarioData);

        return convertNodeToString(startSimulationRequest, true);
    }

    private String convertNodeToString(Node node, boolean omitXMLDeclaration) {
        try {
            StringWriter writer = new StringWriter();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration ? "yes" : "no");
            transformer.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return "";
    }
}
