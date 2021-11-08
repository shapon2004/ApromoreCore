/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
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
package org.apromore.service.loganimation2.recording;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apromore.service.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.AnimationParams;
import org.apromore.service.loganimation2.enablement.CompositeLogEnablement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <b>Movie</b> is a sequence of ({@link Frame} recorded at a certain frame rate.<br>
 * A movie is created from a {@link CompositeLogEnablement} which contains enablement data for multiple logs
 * after aligning them with a BPMN diagram. Enablement data is alignment data plus calculated enablement timestamp for
 * each model element. Each case has its own enablement.
 *
 * <p>
 * Visually, a log enablement can be seen as below where each line is a case enablement and each segment is
 * the starting and ending enablement timestamp for one modelling element (an activity or arc).
 * #1: |---------------|--------|--------------------------|
 * #2:      |-------|----------------|---------------------------|-----------|
 * #3:  |------------------|--------------------|----------------------|
 * </p>
 *
 * <p>
 * Each frame is a snapshot (a cut) of the log enablement at a point in time. Visually, it is a vertical cut
 * across the visualization above. Each cut results in a frame containing multiple cut points, each cut point belongs to
 * a case and a modelling element and is called a <b>token</b>. Depending on the rate of the animation, each cut
 * can be done every few milliseconds in time. Consequently, a <b>Movie</b> contains a sequence of frames. Each frame is
 * given an index, e.g. from 1 to 36,000.
 * </p>
 *
 * @author Bruce Nguyen
 *
 */
public class Movie extends ArrayList<Frame> {
	private final AnimationContext animationContext;
	private final JSONObject setupData;

	public Movie(CompositeLogEnablement enablement, AnimationParams animationParams) {
		animationContext = new AnimationContext(enablement.getStartTimestamp(), enablement.getEndTimestamp());
		setupData = enablement.createSetupData(animationParams);
		generateFrameData(enablement);
	}

	private void generateFrameData(CompositeLogEnablement compositeEnablement) {
		// Create indexes
		List<TokenMap> tokens = compositeEnablement.getEnablements()
				.stream()
				.map(e -> new TokenMap(e, compositeEnablement, animationContext))
				.collect(Collectors.toList());

		// Create frames
		IntStream.range(0, animationContext.getMaxNumberOfFrames()).forEach(frameIndex ->
				add(new Frame(frameIndex, tokens))
		);

		// Add tokens to frames
		this.parallelStream().forEach(frame ->
			IntStream.range(0, tokens.size()).forEach(logIndex ->
					frame.addTokens(logIndex, tokens.get(logIndex).getTokensByFrame(frame.getIndex()))));

		// Cluster tokens in frames
		this.parallelStream().forEach(frame ->
			IntStream.range(0, tokens.size()).forEach(logIndex -> frame.clusterTokens(logIndex)));
	}
	
	public AnimationContext getAnimationContext() {
		return this.animationContext;
	}

	public JSONObject getSetupData() {
		return this.setupData;
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
		
		int step = animationContext.getFrameSkip() + 1;
		for (int i=0; i < chunkSize; i++) {
		    int frameIndex = startFrameIndex + i*step;
		    frameIndex = frameIndex < this.size() ? frameIndex : this.size()-1;
		    json.put(this.get(frameIndex).getJSON());
		    if (frameIndex >= this.size()-1) break;
		}
		return json;
	}
}
