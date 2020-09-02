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
package org.apromore.plugin.portal.loganimation2.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * A series of frames
 * 
 * @author Bruce Nguyen
 *
 */
public class Chunk extends ArrayList<Frame> {
    public Chunk(long startFrameIndex, int chunkSize, AnimationContext context) {
        if (startFrameIndex >= 0) {
            for (int i=0;i<chunkSize;i++) {
                this.add(new Frame(startFrameIndex + i));
            }
        }
    }
    
    public long getStartTimestamp(AnimationContext context) {
        return !this.isEmpty() ? this.get(0).getTimestamp(context) : 0;
    }
    
    public long getEndTimestamp(AnimationContext context) {
        return !this.isEmpty() ? this.get(this.size()-1).getTimestamp(context) : 0;
    }
    
    public JSONArray getJSON() throws JSONException {
        JSONArray result = new JSONArray();
        for (Frame frame : this) {
            if (!frame.isEmpty()) result.put(frame.getJSON());
        }
        return result;
    }
}
