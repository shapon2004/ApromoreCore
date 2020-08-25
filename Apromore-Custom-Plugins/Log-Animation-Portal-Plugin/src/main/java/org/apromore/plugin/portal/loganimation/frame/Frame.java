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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.replay.ReplayTrace;
import de.hpi.bpmn2_0.replay.TraceNode;

public class Frame {
    private long index;
    private Map<String, ElementData> elements = new HashMap<>();
    
    public Frame(long index) {
        this.index = index;
    }
    
    public long getIndex() {
        return index;
    }
    
    public void scanTrace(ReplayTrace trace, AnimationSetting setting) {
        long frameTime = FrameConverter.getTimestampFromFrameIndex(index, setting.getStartTimestamp(), 
                                                                    setting.getFrameGap());
        for (SequenceFlow seq : trace.getSequenceFlows()) {
            long seqStart = ((TraceNode)seq.getSourceRef()).getComplete().getMillis();
            long seqEnd = ((TraceNode)seq.getTargetRef()).getStart().getMillis();
            long seqDuration = seqEnd - seqStart; // only animate if duration > 0
            if (frameTime <= seqEnd && frameTime >= seqStart && seqDuration > 0) {
                double distance = (frameTime - seqStart)/seqDuration;
                String elementId = seq.getId();
                if (!elements.containsKey(elementId)) {
                    elements.put(elementId, new ElementData(elementId));
                }
                elements.get(elementId).addToken(new TokenData(trace.getId(), distance));
            }
        }
        
        for (TraceNode node : trace.getNodes()) {
            long nodeStart = node.getStart().getMillis();
            long nodeEnd = node.getComplete().getMillis();
            long nodeDuration = nodeEnd - nodeStart; // only animate if duration > 0
            if (frameTime <= nodeEnd && frameTime >= nodeStart && nodeDuration > 0) {
                double distance = (frameTime - nodeStart)/nodeDuration;
                String elementId = node.getId();
                if (!elements.containsKey(elementId)) {
                    elements.put(elementId, new ElementData(elementId));
                }
                elements.get(elementId).addToken(new TokenData(trace.getId(), distance));
            }
        }
    }
    
    public String getJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("index", index);
        JSONArray eleArray = new JSONArray();
        for (ElementData ele : elements.values()) {
            eleArray.put(ele.getJSON());
        }
        json.put("elements", eleArray);
        return json.toString();
    }
}
