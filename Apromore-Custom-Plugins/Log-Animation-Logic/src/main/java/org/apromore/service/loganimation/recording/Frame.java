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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.tuple.primitive.IntDoublePair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
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
    
    // Each element corresponds to a log
    // Each element structure: tokenIndex => Set of merged tokens;
    private List<MutableIntObjectMap<IntSet>> tokenMergingMap = new ArrayList<>();
    
    public Frame(int frameIndex, List<AnimationIndex> animationIndexes) {
        this.frameIndex = frameIndex;
        this.animationIndexes = animationIndexes;
        for (int i=0; i< animationIndexes.size(); i++) {
            replayElementMaps.add(new RoaringBitmap());
            tokenMergingMap.add(IntObjectMaps.mutable.empty());
        };
    }
    
    public int getIndex() {
        return this.frameIndex;
    }
    
    public int[] getLogIndexes() {
        return IntStream.range(0, animationIndexes.size()).toArray();
    }
    
    public void addToken(int logIndex, int tokenIndex) {
        replayElementMaps.get(logIndex).add(tokenIndex);
    }
    
    public void addTokens(int logIndex, int[] tokenIndexes) {
        replayElementMaps.get(logIndex).add(tokenIndexes);
    }
    
    public boolean contains(int logIndex, int token) {
        return replayElementMaps.get(logIndex).contains(token);
    }
    
    /*
    public void removeToken(int logIndex, int tokenIndex) {
        replayElementMaps.get(logIndex).remove(tokenIndex);
        if (tokenCountMaps.get(logIndex).containsKey(tokenIndex)) tokenCountMaps.get(logIndex).remove(tokenIndex);
    }
    */
    
    public int[] getElementIndexes(int logIndex) {
        return Arrays.stream(getTokens(logIndex))
                .map(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getCaseIndexes(int logIndex) {
        return Arrays.stream(getTokens(logIndex))
                .map(tokenIndex -> animationIndexes.get(logIndex).getTraceIndex(tokenIndex))
                .distinct().toArray();
    }
    
    public int[] getTokens(int logIndex) {
        return replayElementMaps.get(logIndex).toArray();
    }
    
    private IntSet getTokensByElement(int logIndex, int elementIndex) {
        return IntSets.immutable.of(getTokens(logIndex))
                .select(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex) == elementIndex);
    }
    
    public IntSet getCentralTokens(int logIndex) {
        if (logIndex < 0 || logIndex >= tokenMergingMap.size()) return IntSets.mutable.empty();
        return tokenMergingMap.get(logIndex).keySet();
    }
    
    /**
     * Get a set of merged tokens by a central token
     * @param logIndex: log index
     * @param tokenIndex: central token
     * @return: set of merged token doesn't contain the central token
     */
    public IntSet getMergedTokens(int logIndex, int tokenIndex) {;
        if (logIndex < 0 || logIndex >= tokenMergingMap.size()) return IntSets.immutable.empty();
        return tokenMergingMap.get(logIndex).containsKey(tokenIndex)
                    ? tokenMergingMap.get(logIndex).get(tokenIndex).toImmutable()
                    : IntSets.immutable.empty();
    }
    
    public IntSet getNonMergedTokensByElement(int logIndex, int elementIndex) {
        this.getTokensByElement(logIndex, elementIndex).reject(predicate)
    }
    
    public int getTokenCount(int logIndex, int tokenIndex) {
        return this.getMergedTokens(logIndex, tokenIndex).size() + 1;
    }
    
    /**
     * This is the percentage from the start of the element (0..1) based on frame indexes
     * This distance is suitable for the relative position of tokens on a modeling element
     */
    private double getFrameIndexRelativeTokenDistance(int logIndex, int tokenIndex) {
        int startFrameIndex = animationIndexes.get(logIndex).getStartFrameIndex(tokenIndex);
        int endFrameIndex = animationIndexes.get(logIndex).getEndFrameIndex(tokenIndex);
        int maxLength = endFrameIndex - startFrameIndex;
        return (maxLength == 0) ? 0 : (double)(frameIndex - startFrameIndex)/maxLength;
    }
    
    /**
     * This is the number of frames from the starting token to this token on the same element
     * This distance is suitable for calculating small gap between tokens. The accuracy is not affected
     * by small and large numbers, i.e. 0.001 gap vs. 1000 gap.
     */
    private double getFrameIndexAbsoluteTokenDistance(int logIndex, int tokenIndex) {
        return frameIndex - animationIndexes.get(logIndex).getStartFrameIndex(tokenIndex);
    }
    
    private void mergeTokensOnElement(int logIndex, int elementIndex, Frame precedingFrame) {
        IntSet preCentralTokens = precedingFrame.getCentralTokens(logIndex);
        for (int token : preCentralTokens.toArray()) {
            if (elementIndex != animationIndexes.get(logIndex).getElementIndex(token)) continue;
            
        }
    }
    
    /**
     * Cluster tokens on the same modelling element. Tokens on different modelling elements (node/arc)
     * cannot be clustered. The token count will be updated.
     * @param elementIndex
     */
    private void mergeTokensOnElement(int logIndex, int elementIndex) {
        // Collect tokens and their distances
        MutableList<IntDoublePair> tokenDistances = Lists.mutable.empty();
        for (int token : getTokens(logIndex, elementIndex)) {
            tokenDistances.add(PrimitiveTuples.pair(token, getFrameIndexAbsoluteTokenDistance(logIndex, token)));
        }
        tokenDistances.sortThisBy(pair -> pair.getTwo()); // sort by distance
        
        // Group tokens with close distances
        Set<MutableIntList> tokenGroups = new HashSet<>();
        MutableIntList tokenGroup = IntLists.mutable.empty();
        double tokenGroupDistance = 0;
        boolean newGroup = true;
        for (IntDoublePair token : tokenDistances) {
            double tokenDistance = token.getTwo();
            if (newGroup) {
                tokenGroupDistance = tokenDistance;
                newGroup = false;
            }
            if (Math.abs(tokenDistance - tokenGroupDistance) <= 100) { //tokens within 1 second apart can be merged
                tokenGroup.add(token.getOne());
                if (token == tokenDistances.getLast()) tokenGroups.add(tokenGroup);
            }
            else {
                tokenGroups.add(tokenGroup);
                newGroup = true;
                tokenGroup = IntLists.mutable.empty();
            }
        }
        
        // Collect representative token for each group: take the first one in a group.
        for (IntList group : tokenGroups) {
            if (group.size() > 1) {
                tokenMergingMap.get(logIndex).put(group.get(0), group.toSet());
                //group.forEach(token -> {if (token != group.get(0)) removeToken(logIndex, token);});
            }
        }
    }
    
    public void mergeTokens(int logIndex) {
        for (int elementIndex : getElementIndexes(logIndex)) {
            mergeTokensOnElement(logIndex, elementIndex);
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
                for (int tokenIndex : getCentralTokens(logIndex).toArray()) {
                    casesJSON.put((new JSONObject()).put(animationIndexes.get(logIndex).getTraceIndex(tokenIndex)+"",
                                                        getTokenJSON(logIndex, tokenIndex)));
                }
                elementsJSON.put((new JSONObject()).put(elementIndex+"", casesJSON));
            }
        }
        frameJSON.put("elements", elementsJSON);
        return frameJSON;
    }
    
    private JSONArray getTokenJSON(int logIndex, int tokenIndex) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(logIndex);
        attJSON.put(df.format(getFrameIndexRelativeTokenDistance(logIndex, tokenIndex)));
        attJSON.put(this.getTokenCount(logIndex, tokenIndex));
        return attJSON;
    }
}
