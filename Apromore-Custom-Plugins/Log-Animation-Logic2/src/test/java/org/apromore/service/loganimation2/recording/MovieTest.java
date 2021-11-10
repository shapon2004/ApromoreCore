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

import org.apromore.logman.ALog;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.TestDataSetup;
import org.apromore.service.loganimation2.enablement.AttributeLogAligner;
import org.apromore.service.loganimation2.enablement.CompositeLogEnablement;
import org.apromore.service.loganimation2.enablement.LogEnablement;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MovieTest extends TestDataSetup {

    @Mock
    private AttributeLogAligner aligner;

    @InjectMocks
    private LogAnimationService2 logAnimationService = new LogAnimationServiceImpl2();

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void test_getChunkJSON_OneLog() throws Exception {
//        Movie movie = this.createMovie_d1_1trace();
//
//        Assert.assertEquals(36000, movie.size());
//
//        JSONArray firstChunk = movie.getChunkJSON(0, 300); // first chunk
//        JSONArray firstChunkExpect = this.readChunk_OneTraceAndCompleteEvents(0);
//        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
//
//        JSONArray lastChunk = movie.getChunkJSON(35817, 300); // last chunk
//        JSONArray lastChunkExpect = this.readChunk_OneTraceAndCompleteEvents(35817);
//        Assert.assertEquals(true, lastChunk.similar(lastChunkExpect));
    }
    
    @Test
    public void test_getChunkJSON_TwoLogs() throws Exception {
//        Movie movie = this.createMovie_d1_1trace();
//
//        Assert.assertEquals(36000, movie.size());
//
//        JSONArray firstChunk = movie.getChunkJSON(0, 300);
//        JSONArray firstChunkExpect = this.readChunk_TwoLogs(0);
//        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
//
//        JSONArray lastChunk = movie.getChunkJSON(35744, 300);
//        JSONArray lastChunkExpect = this.readChunk_TwoLogs(0);
//        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
    }

    protected Movie createMovie(BPMNDiagram diagram, AttributeLog log, CompositeLogEnablement logEnablement) throws Exception {
        Mockito.when(aligner.createEnablement(Mockito.any(), Mockito.any())).thenReturn(logEnablement);
        return logAnimationService.createAnimationMovie(diagram, List.of(log));
    }

    protected Movie createMovie_d1_1trace() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d1.bpmn");
        AttributeLog log = readAttributeLog("src/test/logs/d1_1trace_complete_events_abd.xes");
        Mockito.when(aligner.createEnablement(Mockito.any(), Mockito.any()))
                .thenReturn(createCompositeLogEnablement(diagram, log,
                        "src/test/logs/d1_1trace_complete_events_abd.json"));
        return logAnimationService.createAnimationMovie(diagram, List.of(log));
    }

    protected CompositeLogEnablement createCompositeLogEnablement(BPMNDiagram diagram,
                                                                AttributeLog log,
                                                                String enablementFile) throws Exception {
        return new CompositeLogEnablement(diagram, List.of(createLogEnablement(log, enablementFile)));
    }

    protected LogEnablement createLogEnablement(AttributeLog log, String jSonFile) throws IOException {
        return new LogEnablement(log, createEnablements(jSonFile));
    }
}
