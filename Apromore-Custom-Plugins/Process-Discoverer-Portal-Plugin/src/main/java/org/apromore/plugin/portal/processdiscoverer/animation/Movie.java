/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.plugin.portal.processdiscoverer.animation;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * <b>Movie</b> is a sequence of ({@link Frame} recorded at a certain frame rate. 
 * In other words, it is the animation movie to be played back.
 * 
 * @see FrameRecorder
 * 
 * @author Bruce Nguyen
 *
 */
public class Movie extends ArrayList<Frame> {
	private AnimationContext animateContext;
	
	public Movie(AnimationContext animateContext, List<AnimationIndex> animationIndexes) {
		this.animateContext = animateContext;
		for (int frameIndex=0; frameIndex<animateContext.getMaxNumberOfFrames(); frameIndex++) {
            add(new Frame(frameIndex, animationIndexes));
        }
	}
	
	public AnimationContext getAnimationContext() {
		return this.animateContext;
	}
	
	/**
	 * Generate JSON for a chunk of frames identified by a starting frame index (inclusive) and a chunk size (number of frames).
	 * It uses the frame skip parameter in the {@link AnimationContext} to skip frames and thus increases the playback speed.
	 * @param startFrameIndex
	 * @param chunkSize
	 * @return JSON
	 * @throws JSONException
	 */
	public JSONArray getChunkJSON(int startFrameIndex, int chunkSize) throws JSONException {
		JSONArray json = new JSONArray();
		if (startFrameIndex < 0 || startFrameIndex >= this.size() || chunkSize <= 0) {
			return json;
		}
		
		int step = animateContext.getFrameSkip() + 1;
		for (int i=0; i < chunkSize; i++) {
		    int frameIndex = startFrameIndex + i*step;
		    frameIndex = frameIndex < this.size() ? frameIndex : this.size()-1;
		    json.put(this.get(frameIndex).getJSON());
		    if (frameIndex >= this.size()-1) break;
		}
		return json;
	}
}
