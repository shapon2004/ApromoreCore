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

import java.util.Set;
import java.util.stream.IntStream;

import org.apromore.plugin.portal.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.apromore.service.loganimation2.replay.ReplayTrace;
import org.apromore.service.loganimation2.replay.TraceNode;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import com.lodborg.intervaltree.IntegerInterval;
import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.Interval.Bounded;
import com.lodborg.intervaltree.IntervalTree;

import de.hpi.bpmn2_0.model.connector.SequenceFlow;

/**
 * This class is the underlying data used for the animation.<br>
 * A number of key notions:<br>
 * <ol>
 *  <li>Element: the modeling elements on a model such as sequence flow and activities.  
 *  <li>ReplayElement: represent an Element where a Trace has been replayed over. Multiple ReplayElement objects
 *      can correspond to one Element if the element is replayed multiple times for a trace.  
 *  <li>Frame: a frame contains many tokens at a point in time, each token is on a ReplayElement. 
 *  <li>Frame Token: a token is identified by (i,j) where i is the frame index and j is the replay element index.
 * <ol>
 * @author Bruce Nguyen
 *
 */
public class AnimationIndex {
    private AnimationContext animationContext;
    
    // Indexes of replay elements
    private MutableIntIntMap replayElementToElement = IntIntMaps.mutable.empty(); // replay element index => element index
    private MutableIntIntMap replayElementToTrace = IntIntMaps.mutable.empty(); // replay element index => trace index
    private MutableIntObjectMap<IntIntPair> replayElementToFrames = IntObjectMaps.mutable.empty(); //replayElement index => start/end Frame index
    
    // As each replay element is a frame index interval, use interval tree
    // to query all replay elements that contain a frame index
    private IntervalTree<Integer> intervalTree = new IntervalTree<>();
    private MutableObjectIntMap<IntegerInterval> intervalToReplayElement = ObjectIntMaps.mutable.empty();
    
    public AnimationIndex(AnimationLog log, AnimationContext animateContext) {
        this.animationContext = animateContext;
        int replayElementIndex = 0;
        for (ReplayTrace trace : log.getTracesWithOriginalOrder()) {
            for (SequenceFlow flow : trace.getSequenceFlows()) {
                long start = ((TraceNode)flow.getSourceRef()).getComplete().getMillis();
                long end = ((TraceNode)flow.getTargetRef()).getStart().getMillis();  
                int flowIndex = log.getElementIndexFromId(flow.getId());
                index(replayElementIndex, flowIndex, log.getCaseIndexFromId(trace.getId()), start, end);
                replayElementIndex++;
            }   
            for (TraceNode node : trace.getNodes()) {
                long start = node.getStart().getMillis();
                long end = node.getComplete().getMillis();
                int nodeIndex = !node.isActivitySkipped() ? log.getElementIndexFromId(node.getId()) : log.getElementSkipIndexFromId(node.getId());
                index(replayElementIndex, nodeIndex, log.getCaseIndexFromId(trace.getId()), start, end);
                replayElementIndex++;
            }
        }
        
    }
    
    private void index(int replayElementIndex, int elementIndex, int traceIndex, long elementStart, long elementEnd) {
        int startFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementStart);
        int endFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementEnd);
        replayElementToElement.put(replayElementIndex, elementIndex);
        replayElementToTrace.put(replayElementIndex, traceIndex);
        replayElementToFrames.put(replayElementIndex, PrimitiveTuples.pair(startFrameIndex, endFrameIndex));
        createReplayElementInterval(replayElementIndex, startFrameIndex, endFrameIndex);
    }
    
    private void createReplayElementInterval(int replayElementIndex, int startFrameIndex, int endFrameIndex) {
        IntegerInterval interval = new IntegerInterval(startFrameIndex, endFrameIndex, Bounded.CLOSED);
        intervalTree.add(interval);
        intervalToReplayElement.put(interval, replayElementIndex);
    }
    
    public IntStream getReplayElementIndexes() {
        return IntStream.range(0, replayElementToElement.keySet().size());
    }
    
    public int getElementIndex(int replayElementIndex) {
        return replayElementToElement.get(replayElementIndex);
    }
    
    public int getTraceIndex(int replayElementIndex) {
        return replayElementToTrace.get(replayElementIndex);
    }
    
    public int getStartFrameIndex(int replayElementIndex) {
        return replayElementToFrames.containsKey(replayElementIndex) ? 
                replayElementToFrames.get(replayElementIndex).getOne() : -1;
    }
    
    public int getEndFrameIndex(int replayElementIndex) {
        return replayElementToFrames.containsKey(replayElementIndex) ? 
                replayElementToFrames.get(replayElementIndex).getTwo() : -1;
    }
    
    public int[] getReplayElementIndexesByFrame(int frameIndex) {
        Set<Interval<Integer>> result = intervalTree.query(frameIndex);
        return result.stream().mapToInt(interval -> intervalToReplayElement.get(interval)).toArray();
    }
    
}
