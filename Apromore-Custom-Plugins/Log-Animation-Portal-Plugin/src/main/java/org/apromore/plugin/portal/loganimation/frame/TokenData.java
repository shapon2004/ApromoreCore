package org.apromore.plugin.portal.loganimation.frame;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TokenData{
    private String tokenId;
    private double distance;
    private double size = 1;
    private String colorCode = "";
    
    public TokenData(String tokenId, double distance) {
        this.tokenId = tokenId;
        this.distance = distance;
    }
    
    public String getId() {
        return tokenId;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public double getDistance() {
        return this.distance;
    }
    
    public double getSize() {
        return this.size;
    }
    
    public String getColorCode() {
        return this.colorCode;
    }
    
    public String getJSON() throws JSONException {
        JSONArray atts = new JSONArray();
        atts.put(distance);
        atts.put(size);
        atts.put(colorCode);
        JSONObject json = new JSONObject();
        json.put("'" + tokenId + "'", atts);
        return json.toString();
    }
    
}
