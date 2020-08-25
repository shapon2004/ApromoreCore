package org.apromore.plugin.portal.loganimation.frame;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

public class FrameChunk extends ArrayList<Frame> {
    public String getJSON() throws JSONException {
        JSONArray result = new JSONArray();
        for (Frame frame : this) {
            result.put(frame.getJSON());
        }
        return result.toString();
    }
}
