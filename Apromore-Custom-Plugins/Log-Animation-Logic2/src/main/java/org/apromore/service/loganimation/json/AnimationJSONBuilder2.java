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

package org.apromore.service.loganimation.json;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apromore.service.loganimation.replay.AnimationLog;
import org.apromore.service.loganimation.replay.ReplayParams;
import org.apromore.service.loganimation.utils.TimeUtilities;
import org.apromore.service.loganimation.replay.TraceNode;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Seconds;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;

/**
 * Generate the setup data for animation
 */
public class AnimationJSONBuilder2 {
    private List<AnimationLog> animations = null;
    private Interval totalRealInterval = null; //total time interval of all logs
    private ReplayParams params;
    
    public AnimationJSONBuilder2(List<AnimationLog> animations, ReplayParams params) {
        this.animations = animations;
        this.params = params;
        
        Set<DateTime> dateSet = new HashSet<>();
        for (AnimationLog animationLog : animations) {
            dateSet.add(animationLog.getStartDate());
            dateSet.add(animationLog.getEndDate());
        }
        SortedSet<DateTime> sortedDates = TimeUtilities.sortDates(dateSet);
        totalRealInterval = new Interval(sortedDates.first(), sortedDates.last());
    }
    
    //Proportion between animation and data time
    protected double getTimeConversionRatio() {
        return 1.0*params.getTotalEngineSeconds()/(totalRealInterval.toDurationMillis()/1000);
    }
    
    public JSONObject parseLogCollection() throws JSONException {
        JSONObject collectionObj = new JSONObject();
        
        JSONArray logs = new JSONArray();
        for(AnimationLog log : this.animations) {
            logs.put(this.parseLog(log));
        }
        
        collectionObj.put("logs", logs);
        collectionObj.put("timeline", this.parseTimeline(animations));
        return collectionObj;
    }
    
    public JSONObject parseLog(AnimationLog animationLog) throws JSONException {
        DecimalFormat df = new DecimalFormat("#.###");
        JSONObject json = new JSONObject();
        
        json.put("name", animationLog.getName());
        json.put("filename", animationLog.getFileName());
        json.put("color", "");
        json.put("total", animationLog.getTraces().size());
        json.put("play", animationLog.getTraces().size());
        json.put("startDateLabel", animationLog.getStartDate().toString());
        json.put("endDateLabel", animationLog.getEndDate().toString());
        json.put("startLogDateLabel", animationLog.getStartDate().plus(((long)params.getStartEventToFirstEventDuration())*1000));
        json.put("endLogDateLabel", animationLog.getEndDate().minus(((long)params.getLastEventToEndEventDuration())*1000));
        json.put("reliable", animationLog.getReliableTraceCount());
        json.put("unreliableTraces", animationLog.getUnReliableTraceIDs());
        json.put("exactTraceFitness", "0.0");
        
        return json;
    }
    
    protected JSONObject parseTimeline(Collection<AnimationLog> animations) throws JSONException {
        JSONObject json = new JSONObject();
        
        json.put("startDateLabel", totalRealInterval.getStart().toString());
        json.put("endDateLabel", totalRealInterval.getEnd().toString());
        json.put("startLogDateLabel", totalRealInterval.getStart().plus(((long)params.getStartEventToFirstEventDuration())*1000));
        json.put("endLogDateLabel", totalRealInterval.getEnd().minus(((long)params.getLastEventToEndEventDuration())*1000));
        json.put("timelineSlots", params.getTimelineSlots());
        json.put("totalEngineSeconds", params.getTotalEngineSeconds());
        json.put("timezone", ZoneId.systemDefault().toString());

        return json;
    }
    
}
