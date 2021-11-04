/**
 * #%L
 * This file is part of "Apromore Enterprise Edition".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.service.loganimation2.recording;

import org.apromore.service.loganimation2.data.AnimationData;
import org.apromore.service.loganimation2.data.EnablementLog;
import org.apromore.service.loganimation2.data.EnablementTuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AnimationDataTest extends TestDataSetup {
    @Test
    public void test_AnimationData_BPMN() throws Exception {
        AnimationData animationData = animate_model_d1_1trace();

        assertEquals(1633042800000L, animationData.getStartTimestamp());
        assertEquals(1633042920000L, animationData.getEndTimestamp());
        assertEquals(1, animationData.getEnablementLogs().size());

        //EnablementLog
        EnablementLog enablementLog = animationData.getEnablementLogs().get(0);
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
        AnimationData animationData = animate_graph_d2_1trace();

        assertEquals(1633042800000L, animationData.getStartTimestamp());
        assertEquals(1633042800000L, animationData.getEndTimestamp());
        assertEquals(1, animationData.getEnablementLogs().size());

        //EnablementLog
        EnablementLog enablementLog = animationData.getEnablementLogs().get(0);
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
}
