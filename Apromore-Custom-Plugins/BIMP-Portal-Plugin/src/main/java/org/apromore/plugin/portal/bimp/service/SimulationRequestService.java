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
package org.apromore.plugin.portal.bimp.service;

import org.apromore.plugin.portal.bimp.exception.ScenarioNotFoundException;
import org.apromore.plugin.portal.bimp.model.SimulationRequest;
import org.apromore.plugin.portal.bimp.utils.BIMPRequestBodyBuilder;
import org.apromore.plugin.portal.bimp.utils.BIMPRequestBodyBuilderImpl;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;

public class SimulationRequestService {

    public SimulationRequest createBIMPRequest(String process)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, ScenarioNotFoundException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new InputSource(new StringReader(process)));
        BIMPRequestBodyBuilder bimpRequestBodyBuilder = new BIMPRequestBodyBuilderImpl();

        return bimpRequestBodyBuilder.createBIMPRequestBody(document);
    }
}
