/**
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
package org.apromore.service.loganimation2.enablement;

import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.ALog;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.TestDataSetup;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AttributeLogAlignerTest extends TestDataSetup {

    @Mock
    private AlignmentClient alignmentClient;

    @InjectMocks
    private AttributeLogAligner aligner;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void test_AnimationData_BPMN() throws Exception {
        CompositeLogEnablement animationData = createEnablement_d1_1trace();

        assertEquals(1633042800000L, animationData.getStartTimestamp());
        assertEquals(1633042920000L, animationData.getEndTimestamp());
        assertEquals(1, animationData.getEnablements().size());

        //EnablementLog
        LogEnablement enablementLog = animationData.getEnablements().get(0);
        assertEquals(1633042800000L, enablementLog.getStartTimestamp());
        assertEquals(1633042920000L, enablementLog.getEndTimestamp());
        assertEquals(Set.of("Case3.0"), enablementLog.getCaseIDs());
        assertEquals(0, enablementLog.getCaseIndexFromId("Case3.0"));
        assertEquals(20, enablementLog.getEnablementsByCaseId("Case3.0").size());

        EnablementTuple tuple0 = enablementLog.getEnablementsByCaseId("Case3.0").get(0);
        assertEquals("StartEvent_13jlt06", tuple0.getElementId());
        assertEquals(0, tuple0.getStartTimestamp());
        assertEquals(0, tuple0.getEndTimestamp());

        EnablementTuple tuple19 = enablementLog.getEnablementsByCaseId("Case3.0").get(19);
        assertEquals("Event_1ecc5ul", tuple19.getElementId());
        assertEquals(1633042920000L, tuple19.getStartTimestamp());
        assertEquals(1633042920000L, tuple19.getEndTimestamp());
    }

    @Test
    public void test_AnimationData_Graph() throws Exception {
        CompositeLogEnablement animationData = createEnablement_d2_1trace();

        assertEquals(1633042800000L, animationData.getStartTimestamp());
        assertEquals(1633042800000L, animationData.getEndTimestamp());
        assertEquals(1, animationData.getEnablements().size());

        //EnablementLog
        LogEnablement enablementLog = animationData.getEnablements().get(0);
        assertEquals(1633042800000L, enablementLog.getStartTimestamp());
        assertEquals(1633042800000L, enablementLog.getEndTimestamp());
        assertEquals(Set.of("Case1"), enablementLog.getCaseIDs());
        assertEquals(0, enablementLog.getCaseIndexFromId("Case1"));
        assertEquals(5, enablementLog.getEnablementsByCaseId("Case1").size());

        EnablementTuple tuple0 = enablementLog.getEnablementsByCaseId("Case1").get(0);
        assertEquals("StartEvent_0ea7paa", tuple0.getElementId()); // verify: converted to graph diagram
        assertEquals(0, tuple0.getStartTimestamp());
        assertEquals(0, tuple0.getEndTimestamp());

        EnablementTuple tuple1 = enablementLog.getEnablementsByCaseId("Case1").get(1);
        assertEquals("Flow_0enzyac", tuple1.getElementId()); // verify: converted to graph diagram
        assertEquals(0, tuple1.getStartTimestamp());
        assertEquals(0, tuple1.getEndTimestamp());
    }

    private CompositeLogEnablement createEnablement_d1_1trace() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d1.bpmn");
        AttributeLog attLog = new ALog(readXESFile("src/test/logs/d1_1trace_complete_events_abd.xes"))
                .getDefaultActivityLog();
        Mockito.when(alignmentClient.computeEnablement(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(createEnablements("src/test/logs/d1_1trace_complete_events_abd.json"));
        return aligner.createEnablement(diagram, List.of(attLog));
    }

    private CompositeLogEnablement createEnablement_d2_1trace() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d2_graph.bpmn");
        AttributeLog attLog = new ALog(readXESFile("src/test/logs/d2_1trace_a.xes"))
                .getDefaultActivityLog();
        Mockito.mockStatic(BPMNDiagramHelper.class).when(() -> BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram))
                .thenReturn(readBPMNDiagram("src/test/logs/d2_model.bpmn"));
        Mockito.when(alignmentClient.computeEnablement(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(createEnablements("src/test/logs/d2_1trace_a.json"));
        return aligner.createEnablementForGraph(diagram, List.of(attLog));
    }
}
