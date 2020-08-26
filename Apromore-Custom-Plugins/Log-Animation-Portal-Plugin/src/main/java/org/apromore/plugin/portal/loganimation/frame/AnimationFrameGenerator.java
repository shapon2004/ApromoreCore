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
package org.apromore.plugin.portal.loganimation.frame;

import java.util.Collection;
import java.util.Collections;

import de.hpi.bpmn2_0.replay.AnimationLog;
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
    
    public AnimationSetting getAnimationSetting() {
        return this.animationSetting;
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
