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

package org.apromore.service.loganimation2;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnImportPlugin;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class TestDataSetup {
    private final BpmnImportPlugin bpmnImport = new BpmnImportPlugin();

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

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
    
    public XLog readXESFile(String fullFilePath) throws Exception {
        XesXmlParser parser = new XesXmlParser();
        return parser.parse(new File(fullFilePath)).get(0);
    }
    
    public BPMNDiagram readBPMNDiagram(String fullFilePath) throws Exception {
        return bpmnImport.importFromStreamToDiagram(new FileInputStream(fullFilePath), fullFilePath);
    }

    public SortedMap<String, List<EnablementResult>> createEnablements(String jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        FileInputStream is = new FileInputStream(jsonFile);
        Map<String, List<EnablementResult>> results = objectMapper.readValue(new FileInputStream(jsonFile),
                new TypeReference<>(){});
        return new TreeMap<>(results);
    }
}
