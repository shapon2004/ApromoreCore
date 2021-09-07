/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
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

package org.apromore.service.loganimation.impl;

import java.io.IOException;
// Java 2 Standard Edition
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.plugin.DefaultParameterAwarePlugin;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.jgraph.ProMJGraph;
import org.apromore.processmining.plugins.bpmn.BpmnDefinitions;
import org.apromore.service.loganimation.AnimationResult;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.json.AnimationJSONBuilder2;
import org.apromore.service.loganimation.replay.AnimationLog;
import org.apromore.service.loganimation.replay.LogAlignment;
import org.apromore.service.loganimation.utils.BPMNDiagramHelper;
import org.apromore.service.loganimation.utils.LogUtility;
import org.apromore.service.loganimation.replay.ReplayParams;
import org.apromore.service.loganimation.replay.ReplayTrace;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.json.JSONException;
import org.json.JSONObject;
//import org.apromore.processmining.plugins.signaturediscovery.encoding.EncodeTraces;
//import org.apromore.processmining.plugins.signaturediscovery.encoding.EncodingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import de.hpi.bpmn2_0.exceptions.BpmnConverterException;
import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.transformation.BPMN2DiagramConverter;

@Service("logAnimationService2")
@Qualifier("logAnimationService2")
public class LogAnimationServiceImpl2 extends DefaultParameterAwarePlugin implements LogAnimationService2 {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimationServiceImpl2.class);

    @Override
    public AnimationResult createAnimation(BPMNDiagram bpmnDiagram, List<Log> logs) throws AnimationException {
        Definitions oldBpmnDiagram = convertToOldBPMNDiagram(getBPMN(bpmnDiagram, null), true);

        cleanLogs(logs);

        return doCreateAnimation(bpmnDiagram, oldBpmnDiagram, logs, createReplayParams(logs));
    }

    @Override
    public AnimationResult createAnimationWithNoGateways(BPMNDiagram bpmnDiagramNoGateways, List<Log> logs)
            throws AnimationException {

        BPMNDiagram bpmnDiagramWithGateways = BPMNDiagramHelper.createBPMNDiagramWithGateways(bpmnDiagramNoGateways);
        Definitions oldBpmnDiagramWithGateways = convertToOldBPMNDiagram(getBPMN(bpmnDiagramWithGateways, null), true);
        Definitions oldBpmnNoGateways = convertToOldBPMNDiagram(getBPMN(bpmnDiagramNoGateways, null), false);

        cleanLogs(logs);

        AnimationResult animResult = doCreateAnimation(bpmnDiagramWithGateways, oldBpmnDiagramWithGateways, logs, createReplayParams(logs));

        transformToNoGateways(animResult, oldBpmnNoGateways);

        return animResult;
    }

    private Definitions convertToOldBPMNDiagram(String bpmnDiagram, boolean checkValid) throws AnimationException {
        try {
            Definitions oldBpmnDiagram = BPMN2DiagramConverter.parseBPMN(bpmnDiagram, getClass().getClassLoader());
            BPMNDiagramHelper oldBpmnHelper = new BPMNDiagramHelper(oldBpmnDiagram);
            if (checkValid && !oldBpmnHelper.isValidModel()) {
                throw new AnimationException("The BPMN diagram is not valid for animation.");
            }
            return oldBpmnDiagram;
        }
        catch (JAXBException ex) {
            throw new AnimationException("An internal error occurred during parsing BPMN XML. Please check system logs");
        }
    }

    private void transformToNoGateways(AnimationResult animationResult, Definitions oldBpmnNoGateways) throws AnimationException {
        try {
            ElementIDMapper diagramMapping = new ElementIDMapper(oldBpmnNoGateways);
            for (AnimationLog animationLog : animationResult.getAnimationLogs()) {
                transformToNonGateways(animationLog, diagramMapping);
                animationLog.setDiagram(oldBpmnNoGateways);
            }
        }
        catch (DiagramMappingException ex) {
            LOGGER.error(ex.getMessage());
            throw new AnimationException("An internal error occurred during animation result transformation. Please check system logs.");
        }
    }

    private AnimationResult doCreateAnimation(BPMNDiagram bpmnDiagram, Definitions oldBpmnDiagram, List<Log> logs, ReplayParams params) {
        LogAlignment logAlignment = new LogAlignment(bpmnDiagram, oldBpmnDiagram, params);
        List<AnimationLog> animationLogs = logAlignment.align(logs);
        AnimationJSONBuilder2 jsonBuilder = new AnimationJSONBuilder2(animationLogs, oldBpmnDiagram, params);
        JSONObject json = jsonBuilder.parseLogCollection();
        json.put("success", true);  // Ext2JS's file upload requires this flag
        return new AnimationResult(animationLogs, oldBpmnDiagram, json);
    }

    private ReplayParams createReplayParams(List<Log> logs) throws AnimationException {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("properties.xml");
            Properties props = new Properties();
            props.loadFromXML(is);

            ReplayParams params = new ReplayParams();
            params.setTimelineSlots(Integer.valueOf(props.getProperty("TimelineSlots")).intValue());
            params.setTotalEngineSeconds(Integer.valueOf(props.getProperty("TotalEngineSeconds")).intValue());
            params.setTotalEngineSeconds(600); //Override this setting for testing

            int artificialTransitionRatio = Integer.valueOf(props.getProperty("ArtificialTransitionDurationRatio")).intValue();
            int artificalTransitionDur = (int)computeArtificialTransitionDuration(logs, artificialTransitionRatio);
            params.setStartEventToFirstEventDuration(artificalTransitionDur);
            params.setLastEventToEndEventDuration(artificalTransitionDur);

            return params;
        } catch (IOException e) {
            throw new AnimationException("An internal error occurred when reading animation properties file. Please check system logs");
        }
    }
    
    // The input animation log will be modified after the call to this method
    private void transformToNonGateways(AnimationLog diagramAnimationLog, ElementIDMapper diagramMapping) throws DiagramMappingException {
        for (ReplayTrace trace : diagramAnimationLog.getTraces()) {
            trace.convertToNonGateways();
            for (FlowNode node : trace.getNodes()) {
                String newId = diagramMapping.getId(node);
                if (newId.equals(ElementIDMapper.UNFOUND)) {
                    throw new DiagramMappingException("Couldn't find id for the node with name = " + node.getName());
                }
                node.setId(newId);
            }
            for (SequenceFlow flow : trace.getSequenceFlows()) {
                String newId = diagramMapping.getId(flow);
                if (newId.equals(ElementIDMapper.UNFOUND)) {
                    throw new DiagramMappingException("Couldn't find id for the sequence flow with name = " + flow.getName());
                }
                flow.setId(newId);
            }
        }
    }
    
    private void cleanLogs(List<Log> logs) {
        for (Log log : logs) {
            for (XTrace trace : log.xlog) {
                cleanTrace(trace);
            }
        }
    }

    /**
     * Clean the trace:
     *  - Remove events with lifecycle different from "complete" value
     *  - Make sure the only "complete" event logs have the same start and end timestamp as the original one
     */
    private void cleanTrace(XTrace trace) {
        if (trace == null || trace.isEmpty()) return;
        
        Date startTimestamp = LogUtility.getTimestamp(trace.get(0));
        Date endTimestamp = LogUtility.getTimestamp(trace.get(trace.size()-1));

        // Remove events with lifecycle different from "complete"
        Iterator<XEvent> iterator = trace.iterator();
        while (iterator.hasNext()) {
            XEvent event = iterator.next();
            if (!LogUtility.getLifecycleTransition(event).toLowerCase().equals("complete")) {
                iterator.remove();
            }
        }

        // After events have been removed, adjust the timestamp of the first/last events to ensure the clean log has a
        // matched start/end date with the original one.
        if (!trace.isEmpty()) {
            LogUtility.setTimestamp(trace.get(0), startTimestamp);
            if (trace.size() > 1) LogUtility.setTimestamp(trace.get(trace.size()-1), endTimestamp);
        }
    }

    /**
     * Compute an artificial transition duration which is used for the transition
     * from the Start Event to the next node and from a node to the End Event.
     * @param logs: logs used in animation
     * @param artificialTransDurRatio: the parameter for the artificial duration compared to the total timeline duration, e.g. 20 means 1/20
     * @return: artificial transition duration in seconds
     */
    private double computeArtificialTransitionDuration(List<Log> logs, int artificialTransDurRatio) {
        double UPPER_BOUND = 1.0/artificialTransDurRatio;
        double LOWER_BOUND =  1.0/(2*artificialTransDurRatio);
        
        // Scan the log the compute the average transition duration and log duration
        // Artificial transition duration is set according to the log
        double totalAvgTransitionDur = 0;
        long minLogTimestamp = Long.MAX_VALUE, maxLogTimestamp = 0;
        int traceCount = 0;
        for (Log log : logs) {
            for (XTrace trace : log.xlog) {
                minLogTimestamp = Math.min(minLogTimestamp, !trace.isEmpty() ? LogUtility.getTimestamp(trace.get(0)).getTime() : Long.MAX_VALUE);
                maxLogTimestamp = Math.max(maxLogTimestamp, !trace.isEmpty() ? LogUtility.getTimestamp(trace.get(trace.size()-1)).getTime() : 0);
                if (trace.size() >= 2) {
                    long traceDuration = LogUtility.getTimestamp(trace.get(trace.size()-1)).getTime() - LogUtility.getTimestamp(trace.get(0)).getTime();
                    totalAvgTransitionDur += traceDuration/(trace.size()-1);
                    traceCount++;
                }
            }
        }
        
        double avgTransitionDur = (traceCount != 0) ? totalAvgTransitionDur/traceCount : 0;
        long logDuration = (minLogTimestamp >= maxLogTimestamp) ? 0 : (maxLogTimestamp - minLogTimestamp);
        double timelineDur = logDuration + 2*avgTransitionDur;
        double avgTransitionRatio = (timelineDur > 0) ? avgTransitionDur/timelineDur : 0;
        
        // Adjust the artificial transition duration so that it's not too big or small.
        double artificialTransitionDur = avgTransitionDur/1000;
        if (avgTransitionRatio >= UPPER_BOUND) {
            artificialTransitionDur = timelineDur*UPPER_BOUND/1000;
        }
        else if (avgTransitionRatio <= LOWER_BOUND) {
            artificialTransitionDur = timelineDur*LOWER_BOUND/1000;
        }
        
        if (artificialTransitionDur == 0) artificialTransitionDur = 10;

        return artificialTransitionDur;
    }

    //Convert from BPMNDiagram to string
    private String getBPMN(BPMNDiagram diagram, ProMJGraph layoutInfo) {
        BpmnDefinitions.BpmnDefinitionsBuilder definitionsBuilder = null;
        if (layoutInfo != null) {
            definitionsBuilder = new BpmnDefinitions.BpmnDefinitionsBuilder(diagram, layoutInfo);
        }
        else {
            definitionsBuilder = new BpmnDefinitions.BpmnDefinitionsBuilder(diagram);
        }
        BpmnDefinitions definitions = new BpmnDefinitions("definitions", definitionsBuilder);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n " +
                "xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n " +
                "xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n " +
                "targetNamespace=\"http://www.omg.org/bpmn20\"\n " +
                "xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\">");
        sb.append(definitions.exportElements());
        sb.append("</definitions>");
        String bpmnText = sb.toString();
        bpmnText.replaceAll("\n", "");
        return bpmnText;
    }

}
