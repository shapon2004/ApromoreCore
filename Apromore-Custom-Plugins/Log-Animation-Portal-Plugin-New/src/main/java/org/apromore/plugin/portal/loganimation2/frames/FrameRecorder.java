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

import org.apromore.plugin.portal.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.replay.AnimationLog;

/**
 * Used to create a frame collection as source data for animation.
 * 
 * @author Bruce Nguyen
 *
 */
public class FrameRecorder {
	public static Frames record(AnimationLog log, AnimationIndex animationIndex, AnimationContext animationContext) {
		Frames frames = new Frames(animationContext, animationIndex);
		frames.parallelStream().forEach(frame -> {
		    int[] tokenIndexes = animationIndex.getReplayElementIndexesByFrame(frame.getIndex());
		    frame.addTokens(tokenIndexes);
		});
		
		long timer = System.currentTimeMillis();
		frames.parallelStream().forEach(frame -> frame.clusterTokens());
		System.out.println("Clustering tokens: " + (System.currentTimeMillis() - timer)/1000 + " seconds.");
		
		return frames;
	}

}
