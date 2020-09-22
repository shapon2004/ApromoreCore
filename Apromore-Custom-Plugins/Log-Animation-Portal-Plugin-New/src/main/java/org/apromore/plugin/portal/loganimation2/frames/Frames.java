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
package org.apromore.plugin.portal.loganimation2.frames;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * AnimationFrames contains a list of ({@link Frame} recorded at a certain frame rate.
 * 
 * @author Bruce Nguyen
 *
 */
public class Frames extends ArrayList<Frame> {
	private AnimationContext animateContext;
	
	public Frames(AnimationContext animateContext) {
		this.animateContext = animateContext;
	}
	
	public AnimationContext getAnimationContext() {
		return this.animateContext;
	}
	
	public String getChunkJSON(int startFrameIndex, int chunkSize) throws JSONException {
		JSONArray json = new JSONArray();
		if (startFrameIndex < 0 || startFrameIndex >= this.size()) {
			return json.toString();
		}
		int endFrameIndex = (startFrameIndex+chunkSize) >= this.size() ? this.size()-1 : startFrameIndex+chunkSize;
		for (int i=startFrameIndex; i<endFrameIndex; i++) {
			json.put(this.get(i).getJSON());
		}
		return json.toString();
	}
	
	public String getJSON() throws JSONException {
		return getChunkJSON(0, this.size());
	}
}
