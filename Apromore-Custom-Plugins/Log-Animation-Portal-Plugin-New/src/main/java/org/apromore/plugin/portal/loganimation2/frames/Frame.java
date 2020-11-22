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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.tuple.primitive.IntDoublePair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.roaringbitmap.RoaringBitmap;

/**
 * Within a frame, a replay element is also a token of this frame.
 * 
 * @author Bruce Nguyen
 *
 */
public class Frame {
    private int frameIndex;
    private AnimationIndex animationIndex;
    
    // A true value at index ith is a replay element on this frame, also called a token index
    private RoaringBitmap replayElementMap = new RoaringBitmap();
    
    // Map from a token index to the number of tokens in a cluster that it represents
    private MutableIntIntMap tokenCountMap = IntIntMaps.mutable.empty(); 
    
    public Frame(int frameIndex, AnimationIndex animationIndex) {
        this.frameIndex = frameIndex;
        this.animationIndex = animationIndex;
    }
    
    public int getIndex() {
        return this.frameIndex;
    }
    
    public void addToken(int tokenIndex) {
        replayElementMap.add(tokenIndex);
    }
    
    public void addTokens(int[] tokenIndexes) {
        replayElementMap.add(tokenIndexes);
    }
    
    public void removeToken(int tokenIndex) {
        replayElementMap.remove(tokenIndex);
        if (tokenCountMap.containsKey(tokenIndex)) tokenCountMap.remove(tokenIndex);
    }
    
    public int[] getElementIndexes() {
        return Arrays.stream(getTokenIndexes())
                .map(tokenIndex -> animationIndex.getElementIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getCaseIndexes() {
        return Arrays.stream(getTokenIndexes())
                .map(tokenIndex -> animationIndex.getTraceIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getTokenIndexes() {
        return replayElementMap.toArray();
    }
    
    public int[] getTokenIndexesByElement(int elementIndex) {
        return Arrays.stream(getTokenIndexes())
                .filter(tokenIndex -> animationIndex.getElementIndex(tokenIndex) == elementIndex)
                .toArray();
    }
    
    private double getTokenDistance(int tokenIndex) {
        int startFrameIndex = animationIndex.getStartFrameIndex(tokenIndex);
        int numberOfFrames = animationIndex.getEndFrameIndex(tokenIndex) - startFrameIndex;
        return (numberOfFrames == 0) ? 0 : (double)(frameIndex - startFrameIndex)/numberOfFrames;
    }
    
    private void clusterTokensOnElement(int elementIndex) {
        // Collect tokens and their distances
        MutableList<IntDoublePair> tokenDistances = Lists.mutable.empty();
        for (int token : getTokenIndexesByElement(elementIndex)) {
            tokenDistances.add(PrimitiveTuples.pair(token, getTokenDistance(token)));
        }
        tokenDistances.sortThisBy(pair -> pair.getTwo()); // sort by distance
        
        // Group tokens with close distances
        Set<MutableIntList> tokenGroups = new HashSet<>();
        MutableIntList tokenGroup = IntLists.mutable.empty();
        double tokenGroupTotalDist = 0;
        for (IntDoublePair tokenPair : tokenDistances) {
            double tokenDistance = tokenPair.getTwo();
            double diff = tokenGroup.isEmpty() ? 0 : Math.abs(tokenDistance - tokenGroupTotalDist/tokenGroup.size());
            if (diff <= 0.01) {
                tokenGroup.add(tokenPair.getOne());
                tokenGroupTotalDist += tokenDistance;
                if (tokenPair == tokenDistances.getLast()) tokenGroups.add(tokenGroup);
            }
            else {
                tokenGroups.add(tokenGroup);
                tokenGroup = IntLists.mutable.empty();
                tokenGroupTotalDist = 0;
            }
        }
        
        // Collect representative token for each group
        for (IntList group : tokenGroups) {
            if (group.size() > 1) {
                tokenCountMap.put(group.get(0), group.size());
                group.forEach(token -> {if (token != group.get(0)) removeToken(token);});
            }
        }
    }
    
    public void clusterTokens() {
        for (int elementIndex : getElementIndexes()) {
            clusterTokensOnElement(elementIndex);
        }
    }
    
    /**
     * A sample of frame JSON:
     * {
     *  index: 100,
     *  elements: [
     *      {elementIndex1: [{caseIndex1:[0.1]}, {caseIndex2:[0.2]}, {caseIndex3:[0.1]}]},
     *      {elementIndex2: [{caseIndex1:[0.2]}, {caseIndex2:[0.5]}]},
     *      {elementIndex3: [{caseIndex4:[0.1]}]}
     *  ]
     * }
     */
    public JSONObject getJSON() throws JSONException {
        JSONObject frameJSON = new JSONObject();
        frameJSON.put("index", frameIndex);
        JSONArray elementsJSON = new JSONArray();
        for (int elementIndex : getElementIndexes()) {
            JSONArray casesJSON = new JSONArray();
            for (int tokenIndex : getTokenIndexesByElement(elementIndex)) {
                casesJSON.put((new JSONObject()).put(animationIndex.getTraceIndex(tokenIndex)+"", getTokenJSON(tokenIndex)));
            }
            elementsJSON.put((new JSONObject()).put(elementIndex+"", casesJSON));
        }
        frameJSON.put("elements", elementsJSON);
        return frameJSON;
    }
    
    private JSONArray getTokenJSON(int tokenIndex) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(df.format(getTokenDistance(tokenIndex)));
        attJSON.put(tokenCountMap.containsKey(tokenIndex) ? tokenCountMap.get(tokenIndex) : 1);
        return attJSON;
    }
}
