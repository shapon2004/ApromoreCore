package org.apromore.plugin.portal.loganimation.frame;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ElementData {
    private String elementId="";
    private Map<String, TokenData> tokens = new HashMap<>();
    
    public ElementData(String elementId) {
        this.elementId = elementId;
    }
    
    public String getId() {
        return elementId;
    }
    
    public void addToken(TokenData token) {
        tokens.put(token.getId(), token);
    }
    
    public Collection<String> getTokenIDs() {
        return tokens.keySet();
    }
    
    public Collection<TokenData> getTokens() {
        return tokens.values();
    }
    
    public String getJSON() throws JSONException {
        JSONArray jsonTokens = new JSONArray();
        for (TokenData token : tokens.values()) {
            jsonTokens.put(token.getJSON());
        }
        JSONObject json = new JSONObject();
        json.put("'" + elementId + "'", jsonTokens);
        return json.toString();
    }
}
