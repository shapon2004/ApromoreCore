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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.NonNull;
import org.apromore.service.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.enablement.CompositeAttributeLogEnablement;
import org.apromore.service.loganimation2.enablement.AttributeLogEnablement;
import org.apromore.service.loganimation2.enablement.EnablementTuple;
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

/**
 * An <b>AnimationIndex</b> is an index of an EnablementLog.<br>
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
    
    public AnimationIndex(@NonNull AttributeLogEnablement log, @NonNull CompositeAttributeLogEnablement animationData,
                          @NonNull AnimationContext animateContext) {
        if (!animationData.getEnablements().contains(log)) throw new IllegalArgumentException("Invalid log or animation data");
        this.animationContext = animateContext;
        int replayElementIndex = 0;
        for (String caseId : log.getCaseIDs()) {
            for (EnablementTuple tuple : log.getEnablementsByCaseId(caseId)) {
                int caseIndex = log.getCaseIndexFromId(caseId);
                if (caseIndex < 0) throw new IllegalArgumentException("Couldn't find case with id = " + caseId);
                int elementIndex = animationData.getElementIndex(tuple.getElementId(), tuple.isSkip());
                if (elementIndex < 0) throw new IllegalArgumentException("Couldn't find element with id = " + tuple.getElementId());
                index(replayElementIndex, elementIndex, caseIndex, tuple.getStartTimestamp(), tuple.getEndTimestamp());
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
