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

package org.apromore.plugin.portal.bimp;

import org.apromore.dao.model.Log;
import org.apromore.plugin.portal.PortalContext;
import org.apromore.plugin.portal.bimp.client.BIMPClient;
import org.apromore.plugin.portal.bimp.client.BIMPClientImpl;
import org.apromore.plugin.portal.bimp.exception.ScenarioNotFoundException;
import org.apromore.plugin.portal.bimp.model.SimulationRequest;
import org.apromore.plugin.portal.bimp.model.SimulationResponse;
import org.apromore.plugin.portal.bimp.service.SimulationRequestService;
import org.apromore.plugin.portal.bimp.service.SimulationResponseService;
import org.apromore.service.EventLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.zkoss.util.Locales;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

/**
 * Controller for <code>bimp.zul</code>.
 */
public class BIMPController extends SelectorComposer<Window> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BIMPController.class);

    private final PortalContext portalContext = (PortalContext) Sessions.getCurrent().getAttribute("portalContext");
    private final EventLogService eventLogService = (EventLogService) SpringUtil.getBean("eventLogService");
    private String bpmn = (String) Executions.getCurrent().getArg().get("bpmn");

    @Wire("#logNameTextbox")
    private Textbox logNameTextbox;

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

    @Listen("onClick = #generateButton")
    public void onClickGenerateButton(MouseEvent event) {
        try {
            SimulationRequestService simulationRequestService = new SimulationRequestService();
            SimulationRequest bimpRequest = simulationRequestService.createBIMPRequest(bpmn);

            BIMPClient bimpClient = new BIMPClientImpl();
            SimulationResponse simulationResponse = bimpClient.postSimulation(bimpRequest);

            SimulationResponseService simulationResponseService = new SimulationResponseService();
            String simulationId = simulationResponseService.getSimulationId(simulationResponse);

            InputStream simulationMXMLLog = bimpClient.getSimulationMXMLLogs(simulationId);

            PortalContext portalContext = this.portalContext;
            Log log = eventLogService.importLog(
                    portalContext.getCurrentUser().getUsername(), portalContext.getCurrentFolder().getId(),
                    logNameTextbox.getValue(),
                    simulationMXMLLog,
                    "mxml.gz",
                    "",
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toString(),
                    false
            );

//            Messagebox.show("A new log named '" + log.getName() + "' has been saved in the '" +
//                    log.getFolder().getName() + "' folder.", "Success", Messagebox.OK, Messagebox.NONE);

            getSelf().detach();
            portalContext.refreshContent();
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException | JAXBException | ScenarioNotFoundException e) {
            LOGGER.error("Error ", e);
            Messagebox.show(e.getMessage(), "Attention", Messagebox.OK, Messagebox.ERROR);
        } catch (Exception e) {
            LOGGER.error("Error ", e);
            e.printStackTrace();
        }
    }

    private void close() {
        getSelf().detach();
        getSelf().invalidate();
    }
}
