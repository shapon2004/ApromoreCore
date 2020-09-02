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
package org.apromore.plugin.portal.loganimation2.model;

import java.util.Collection;
import java.util.HashSet;

import org.apromore.service.loganimation2.replay.AnimationLog;
import org.apromore.service.loganimation2.replay.ReplayTrace;

public class AnimationFrameGenerator {
    private AnimationContext animationContext;
    
    public AnimationFrameGenerator(AnimationLog animationLog) {
        this.animationContext = new AnimationContext(animationLog);
    }
    
    public AnimationContext getAnimationSetting() {
        return this.animationContext;
    }
    
    public Chunk generateChunk(long startFrameIndex, int chunkSize) {
        Chunk chunk = new Chunk(startFrameIndex, chunkSize, animationContext);
        if (chunk.isEmpty()) return chunk;
        
        Collection<ReplayTrace> selectedTraces = new HashSet<>();
        for (ReplayTrace trace : animationContext.getLog().getTraces()) {
            if (trace.getStartDate().getMillis() > chunk.getEndTimestamp(animationContext) && 
                    trace.getEndDate().getMillis() < chunk.getStartTimestamp(animationContext)) {
                continue;
            }
            else {
                selectedTraces.add(trace);
            }
        }
        
        for (Frame frame : chunk) {
            long frameTimestamp = frame.getTimestamp(animationContext);
            for (ReplayTrace trace : selectedTraces) {
                if (frameTimestamp < trace.getStartDate().getMillis() || 
                        frameTimestamp > trace.getEndDate().getMillis()) {
                    continue;
                }
                frame.scanTrace(trace, animationContext);
            }
        }
        
        return chunk;

    }

}
