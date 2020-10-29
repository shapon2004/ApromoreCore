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
package org.apromore.plugin.portal.loganimation2.frames;

import org.apromore.service.loganimation2.replay.AnimationLog;

/**
 * Animation global parameters
 * 
 * @author Bruce Nguyen
 *
 */
public class AnimationContext {
	private int recordingFrameRate = 60; //frames per second
    private int frameInterval =  (int)(1.0/recordingFrameRate*1000); //milliseconds between two consecutive frames
    private int chunkSize = 4920; //number of frames
    private int totalDuration = 120; //seconds
    private double timelineRatio = 1; //a second on the animation timeline is converted to actual seconds
    private long startTimestamp;
    private long endTimestamp;
    
    public AnimationContext(AnimationLog log) {
        this.startTimestamp = log.getStartDate().getMillis();
        this.endTimestamp = log.getEndDate().getMillis();
        if (totalDuration > 0) {
            this.timelineRatio = (endTimestamp - startTimestamp)/(totalDuration*1000);
        }
    }
    
    public boolean isValid() {
        return (totalDuration > 0 && endTimestamp > startTimestamp && recordingFrameRate > 0);
    }
    
    public int getRecordingFrameRate() {
        return this.recordingFrameRate;
    }
    
    public void setFrameRate(int fps) {
        if (fps > 0) {
            this.recordingFrameRate = fps;
            this.frameInterval = (int)(1.0/fps*1000);    
        }
    }
    
    public int getMaxNumberOfFrames() {
    	return this.recordingFrameRate*this.totalDuration;
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
    
    //Unit: seconds
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
