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

package org.apromore.service.loganimation2.json;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apromore.service.loganimation2.enablement.EnablementLog;
import org.apromore.service.loganimation2.enablement.AnimationParams;
import org.apromore.service.loganimation2.utils.TimeUtilities;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Generate the setup data for animation
 */
public class AnimationJSONBuilder2 {
    private List<EnablementLog> animations = null;
    private Interval totalRealInterval = null; //total time interval of all logs
    private AnimationParams params;
    
    public AnimationJSONBuilder2(List<EnablementLog> animations, AnimationParams params) {
        this.animations = animations;
        this.params = params;
        
        Set<DateTime> dateSet = new HashSet<>();
        for (EnablementLog animationLog : animations) {
            dateSet.add(new DateTime(animationLog.getStartTimestamp()));
            dateSet.add(new DateTime(animationLog.getEndTimestamp()));
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
        for(EnablementLog log : this.animations) {
            logs.put(this.parseLog(log));
        }
        
        collectionObj.put("logs", logs);
        collectionObj.put("timeline", this.parseTimeline(animations));
        return collectionObj;
    }
    
    public JSONObject parseLog(EnablementLog animationLog) throws JSONException {
        DecimalFormat df = new DecimalFormat("#.###");
        JSONObject json = new JSONObject();
        
        json.put("name", animationLog.getName());
        json.put("filename", animationLog.getFileName());
        json.put("color", "");
        json.put("total", animationLog.size());
        json.put("play", animationLog.size());
        json.put("startDateLabel", new DateTime(animationLog.getStartTimestamp()).toString());
        json.put("endDateLabel", new DateTime(animationLog.getEndTimestamp()).toString());
        json.put("startLogDateLabel", new DateTime(animationLog.getStartTimestamp()).plus(((long)params.getStartEventToFirstEventDuration())*1000));
        json.put("endLogDateLabel", new DateTime(animationLog.getEndTimestamp()).minus(((long)params.getLastEventToEndEventDuration())*1000));
        json.put("reliable", "0");
        json.put("unreliableTraces", "0");
        json.put("exactTraceFitness", "0.0");
        
        return json;
    }
    
    protected JSONObject parseTimeline(Collection<EnablementLog> animations) throws JSONException {
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
