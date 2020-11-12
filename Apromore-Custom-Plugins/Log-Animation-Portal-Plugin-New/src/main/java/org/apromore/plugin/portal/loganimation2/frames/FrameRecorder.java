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

/**
 * Used to create a frame collection as source data for animation.
 * 
 * @author Bruce Nguyen
 *
 */
//public class FrameRecorder {
//	public static Frames record(AnimationLog log, AnimationContext animateContext) throws InvalidFrameParamsException {
//		Frames frames = new Frames(animateContext);
//		for (int frameIndex=0; frameIndex<animateContext.getMaxNumberOfFrames(); frameIndex++) {
//			frames.add(new Frame2(frameIndex, log.getNumberOfElements(), log.getNumberOfCases()));
//        }
//		
//		for (ReplayTrace trace : log.getTracesWithOriginalOrder()) {
//		    int caseIndex = log.getCaseIndexFromId(trace.getId());
//            for (SequenceFlow flow : trace.getSequenceFlows()) {
//                int flowIndex = log.getElementIndexFromId(flow.getId());
//                long start = ((TraceNode)flow.getSourceRef()).getComplete().getMillis();
//                long end = ((TraceNode)flow.getTargetRef()).getStart().getMillis();                
//                addTokensToFrames(frames, caseIndex, flowIndex, start, end, animateContext);
//            }   
//            for (TraceNode node : trace.getNodes()) {
//                int nodeIndex = !node.isActivitySkipped() ? log.getElementIndexFromId(node.getId()) : log.getElementSkipIndexFromId(node.getId());
//                long start = node.getStart().getMillis();
//                long end = node.getComplete().getMillis();
//                addTokensToFrames(frames, caseIndex, nodeIndex, start, end, animateContext);
//            }
//        }
//		
//		return frames;
//	}
//	
//	private static void addTokensToFrames(Frames frames, int caseIndex, int elementIndex, long elementStart, long elementEnd, 
//	                                        AnimationContext animateContext) {
//	    int startFrameIndex = animateContext.getFrameIndexFromLogTimestamp(elementStart);
//        int endFrameIndex = animateContext.getFrameIndexFromLogTimestamp(elementEnd); 
//        int numberOfFrames = endFrameIndex - startFrameIndex + 1;
//        double frameIntervalPercent = (numberOfFrames > 2) ? 1.0/(numberOfFrames-1) : 1.0; // the first and last frames are at 0 and 1 position.
//        double distance = 0;
//        for (int i=startFrameIndex; i<=endFrameIndex; i++) {
//            frames.get(i).addToken(elementIndex, caseIndex, distance);
//            distance += frameIntervalPercent;
//        }
//	}
//
//
//}
