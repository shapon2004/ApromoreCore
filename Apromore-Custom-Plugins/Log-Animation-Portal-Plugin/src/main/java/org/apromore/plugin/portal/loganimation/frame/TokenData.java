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
