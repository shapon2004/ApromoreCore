/*-
 * #%L
 * This file is part of "Apromore Core".
 *
 * Copyright (C) 2020 University of Tartu
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

package org.apromore.plugin.portal.bimp.controller;

import org.apromore.plugin.portal.bimp.BIMPRequestBodyBuilder;
import org.apromore.plugin.portal.bimp.BIMPRequestBodyBuilderImpl;
import org.apromore.plugin.portal.bimp.client.BIMPClient;
import org.apromore.plugin.portal.bimp.client.BIMPClientImpl;
import org.apromore.service.EventLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zkoss.util.Locales;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ResourceBundle;

/**
 * Controller for <code>bimp.zul</code>.
 */
public class BIMPController extends SelectorComposer<Window> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BIMPController.class);

    private final EventLogService eventLogService = (EventLogService) SpringUtil.getBean("eventLogService");

    private String bpmn = (String) Executions.getCurrent().getArg().get("bpmn");

    @Wire("#testLabel")
    private Label testLabel;

    /**
     * @return the ZK localizations
     */
    public ResourceBundle getLabels() {
        return ResourceBundle.getBundle("WEB-INF.zk-label", Locales.getCurrent(), getClass().getClassLoader());
    }

    @Override
    public void doFinally() throws Exception {
        super.doFinally();

        // This method is called after all the injections have been performed and all the child UI components exist
    }

    @Listen("onClick = #cancelButton")
    public void onClickCancelButton(MouseEvent event) {
        close();
    }

    @Listen("onClick = #testButton")
    public void onClickTestButton(MouseEvent event) {
        testLabel.setValue(getLabels().getString("testLabel_clickedValue"));

        LOGGER.info("bpmn {}", bpmn);

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(new InputSource(new StringReader(bpmn)));

            BIMPRequestBodyBuilder bimpRequestBodyBuilder = new BIMPRequestBodyBuilderImpl();
            String bimpRequestBody = bimpRequestBodyBuilder.createBIMPRequestBody(document);

            BIMPClient bimpClient = new BIMPClientImpl();
            String s = bimpClient.postSimulation(bimpRequestBody);

            LOGGER.info(s);
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException | JAXBException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void close() {
        getSelf().detach();
        getSelf().invalidate();
    }
}
