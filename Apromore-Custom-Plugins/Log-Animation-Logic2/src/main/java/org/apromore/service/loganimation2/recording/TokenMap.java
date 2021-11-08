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

import lombok.NonNull;
import org.apromore.service.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.enablement.CompositeLogEnablement;
import org.apromore.service.loganimation2.enablement.LogEnablement;
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
 * A <b>TokenMap</b> contains all animation tokens for an {@link LogEnablement}. Each token belongs to
 * a case and a modelling element. Each token lasts between starting and ending frame indexes. Each token is visualized
 * as a dot in an animation.
 *
 * @see Movie
 * 
 * @author Bruce Nguyen
 *
 */
public class TokenMap {
    private AnimationContext animationContext;

    private MutableIntIntMap tokenToElement = IntIntMaps.mutable.empty(); // token index => element index
    private MutableIntIntMap tokenToTrace = IntIntMaps.mutable.empty(); // token index => trace index
    private MutableIntObjectMap<IntIntPair> tokenToFrames = IntObjectMaps.mutable.empty(); //replayElement index => start/end Frame index
    
    // As each token is played within an interval of two frame indexes, an interval tree is used
    // as an efficient data structure to query, e.g. retrieve all tokens contained at a frame index
    private IntervalTree<Integer> intervalTree = new IntervalTree<>();
    private Map<IntegerInterval, MutableIntSet> intervalToTokens = new HashMap<>();
    
    public TokenMap(@NonNull LogEnablement logEnablement, @NonNull CompositeLogEnablement enablementData,
                    @NonNull AnimationContext animateContext) {
        if (!enablementData.getEnablements().contains(logEnablement)) throw new IllegalArgumentException("Invalid log or animation data");
        this.animationContext = animateContext;
        int tokenIndex = 0;
        for (String caseId : logEnablement.getCaseIDs()) {
            for (EnablementTuple tuple : logEnablement.getEnablementsByCaseId(caseId)) {
                int caseIndex = logEnablement.getCaseIndexFromId(caseId);
                if (caseIndex < 0) throw new IllegalArgumentException("Couldn't find case with id = " + caseId);
                int elementIndex = enablementData.getElementIndex(tuple.getElementId(), tuple.isSkip());
                if (elementIndex < 0) throw new IllegalArgumentException("Couldn't find element with id = " + tuple.getElementId());
                index(tokenIndex, elementIndex, caseIndex, tuple.getStartTimestamp(), tuple.getEndTimestamp());
                tokenIndex++;
            }
        }
        
    }
    
    private void index(int tokenIndex, int elementIndex, int traceIndex, long elementStart, long elementEnd) {
        int startFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementStart);
        int endFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementEnd);
        tokenToElement.put(tokenIndex, elementIndex);
        tokenToTrace.put(tokenIndex, traceIndex);
        tokenToFrames.put(tokenIndex, PrimitiveTuples.pair(startFrameIndex, endFrameIndex));
        createTokenInterval(tokenIndex, startFrameIndex, endFrameIndex);
    }
    
    private void createTokenInterval(int token, int startFrameIndex, int endFrameIndex) {
        IntegerInterval interval = new IntegerInterval(startFrameIndex, endFrameIndex, Bounded.CLOSED);
        intervalTree.add(interval);
        
        // IntervalTree doesn't keep duplicate intervals (only keeps one).
        // intervalToReplayElement must store value as a set of replay element indexes.
        if (!intervalToTokens.containsKey(interval)) intervalToTokens.put(interval, IntSets.mutable.empty());
        intervalToTokens.get(interval).add(token);
    }
    
    public int getElementIndex(int token) {
        return tokenToElement.get(token);
    }
    
    public int getTraceIndex(int token) {
        return tokenToTrace.get(token);
    }
    
    public int getStartFrameIndex(int token) {
        return tokenToFrames.containsKey(token) ? tokenToFrames.get(token).getOne() : -1;
    }
    
    public int getEndFrameIndex(int token) {
        return tokenToFrames.containsKey(token) ? tokenToFrames.get(token).getTwo() : -1;
    }
    
    public int[] getTokensByFrame(int frameIndex) {
        Set<Interval<Integer>> result = intervalTree.query(frameIndex);
        return result.stream()
                .map(interval -> intervalToTokens.get(interval))
                .flatMap(set -> Arrays.stream(set.toArray()).boxed())
                .mapToInt(Integer::intValue)
                .toArray();
    }
    
}
