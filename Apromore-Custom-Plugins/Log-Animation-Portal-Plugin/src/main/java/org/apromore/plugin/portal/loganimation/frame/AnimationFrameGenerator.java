package org.apromore.plugin.portal.loganimation.frame;

import java.util.Collection;
import java.util.Collections;

import de.hpi.bpmn2_0.animation.AnimationLog;
import de.hpi.bpmn2_0.replay.ReplayTrace;

public class AnimationFrameGenerator {
    private AnimationLog animationLog;
    private AnimationSetting animationSetting;
    
    public AnimationFrameGenerator(AnimationLog animationLog) {
        this.animationLog = animationLog;
        this.animationSetting = new AnimationSetting();
        animationSetting.setStartTimestamp(animationLog.getStartDate().getMillis());
        animationSetting.setFPS(24);
        animationSetting.setFrameGap(41);
        animationSetting.setChunkSize(4920);
    }
    
    public FrameChunk generateFrameChunk(long startFrameIndex) {
        long chunkStart = FrameConverter.getTimestampFromFrameIndex(startFrameIndex, animationSetting.getStartTimestamp(), 
                                                animationSetting.getFrameGap());
        long chunkEnd = animationSetting.getStartTimestamp() + animationSetting.getChunkSize()*animationSetting.getFrameGap();
        FrameChunk frameChunk = new FrameChunk();
        for (int i=0;i<animationSetting.getChunkSize();i++) {
            frameChunk.add(new Frame(startFrameIndex + i));
        }
        
        Collection<ReplayTrace> selectedTraces = Collections.emptyList();
        for (ReplayTrace trace : animationLog.getTraces()) {
            if (trace.getStartDate().getMillis() > chunkEnd && trace.getEndDate().getMillis() < chunkStart) {
                continue;
            }
            else {
                selectedTraces.add(trace);
            }
        }
        
        for (Frame frame : frameChunk) {
            for (ReplayTrace trace : selectedTraces) {
                frame.scanTrace(trace, animationSetting);
            }
        }
        
        return frameChunk;

    }

}
