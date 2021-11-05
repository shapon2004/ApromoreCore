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

package org.apromore.service.loganimation2.recording;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.ALog;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnImportPlugin;
import org.apromore.service.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.enablement.AttributeLogEnablement;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.apromore.service.loganimation2.enablement.CompositeAttributeLogEnablement;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.mockito.Mock;
import org.mockito.Mockito;

public class TestDataSetup {
    private final BpmnImportPlugin bpmnImport = new BpmnImportPlugin();

    @Mock
    protected AlignmentClient alignmentServiceClient;

    protected LogAnimationService2 logAnimationService = null;

    public static void main(String[] args) {
        TestDataSetup testDataSetup = new TestDataSetup();
        try {
            System.out.println(System.getProperty("system.dir"));
            SortedMap<String, List<EnablementResult>> test = testDataSetup.createEnablements(
                    "C:/apromore\\ApromoreEE/ApromoreCore/Apromore-Custom-Plugins/Log-Animation-Logic2/" +
                    "/src/test/logs/d1_1trace_complete_events_abd.json");
            System.out.println(test);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SortedMap<String, List<EnablementResult>> createEnablements(String jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        FileInputStream is = new FileInputStream(jsonFile);
        Map<String, List<EnablementResult>> results = objectMapper.readValue(new FileInputStream(jsonFile),
                                            new TypeReference<>(){});
        return new TreeMap<>(results);
    }

    public CompositeAttributeLogEnablement animate_model_d1_1trace() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d1.bpmn");
        AttributeLog attLog = new ALog(readXESFile("src/test/logs/d1_1trace_complete_events_abd.xes"))
                                    .getDefaultActivityLog();
        LogAnimationService2 logAnimationService = new LogAnimationServiceImpl2(alignmentServiceClient);
        Mockito.when(alignmentServiceClient.computeEnablement(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(createEnablements("src/test/logs/d1_1trace_complete_events_abd.json"));
        return logAnimationService.createAnimationData(diagram, List.of(attLog));
    }

    public CompositeAttributeLogEnablement animate_graph_d2_1trace() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d2_graph.bpmn");
        AttributeLog attLog = new ALog(readXESFile("src/test/logs/d2_1trace_a.xes"))
                .getDefaultActivityLog();
        LogAnimationService2 logAnimationService = new LogAnimationServiceImpl2(alignmentServiceClient);
        Mockito.mockStatic(BPMNDiagramHelper.class).when(() -> BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram))
                .thenReturn(readBPMNDiagram("src/test/logs/d2_model.bpmn"));
        Mockito.when(alignmentServiceClient.computeEnablement(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(createEnablements("src/test/logs/d2_1trace_a.json"));
        return logAnimationService.createAnimationDataForGraph(diagram, List.of(attLog));
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
    
    private XLog readXESFile(String fullFilePath) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        return parser.parse(new File(fullFilePath)).get(0);
    }
    
    private BPMNDiagram readBPMNDiagram(String fullFilePath) throws Exception {
        return bpmnImport.importFromStreamToDiagram(new FileInputStream(fullFilePath), fullFilePath);
    }
    
    protected List<AnimationIndex> createAnimationIndexes(CompositeAttributeLogEnablement animationData, AnimationContext context) {
        List<AnimationIndex> animationIndexes = new ArrayList<>();
        for (AttributeLogEnablement log : animationData.getEnablements()) {
            animationIndexes.add(new AnimationIndex(log, animationData, context));
        }
        return animationIndexes;
    }
}
