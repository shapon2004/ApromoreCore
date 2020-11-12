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
package org.apromore.plugin.portal.loganimation2.stats;

import org.apromore.plugin.portal.loganimation2.frames.Frame3;
import org.apromore.plugin.portal.loganimation2.frames.Frames;
import org.json.JSONArray;

public class Stats {
    public static JSONArray computeCaseCountsJSON(Frames frames) {
        JSONArray json = new JSONArray();
        for (Frame3 frame : frames) {
            json.put(frame.getCaseIndexes().length);
        }
        return json;
    }
//    public static JSONArray computeCaseCountsJSON(AnimationData data) {
//        JSONArray json = new JSONArray();
//        for (int frameIndex : data.getFrameIndexes()) {
//            json.put(data.getFrameCases(frameIndex).length);
//        }
//        return json;
//    }
}
