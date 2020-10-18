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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apromore.service.loganimation2.replay.AnimationLog;
import org.apromore.service.loganimation2.replay.ReplayTrace;
import org.apromore.service.loganimation2.replay.TraceNode;

import de.hpi.bpmn2_0.model.connector.SequenceFlow;

/**
 * Used to create a frame collection as source data for animation.
 * 
 * @author Bruce Nguyen
 *
 */
public class FrameRecorder {
	public static Frames record(AnimationLog log, AnimationContext animateContext) throws InvalidFrameParamsException {
		Map<ReplayTrace,Collection<SequenceFlow>> traceToSequenceFlows = new HashMap<>();
		Map<ReplayTrace,Collection<TraceNode>> traceToNodes = new HashMap<>();
		List<ReplayTrace> replayTraces = log.getTracesWithOriginalOrder();
		Frames frames = new Frames(animateContext);
		
		for (int frameIndex=0; frameIndex<animateContext.getMaxNumberOfFrames(); frameIndex++) {
			Frame frame = new Frame(frameIndex, log.getNumberOfElements(), log.getNumberOfCases());
			long frameTimestamp = frame.getTimestamp(animateContext);
			for (ReplayTrace trace : replayTraces) {
	            if (trace.getStartDate().getMillis()<=frameTimestamp && frameTimestamp<=trace.getEndDate().getMillis()) {
	            	if (!traceToSequenceFlows.containsKey(trace)) {
	            	    traceToSequenceFlows.put(trace, trace.getSequenceFlows());
	            	}
			        
	            	for (SequenceFlow seq : traceToSequenceFlows.get(trace)) {
			            long seqStart = ((TraceNode)seq.getSourceRef()).getComplete().getMillis();
			            long seqEnd = ((TraceNode)seq.getTargetRef()).getStart().getMillis();
			            long seqDuration = seqEnd - seqStart; // only animate if duration > 0
			            if (seqDuration > 0 && seqStart <= frameTimestamp && frameTimestamp <= seqEnd) {
			            	double distance = 1.0*(frameTimestamp - seqStart)/seqDuration;
			                frame.addToken(log.getElementIndexFromId(seq.getId()), 
			                				log.getCaseIndexFromId(trace.getId()),
			                				distance);
			            }
			        }	
			        
	            	if (!traceToNodes.containsKey(trace)) {
	            	    traceToNodes.put(trace, trace.getNodes());
                    }
	            	
	            	for (TraceNode node : traceToNodes.get(trace)) {
                        long start = node.getStart().getMillis();
                        long end = node.getComplete().getMillis();
                        long duration = end - start; // only animate if duration > 0
                        if (duration > 0 && start <= frameTimestamp && frameTimestamp <= end) {
                            double distance = 1.0*(frameTimestamp - start)/duration;
                            int elementIndex = !node.isActivitySkipped() ? log.getElementIndexFromId(node.getId()) :
                                                log.getElementSkipIndexFromId(node.getId());
                            frame.addToken(elementIndex, log.getCaseIndexFromId(trace.getId()), distance);
                        }
                    }   
	            }
			}
			frames.add(frame); //add all frames, including empty ones.
        }
		return frames;
	}

}
