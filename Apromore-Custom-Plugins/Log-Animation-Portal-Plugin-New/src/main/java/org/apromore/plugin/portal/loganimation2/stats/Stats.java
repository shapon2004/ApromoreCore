package org.apromore.plugin.portal.loganimation2.stats;

import org.apromore.plugin.portal.loganimation2.frames.Frame;
import org.apromore.plugin.portal.loganimation2.frames.Frames;
import org.json.JSONArray;


public class Stats {
    public static JSONArray computeCaseCountsJSON(Frames frames) {
        JSONArray json = new JSONArray();
        for (Frame frame : frames) {
            json.put(frame.getCaseIndexes().length);
        }
        return json;
    }
}
