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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apromore.service.loganimation.replay.AnimationLog;
import org.apromore.service.loganimation.replay.ReplayTrace;
import org.apromore.service.loganimation.replay.TraceNode;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import com.lodborg.intervaltree.IntegerInterval;
import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.Interval.Bounded;
import com.lodborg.intervaltree.IntervalTree;

import de.hpi.bpmn2_0.model.activity.Activity;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;

/**
 * An <b>AnimationIndex</b> is an index of an AnimationLog.<br>
 * Note that each replay element is given a unique sequential number (called replay element index, or token index)
 * The animation index is built for each replay element to point to its modelling elements, replay trace and frame indexes.
 * 
 * @see FrameRecorder
 * 
 * @author Bruce Nguyen
 *
 */
public class AnimationIndex {
    private AnimationContext animationContext;
    
    // Indexes of replay elements
    private MutableIntIntMap replayElementToElement = IntIntMaps.mutable.empty(); // replay element index => element index
    private MutableIntIntMap replayElementToTrace = IntIntMaps.mutable.empty(); // replay element index => trace index
    private MutableIntObjectMap<IntIntPair> replayElementToFrames = IntObjectMaps.mutable.empty(); //replayElement index => start/end Frame index
    
    // As each replay element is an interval of two frame indexes, interval tree is used
    // as an efficient data structure to query, e.g. retrieve all replay elements contain a frame index
    private IntervalTree<Integer> intervalTree = new IntervalTree<>();
    private Map<IntegerInterval, MutableIntSet> intervalToReplayElement = new HashMap<>();
    
    public AnimationIndex(AnimationLog log, ModelMapping modelMapping, AnimationContext animateContext)
            throws ElementNotFoundException, CaseNotFoundException {
        this.animationContext = animateContext;
        int replayElementIndex = 0;
        for (ReplayTrace trace : log.getTracesWithOriginalOrder()) {
            for (SequenceFlow flow : trace.getSequenceFlows()) {
                long start = ((TraceNode)flow.getSourceRef()).getComplete().getMillis();
                long end = ((TraceNode)flow.getTargetRef()).getStart().getMillis();  
                int flowIndex = modelMapping.getIndex(flow.getId());
                if (flowIndex < 0) throw new ElementNotFoundException("Couldn't find index for flow with id = " + flow.getId());
                int caseIndex = log.getCaseIndexFromId(trace.getId());
                if (caseIndex < 0) throw new CaseNotFoundException("Couldn't find case with id = " + trace.getId());
                index(replayElementIndex, flowIndex, caseIndex, start, end);
                replayElementIndex++;
            }   
            for (TraceNode node : trace.getNodes()) {
                if (!(node.getModelNode() instanceof Activity)) continue;
                long start = node.getStart().getMillis();
                long end = node.getComplete().getMillis();
                
                int nodeIndex = modelMapping.getIndex(node.getId());
                if (nodeIndex < 0) throw new ElementNotFoundException("Couldn't find index for node with id = " + node.getId());
                int nodeSkipIndex = modelMapping.getSkipIndex(node.getId());
                if (nodeSkipIndex < 0) throw new ElementNotFoundException("Couldn't find skipIndex for node with id = " + node.getId());
                int selectedNodeIndex = !node.isActivitySkipped() ? nodeIndex : nodeSkipIndex;
                
                int caseIndex = log.getCaseIndexFromId(trace.getId());
                if (caseIndex < 0) throw new CaseNotFoundException("Couldn't find case with id = " + trace.getId());
                index(replayElementIndex, selectedNodeIndex, caseIndex, start, end);
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
        
        // IntervalTree doesn't keep duplicate intervals (only keeps one).
        // intervalToReplayElement must store value as a set of replay element indexes. 
        if (!intervalToReplayElement.containsKey(interval)) intervalToReplayElement.put(interval, IntSets.mutable.empty()); 
        intervalToReplayElement.get(interval).add(replayElementIndex);
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
        return result.stream()
                .map(interval -> intervalToReplayElement.get(interval))
                .flatMap(set -> Arrays.stream(set.toArray()).boxed())
                .mapToInt(Integer::intValue)
                .toArray();
    }
    
}
