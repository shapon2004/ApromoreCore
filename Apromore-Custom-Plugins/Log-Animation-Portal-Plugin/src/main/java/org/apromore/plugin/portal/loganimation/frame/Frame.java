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
