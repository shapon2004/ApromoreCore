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
import org.apromore.service.loganimation2.enablement.CompositeAttributeLogEnablement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <b>Movie</b> is a sequence of ({@link Frame} recorded at a certain frame rate.
 * It is created from {@link CompositeAttributeLogEnablement} which contains enablement data for multiple logs
 * after being aligned with a BPMN diagram. Enablement data is alignment data plus calculated enablement
 * timestamp for each model element. Each case has its own enablement.<br>
 * Visually, a log enablement can be seen as below where each line is a case enablement and each segment is
 * the start enablement and end of enablement timestamp for one model element.
 * #1: |---------------|--------|--------------------------|
 * #2:      |-------|----------------|---------------------------|-----------|
 * #3:  |------------------|--------------------|----------------------|
 *
 * Each frame is a snapshot (a cut) of the log enablement at a point in time. Visually, it is a vertical cut
 * across the visualization above. Each cut results in a frame which contain some model elements.
 * Depending on the resolution of the animation, each cut can be done every few milliseconds in time. Consequently,
 * a <b>Movie</b> contains a large collection of frames to be played back on the browser. Each frame is given
 * a frame index, e.g. from 1 to 36,000.<br>
 *
 * Each Frame contains a number of model elements. Each occurrence of one model element in a frame is called
 * a <b>token</b>, visualized as a dot in the animation. Thus, each token is identified by: a modelling element, a case,
 * and a frame index.<br>
 *
 * Given a cut at a point in time and a log enablement, a question is what modelling elements will be cut and included in the frame.
 * It is inefficient to check all the cases and each model element in each case<br>
 *
 * <b>AnimationIndex</b> is an indexed data structure used to support this operation. An AnimationIndex is created by
 * scanning the animation log and creating indexes from each token to the modelling elements, cases and the frame.
 * Once an AnimationIndex has been created, given a cut in time, it is possible to query what model
 * elements (a segment above) are included in the cut.<br>
 *
 * As an AnimationIndex is a collection of intervals (i.e. segments), a binary interval tree is created to support
 * fast finding the cut intervals. Note that each interval above is marked by a starting and ending time. These points in time can be
 * converted (approximately) to a frame index. Thus, each interval is marked by starting and ending frame indexes. Each cut in time corresponds
 * to a frame index, thus, given a frame index, it is to search the interval tree to find the cut intervals containing that index. As each
 * cut interval corresponds to a set of modelling elements and traces, it it possible to identify information of all tokens in the frame.
 *
 * @author Bruce Nguyen
 *
 */
public class Movie extends ArrayList<Frame> {
	private final AnimationContext animateContext;
	private final JSONObject setupData;

	public Movie(CompositeAttributeLogEnablement enablement, AnimationParams animationParams) throws Exception {
		animateContext = new AnimationContext(enablement.getStartTimestamp(), enablement.getEndTimestamp());
		setupData = enablement.createSetupData(animationParams);
		generateFrameData(enablement);
	}

	private void generateFrameData(CompositeAttributeLogEnablement compositeEnablement) {
		// Create indexes
		List<AnimationIndex> animationIndexes = compositeEnablement.getEnablements()
				.stream()
				.map(e -> new AnimationIndex(e, compositeEnablement, animateContext))
				.collect(Collectors.toList());

		// Create frames
		IntStream.range(0, animateContext.getMaxNumberOfFrames()).forEach(frameIndex ->
				add(new Frame(frameIndex, animationIndexes))
		);

		// Add tokens
		this.parallelStream().forEach(frame -> {
			IntStream.range(0, animationIndexes.size()).forEach(logIndex ->
					frame.addTokens(logIndex,
							animationIndexes.get(logIndex).getReplayElementIndexesByFrame(frame.getIndex()))
			);
		});

		// Cluster tokens
		this.parallelStream().forEach(frame -> {
			IntStream.range(0, animationIndexes.size()).forEach(logIndex -> frame.clusterTokens(logIndex));
		});
	}
	
	public AnimationContext getAnimationContext() {
		return this.animateContext;
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
