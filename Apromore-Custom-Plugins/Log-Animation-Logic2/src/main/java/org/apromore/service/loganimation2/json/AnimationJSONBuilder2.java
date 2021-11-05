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

import org.apromore.service.loganimation2.enablement.CompositeAttributeLogEnablement;
import org.apromore.service.loganimation2.AnimationParams;
import org.apromore.service.loganimation2.enablement.AttributeLogEnablement;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;

/**
 * Generate the setup data for animation
 */
public class AnimationJSONBuilder2 {

    public static JSONObject parseLogCollection(CompositeAttributeLogEnablement animationData, AnimationParams params) throws JSONException {
        JSONObject collectionObj = new JSONObject();
        
        JSONArray logs = new JSONArray();
        for(AttributeLogEnablement log : animationData.getEnablements()) {
            logs.put(parseLog(log, params));
        }
        collectionObj.put("logs", logs);

        collectionObj.put("timeline", parseTimeline(
                new Interval(animationData.getStartTimestamp(), animationData.getEndTimestamp()),
                params));

        return collectionObj;
    }
    
    private static JSONObject parseLog(AttributeLogEnablement animationLog, AnimationParams params) throws JSONException {
        JSONObject json = new JSONObject();
        
        json.put("name", animationLog.getName());
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
    
    private static JSONObject parseTimeline(Interval totalRealInterval, AnimationParams params) throws JSONException {
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
