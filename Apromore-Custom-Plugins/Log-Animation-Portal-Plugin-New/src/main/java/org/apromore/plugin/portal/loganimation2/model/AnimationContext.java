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

import org.apromore.service.loganimation.replay.AnimationLog;

public class AnimationContext {
    private int frameRate = 24; //frames per second
    private int frameInterval =  (int)(1.0/frameRate*1000); //milliseconds between two consecutive frames
    private int chunkSize = 4920; //number of frames
    private int totalDuration = 120; //seconds
    private AnimationLog log;
    private long startTimestamp;
    private long endTimestamp;
    private double timelineRatio = 1; //a second on the animation timeline is converted to actual seconds
    
    public AnimationContext(AnimationLog log) {
        this.log = log;
        this.startTimestamp = log.getStartDate().getMillis();
        this.endTimestamp = log.getEndDate().getMillis();
        if (totalDuration > 0) {
            this.timelineRatio = (endTimestamp - startTimestamp)/(totalDuration*1000);
        }
    }
    
    public boolean isValid() {
        return (totalDuration > 0 && endTimestamp > startTimestamp && frameRate > 0);
    }
    
    public AnimationLog getLog() {
        return this.log;
    }
    
    public int getFrameRate() {
        return this.frameRate;
    }
    
    public void setFrameRate(int fps) {
        if (fps > 0) {
            this.frameRate = fps;
            this.frameInterval = (int)(1.0/fps*1000);    
        }
    }
    
    public int getFrameInterval() {
        return this.frameInterval;
    }
    
    public long getStartTimestamp() {
        return this.startTimestamp;
    }
    
    public long getEndTimestamp() {
        return this.endTimestamp;
    }
    
    public int getChunkSize() {
        return this.chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public int getTotalDuration() {
        return this.totalDuration;
    }
    
    public void setTotalDuration(int totalDuration) {
        if (totalDuration > 0) {
            this.totalDuration = totalDuration;
            this.timelineRatio = (endTimestamp - startTimestamp)/(totalDuration*1000);
        }
    }
    
    public double getTimelineRatio() {
        return this.timelineRatio;
    }
    
}
