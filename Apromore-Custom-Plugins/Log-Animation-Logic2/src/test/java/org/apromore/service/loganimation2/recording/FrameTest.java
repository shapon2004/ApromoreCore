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

import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.enablement.CompositeLogEnablement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FrameTest extends MovieTest {
    @Test
    public void test_FrameData_OneTraceLog() throws Exception {
        BPMNDiagram diagram = readBPMNDiagram("src/test/logs/d1.bpmn");
        AttributeLog log = readAttributeLog("src/test/logs/d1_1trace_complete_events_abd.xes");
        CompositeLogEnablement logEnablement = createCompositeLogEnablement(diagram, log,
                "src/test/logs/d1_1trace_complete_events_abd.json");
        Movie animationMovie = createMovie(diagram, log, logEnablement);

        // First frame
        Frame frame0 = animationMovie.get(0);
        assertEquals(0, frame0.getIndex());
        assertArrayEquals(new int[] {0}, frame0.getCaseIndexes(0));
        assertEquals(List.of(
                "StartEvent_13jlt06",
                "Flow_0p5ie2x",
                "Activity_08c2a6r",
                "Flow_1hor0wq",
                "Gateway_1himj9s",
                "Flow_0spidhr",
                "Gateway_11wnsyy",
                "Flow_01wj4a9",
                "Activity_0q3anvm",
                "Flow_02an7zo",
                "Activity_0tuf2hf",
                "Flow_0zui7au"),
                getElementIDs(frame0.getElementIndexes(0), logEnablement));
        assertEquals(12, frame0.getTokens(0).length);
        assertEquals(1, frame0.getTokensByElement(0,
                logEnablement.getElementIndex("StartEvent_13jlt06", false)).length);
        assertEquals(1, frame0.getTokensByElement(0,
                logEnablement.getElementIndex("Activity_0tuf2hf", true)).length);
        assertEquals(0, frame0.getTokensByElement(0,
                logEnablement.getElementIndex("Activity_0k0ztsm", false)).length);

        // Last frame
        Frame frame35999 = animationMovie.get(35999);
        assertEquals(35999, frame35999.getIndex());
        assertArrayEquals(new int[] {0}, frame35999.getCaseIndexes(0));
        assertEquals(List.of(
                        "Activity_0wx1wp2",
                        "Flow_0cau0oh",
                        "Event_1ecc5ul"),
                getElementIDs(frame35999.getElementIndexes(0), logEnablement));
        assertEquals(3, frame35999.getTokens(0).length);
        assertEquals(1, frame35999.getTokensByElement(0,
                logEnablement.getElementIndex("Event_1ecc5ul", false)).length);
        assertEquals(0, frame35999.getTokensByElement(0,
                logEnablement.getElementIndex("Flow_0spkzgr", false)).length);
    }
    
    @Test
    public void test_FrameJSON_OneTraceLog() throws Exception {
        Movie animationMovie = this.createMovie_d1_1trace();
//
//        JSONObject frame0 = animationMovie.get(0).getJSON();
//        JSONObject frame0Expect = this.readFrame_OneTraceAndCompleteEvents(0);
//        Assert.assertEquals(true, frame0Expect.similar(frame0));
//
//        JSONObject frame299 = animationMovie.get(299).getJSON();
//        JSONObject frame299Expect = this.readFrame_OneTraceAndCompleteEvents(299);
//        Assert.assertEquals(true, frame299Expect.similar(frame299));
//
//        JSONObject frame35990 = animationMovie.get(35990).getJSON();
//        JSONObject frame35990Expect = this.readFrame_OneTraceAndCompleteEvents(35990);
//        Assert.assertEquals(true, frame35990Expect.similar(frame35990));
//
//        JSONObject frame35999 = animationMovie.get(35999).getJSON();
//        JSONObject frame35999Expect = this.readFrame_OneTraceAndCompleteEvents(35999);
//        Assert.assertEquals(true, frame35999Expect.similar(frame35999));
    }
}
