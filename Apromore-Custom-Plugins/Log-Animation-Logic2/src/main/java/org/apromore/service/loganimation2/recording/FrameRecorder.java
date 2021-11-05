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

import java.util.List;

import org.apromore.service.loganimation2.AnimationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FrameRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameRecorder.class.getCanonicalName());
	public static Movie record(List<AnimationIndex> animationIndexes, AnimationContext animationContext) {
		Movie movie = new Movie(animationContext, animationIndexes);
		movie.parallelStream().forEach(frame -> {
		    for (int logIndex=0; logIndex < animationIndexes.size(); logIndex++) {
		        int[] tokenIndexes = animationIndexes.get(logIndex).getReplayElementIndexesByFrame(frame.getIndex());
	            frame.addTokens(logIndex, tokenIndexes);
		    }
		});
		
		long timer = System.currentTimeMillis();
		movie.parallelStream().forEach(frame -> {
		    for (int logIndex=0; logIndex < animationIndexes.size(); logIndex++) {
		        frame.clusterTokens(logIndex);
		    }
		});
		LOGGER.debug("Clustering tokens: " + (System.currentTimeMillis() - timer)/1000 + " seconds.");
		
		return movie;
	}

}
