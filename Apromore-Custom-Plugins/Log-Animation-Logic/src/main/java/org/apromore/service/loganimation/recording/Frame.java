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
package org.apromore.service.loganimation.recording;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.roaringbitmap.RoaringBitmap;

/**
 * A <b>Frame</b> is an animation frame to be played back. It contains a set of <b>tokens</b>. Each token is
 * identified by a modelling element, replay trace (or case). So, it is possible to query all tokens, modelling elements
 * and replay traces from a frame.<br>
 * 
 * As the number of tokens in the animation can be very large, a compressed bitmap is used to store tokens.
 * 
 * @see FrameRecorder
 * 
 * @author Bruce Nguyen
 *
 */
public class Frame {
    private int frameIndex;
    
    // One index for each log
    private List<AnimationIndex> animationIndexes;
    
    // One bitmap for each log
    // Each bitmap: a true value at index ith is a replay element on this frame, also called a token index
    private List<RoaringBitmap> replayElementMaps = new ArrayList<>();
    
    // Contains token clusters
    private TokenClustering tokenClustering;
    
    public Frame(int frameIndex, List<AnimationIndex> animationIndexes) {
        this.frameIndex = frameIndex;
        this.animationIndexes = animationIndexes;
        
        MutableIntSet elementIndexes = IntSets.mutable.empty();
        animationIndexes.forEach(animationIndex -> {
            replayElementMaps.add(new RoaringBitmap());
            elementIndexes.addAll(animationIndex.getElementIndexes());
        });
        
        tokenClustering = new TokenClustering(animationIndexes.size(), elementIndexes, 500);
    }
    
    private boolean isValidLogIndex(int logIndex) {
        return logIndex >= 0 && logIndex < animationIndexes.size();
    }
    
    public int getIndex() {
        return this.frameIndex;
    }
    
    public int[] getLogIndexes() {
        return IntStream.range(0, animationIndexes.size()).toArray();
    }
    
    public void addTokens(int logIndex, int[] tokenIndexes) {
        if (!isValidLogIndex(logIndex)) return;
        replayElementMaps.get(logIndex).add(tokenIndexes);
    }
    
    public int[] getElementIndexes(int logIndex) {
        if (!isValidLogIndex(logIndex)) return new int[] {};
        return Arrays.stream(getTokenIndexes(logIndex))
                .map(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getCaseIndexes(int logIndex) {
        if (!isValidLogIndex(logIndex)) return new int[] {};
        return Arrays.stream(getTokenIndexes(logIndex))
                .map(tokenIndex -> animationIndexes.get(logIndex).getTraceIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getTokenIndexes(int logIndex) {
        if (!isValidLogIndex(logIndex)) return new int[] {};
        return replayElementMaps.get(logIndex).toArray();
    }
    
    public int[] getTokenIndexesByElement(int logIndex, int elementIndex) {
        if (!isValidLogIndex(logIndex)) return new int[] {};
        return Arrays.stream(getTokenIndexes(logIndex))
                .filter(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex) == elementIndex)
                .toArray();
    }
    
    /**
     * This is the percentage from the start of the element (0..1) based on frame indexes
     * This distance is suitable for the relative position of tokens on a modeling element
     */
    private double getRelativeTokenDistance(int logIndex, int tokenIndex) {
        int startFrameIndex = animationIndexes.get(logIndex).getStartFrameIndex(tokenIndex);
        int endFrameIndex = animationIndexes.get(logIndex).getEndFrameIndex(tokenIndex);
        int maxLength = endFrameIndex - startFrameIndex;
        return (maxLength == 0) ? 0 : (double)(frameIndex - startFrameIndex)/maxLength;
    }
    
    public void clusterTokens(int logIndex) {
        if (!isValidLogIndex(logIndex)) return;
        tokenClustering.clear();
        
        for (int elementIndex : getElementIndexes(logIndex)) {
            for (int token : getTokenIndexesByElement(logIndex, elementIndex)) {
                tokenClustering.incrementClusterSizeByTokenDistance(logIndex, elementIndex, getRelativeTokenDistance(logIndex, token));
            }
        }
    }
    
    /**
     * Get JSON representation of a frame.
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
        for (int logIndex: getLogIndexes()) {
            for (int elementIndex : getElementIndexes(logIndex)) {
                JSONArray casesJSON = new JSONArray();
                for (int cluster : tokenClustering.getClusters(logIndex, elementIndex)) {
                    if (tokenClustering.getClusterSize(logIndex, elementIndex, cluster) > 0) {
                        casesJSON.put((new JSONObject()).put("0", getTokenClusterJSON(logIndex, elementIndex, cluster)));
                    }
                }
                elementsJSON.put((new JSONObject()).put(String.valueOf(elementIndex), casesJSON));
            }
        }
        frameJSON.put("elements", elementsJSON);
        return frameJSON;
    }
    
    private JSONArray getTokenClusterJSON(int logIndex, int elementIndex, int cluster) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(logIndex);
        attJSON.put(df.format(tokenClustering.getClusterDistance(cluster)));
        attJSON.put(tokenClustering.getClusterSize(logIndex, elementIndex, cluster));
        return attJSON;
    }
}
