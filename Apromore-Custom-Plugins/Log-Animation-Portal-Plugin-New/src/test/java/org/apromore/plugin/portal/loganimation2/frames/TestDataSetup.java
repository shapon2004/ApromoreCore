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

package org.apromore.plugin.portal.loganimation2.frames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.jgraph.ProMJGraph;
import org.apromore.processmining.plugins.bpmn.BpmnDefinitions;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnImportPlugin;
import org.apromore.service.loganimation2.LogAnimationService;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class TestDataSetup {
    private BpmnImportPlugin bpmnImport = new BpmnImportPlugin();
    private LogAnimationService logAnimationService = new LogAnimationServiceImpl();
    

    public AnimationLog animate_OneTraceAndCompleteEvents_Graph() throws Exception {
        return this.replay(this.readLog_OneTraceAndCompleteEvents(), 
                            this.readBPNDiagram_OneTraceAndCompleteEvents_WithGateways(),
                            this.readBPNDiagram_OneTraceAndCompleteEvents_NoGateways());
    }
    
    public AnimationLog animate_TwoTraceAndCompleteEvents_Graph() throws Exception {
        return this.replay(this.readLog_TwoTraceAndCompleteEvents(), 
                            this.readBPNDiagram_OneTraceAndCompleteEvents_WithGateways(),
                            this.readBPNDiagram_OneTraceAndCompleteEvents_NoGateways());
    }
    
    public JSONObject readFrame_OneTraceAndCompleteEvents(int frameIndex) throws JSONException, FileNotFoundException {
        String fileName = "frame" + frameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONObject(tokener);
    }
    
    public JSONArray readChunk_OneTraceAndCompleteEvents(int startFrameIndex) throws JSONException, FileNotFoundException {
        String fileName = "chunk" + startFrameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONArray(tokener);
    }
    
    private AnimationLog replay(XLog log, BPMNDiagram diagramWithGateways, BPMNDiagram diagramNoGateways) throws Exception {
        LogAnimationService.Log serviceLog = new LogAnimationService.Log();
        serviceLog.xlog = log;
        Object[] result = logAnimationService.createAnimationWithNoGateways(
                                                                getBPMN(diagramWithGateways, null), 
                                                                getBPMN(diagramNoGateways, null),
                                                                Arrays.asList(new LogAnimationService.Log[] {serviceLog}));
        if (result != null) {
            AnimationLog animationLog = (AnimationLog)result[1];
            return animationLog;
        }
        else {
            throw new Exception("Internal error: no animation was created, pls check the system logs.");
        }
    }
    
    private XLog readLog_OneTraceAndCompleteEvents() throws Exception {
        return this.readXESFile("src/test/logs/L1_1trace_complete_events_only.xes");
    }
    
    private XLog readLog_TwoTraceAndCompleteEvents() throws Exception {
        return this.readXESFile("src/test/logs/L1_2trace_complete_events_only.xes");
    }
    
    private BPMNDiagram readBPNDiagram_OneTraceAndCompleteEvents_WithGateways() throws Exception {
        return this.readBPMNDiagram("src/test/logs/L1_1trace_complete_events_only_with_gateways.bpmn");
    }
    
    private BPMNDiagram readBPNDiagram_OneTraceAndCompleteEvents_NoGateways() throws Exception {
        return this.readBPMNDiagram("src/test/logs/L1_1trace_complete_events_only_no_gateways.bpmn");
    }
    
    private XLog readXESFile(String fullFilePath) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        return parser.parse(new File(fullFilePath)).get(0);
    }
    
    private XLog readXESCompressedFile(String fullFilePath) throws Exception {
        XesXmlParser parser = new XesXmlGZIPParser();
        return parser.parse(new File(fullFilePath)).get(0);
    }
    
    private BPMNDiagram readBPMNDiagram(String fullFilePath) throws FileNotFoundException, Exception {
        return bpmnImport.importFromStreamToDiagram(new FileInputStream(new File(fullFilePath)), fullFilePath);
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
