package org.apromore.service.loganimation.recording;

import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.impl.ParamsReader;
import org.apromore.service.loganimation.replay.AnimationData;
import org.apromore.service.loganimation.replay.LogAnimation;
import org.junit.Test;

import java.util.List;

public class AnimationDataTest extends TestDataSetup {
    @Test
    public void test_AlignmentService_Simple() throws Exception {
        BPMNDiagram diagram = readBPNDiagram_abc_full_labels();
        List<LogAnimationService2.Log> logs = readLogs_ab();
        LogAnimation logAnimation  = new LogAnimation(diagram, ParamsReader.createAnimationParams(logs), false);
        AnimationData animationData = logAnimation.createAnimation(logs);
    }

    @Test
    public void test_AlignmentService_Simple2() throws Exception {
        BPMNDiagram diagram = readBPNDiagram_OneTraceAndCompleteEvents_WithGateways();
        List<LogAnimationService2.Log> logs = readLogs_OneTraceAndCompleteEvents();
        LogAnimation logAnimation  = new LogAnimation(diagram, ParamsReader.createAnimationParams(logs), false);
        AnimationData animationData = logAnimation.createAnimation(logs);
    }
}
