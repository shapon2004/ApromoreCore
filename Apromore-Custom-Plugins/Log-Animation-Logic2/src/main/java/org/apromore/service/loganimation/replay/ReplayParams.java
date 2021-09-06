/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2015 - 2017 Queensland University of Technology.
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

package org.apromore.service.loganimation.replay;

public class ReplayParams {
    private int TimelineSlots;
    private int TotalEngineSeconds;
    private int ProgressCircleBarRadius;
    private int StartEventToFirstEventDuration = 60; // default is 1 minute
    private int LastEventToEndEventDuration = 24*3600; // default is 1 day
    
    public int getStartEventToFirstEventDuration() {
        return this.StartEventToFirstEventDuration;
    }
    
    public void setStartEventToFirstEventDuration(int newDuration) {
        this.StartEventToFirstEventDuration = newDuration;
    }    
    
    public int getLastEventToEndEventDuration() {
        return this.LastEventToEndEventDuration;
    }
    
    public void setLastEventToEndEventDuration(int newDuration) {
        this.LastEventToEndEventDuration = newDuration;
    }

    public int getTotalEngineSeconds() {
        return TotalEngineSeconds;
    }

    public void setTotalEngineSeconds(int TotalEngineSeconds) {
        this.TotalEngineSeconds = TotalEngineSeconds;
    }

    public int getTimelineSlots() {
        return TimelineSlots;
    }

    public void setTimelineSlots(int TimelineSlots) {
        this.TimelineSlots = TimelineSlots;
    }

}
