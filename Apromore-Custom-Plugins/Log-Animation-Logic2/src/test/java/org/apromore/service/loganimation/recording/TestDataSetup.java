/*-
 * #%L
 * This file is part of "Apromore Core".
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

package org.apromore.service.loganimation.recording;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnImportPlugin;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.apromore.service.loganimation2.recording.*;
import org.apromore.service.loganimation2.replay.AnimationData;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.collect.Lists;

public class TestDataSetup {
    private BpmnImportPlugin bpmnImport = new BpmnImportPlugin();
    private LogAnimationService2 logAnimationService = new LogAnimationServiceImpl2();
    
    public AnimationData animate_OneTraceAndCompleteEvents_Graph() throws Exception {
        return this.replay(this.readLog_OneTraceAndCompleteEvents(),
                            //this.readBPNDiagram_OneTraceAndCompleteEvents_WithGateways(),
                            this.readBPNDiagram_OneTraceAndCompleteEvents_NoGateways());
    }
    
    public AnimationData animate_OneTraceAndCompleteEvents_BPMNDiagram() throws Exception {
        return this.replay(Arrays.asList(this.readLog_OneTraceAndCompleteEvents()),
                            this.readBPNDiagram_OneTraceAndCompleteEvents_WithGateways());
    }
    
    public AnimationData animate_TwoTraceAndCompleteEvents_Graph() throws Exception {
        return this.replay(this.readLog_TwoTraceAndCompleteEvents(),
                            //this.readBPNDiagram_OneTraceAndCompleteEvents_WithGateways(),
                            this.readBPNDiagram_OneTraceAndCompleteEvents_NoGateways());
    }
    
    public AnimationData animate_OneTraceOneEvent_OneTaskGraph() throws Exception {
        return this.replay(this.readLog_OneTraceOneEvent(),
                            //this.readBPNDiagram_OneTask(),
                            this.readBPNDiagram_OneTask());
    }
    
    public AnimationData animate_TwoTracesOneEvent_OneTaskGraph() throws Exception {
        return this.replay(this.readLog_TwoTracesOneEvent(),
                            //this.readBPNDiagram_OneTask(),
                            this.readBPNDiagram_OneTask());
    }
    
    public AnimationData animate_OneLog_With_BPMNDiagram() throws Exception {
        return this.replay(Arrays.asList(this.readLog_ab(), this.readLog_ac()), this.readBPNDiagram_abc());
    }
    
    public AnimationData animate_TwoLogs_With_BPMNDiagram() throws Exception {
        return this.replay(Arrays.asList(this.readLog_ab(), this.readLog_ac()), this.readBPNDiagram_abc());
    }
    
    public JSONObject readFrame_OneTraceAndCompleteEvents(int frameIndex) throws JSONException, FileNotFoundException {
        String fileName = "frame" + frameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONObject(tokener);
    }
    
    public JSONObject readFrame_TwoLogs(int frameIndex) throws JSONException, FileNotFoundException {
        String fileName = "twologs_frame" + frameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONObject(tokener);
    }
    
    public JSONArray readChunk_OneTraceAndCompleteEvents(int startFrameIndex) throws JSONException, FileNotFoundException {
        String fileName = "chunk" + startFrameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONArray(tokener);
    }
    
    public JSONArray readChunk_TwoLogs(int startFrameIndex) throws JSONException, FileNotFoundException {
        String fileName = "twologs_chunk" + startFrameIndex + ".json";
        JSONTokener tokener = new JSONTokener(new FileReader("src/test/logs/" + fileName));
        return new JSONArray(tokener);
    }
    
    private AnimationData replay(XLog log, BPMNDiagram diagramNoGateways) throws Exception {
        LogAnimationService2.Log serviceLog = new LogAnimationService2.Log("", log);
        return logAnimationService.createAnimationForGraph(diagramNoGateways,
                                                                Arrays.asList(new LogAnimationService2.Log[] {serviceLog}));
    }
    
    private AnimationData replay(List<XLog> logs, BPMNDiagram diagram) throws Exception {
        List<LogAnimationService2.Log> serviceLogs = Lists.newArrayList();
        logs.forEach(log -> {
            LogAnimationService2.Log serviceLog = new LogAnimationService2.Log("", log);
            serviceLogs.add(serviceLog);
        });
        return logAnimationService.createAnimation(diagram, serviceLogs);
    }
    
    private XLog readLog_OneTraceOneEvent() throws Exception {
        return this.readXESFile("src/test/logs/L1_1trace_1event.xes");
    }
    
    private XLog readLog_TwoTracesOneEvent() throws Exception {
        return this.readXESFile("src/test/logs/L1_2trace_1event.xes");
    }
    
    private XLog readLog_OneTraceAndCompleteEvents() throws Exception {
        return this.readXESFile("src/test/logs/L1_1trace_complete_events_only.xes");
    }

    public List<LogAnimationService2.Log> readLogs_OneTraceAndCompleteEvents() throws Exception {
        return Arrays.asList(this.readXESFile("src/test/logs/L1_1trace_complete_events_only.xes"))
                .stream()
                .map(log -> new LogAnimationService2.Log("L1_1trace_complete_events_only.xes", log))
                .collect(Collectors.toList());
    }
    
    private XLog readLog_TwoTraceAndCompleteEvents() throws Exception {
        return this.readXESFile("src/test/logs/L1_2trace_complete_events_only.xes");
    }
    
    public XLog readLog_ab() throws Exception {
        return this.readXESFile("src/test/logs/ab.xes");
    }

    public List<LogAnimationService2.Log> readLogs_ab() throws Exception {
        return Arrays.asList(this.readXESFile("src/test/logs/ab.xes"))
                .stream()
                .map(log -> new LogAnimationService2.Log("ab.xes", log))
                .collect(Collectors.toList());
    }
    
    public XLog readLog_ac() throws Exception {
        return this.readXESFile("src/test/logs/ac.xes");
    }
    
    private BPMNDiagram readBPNDiagram_OneTask() throws Exception {
        return this.readBPMNDiagram("src/test/logs/L1_1task.bpmn");
    }
    
    public BPMNDiagram readBPNDiagram_OneTraceAndCompleteEvents_WithGateways() throws Exception {
        return this.readBPMNDiagram("src/test/logs/L1_1trace_complete_events_only_with_gateways.bpmn");
    }
    
    private BPMNDiagram readBPNDiagram_OneTraceAndCompleteEvents_NoGateways() throws Exception {
        return this.readBPMNDiagram("src/test/logs/L1_1trace_complete_events_only_no_gateways.bpmn");
    }
    
    public BPMNDiagram readBPNDiagram_abc() throws Exception {
        return this.readBPMNDiagram("src/test/logs/abc.bpmn");
    }

    public BPMNDiagram readBPNDiagram_abc_full_labels() throws Exception {
        return this.readBPMNDiagram("src/test/logs/abc_full_labels.bpmn");
    }
    
    private XLog readXESFile(String fullFilePath) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        return parser.parse(new File(fullFilePath)).get(0);
    }
    
    private BPMNDiagram readBPMNDiagram(String fullFilePath) throws FileNotFoundException, Exception {
        return bpmnImport.importFromStreamToDiagram(new FileInputStream(new File(fullFilePath)), fullFilePath);
    }

    private String readBPMNDiagramAsString(String fullFilePath) throws Exception {
        byte[] encoded = Files.readAllBytes(Paths.get(fullFilePath));
        return new String(encoded, StandardCharsets.UTF_8);
    }
    
    protected List<AnimationIndex> createAnimationIndexes(List<AnimationLog> logs, ModelMapping modelMapping, AnimationContext context) throws ElementNotFoundException, CaseNotFoundException {
        List<AnimationIndex> animationIndexes = new ArrayList<>();
        for (AnimationLog log : logs) {
            animationIndexes.add(new AnimationIndex(log, modelMapping, context));
        }
        return animationIndexes;
    }
}
