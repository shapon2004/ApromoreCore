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
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntDoublePair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.tuple.Tuples;
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
    private final int CLUSTERING_GAP = 100;
    
    private int frameIndex;
    
    // One index for each log
    private List<AnimationIndex> animationIndexes;
    
    // One bitmap for each log. Each bitmap: a true value at index ith is a token index on this frame (a token = a replay element)
    private List<RoaringBitmap> tokenBitmap = new ArrayList<>();

    // Contains token clusters after merging tokens
    private TokenClustering tokenClustering;
    
    public Frame(int frameIndex, List<AnimationIndex> animationIndexes) {
        this.frameIndex = frameIndex;
        this.animationIndexes = animationIndexes;
        for (int i=0; i< animationIndexes.size(); i++) {
            tokenBitmap.add(new RoaringBitmap());
        }
        tokenClustering = TokenClustering.of(animationIndexes.size());
    }
    
    public int getIndex() {
        return this.frameIndex;
    }
    
    private boolean isValidLog(int logIndex) {
        return logIndex >= 0 & logIndex < animationIndexes.size();
    }
    
    private boolean isValidBitmapIndex(int logIndex) {
        return logIndex >= 0 & logIndex < tokenBitmap.size();
    }
    
    public int[] getLogIndexes() {
        return IntStream.range(0, animationIndexes.size()).toArray();
    }
    
    public void addToken(int logIndex, int token) {
        if (!isValidBitmapIndex(logIndex)) return;
        tokenBitmap.get(logIndex).add(token);
    }
    
    public void addTokens(int logIndex, int[] tokens) {
        if (!isValidBitmapIndex(logIndex)) return;
        tokenBitmap.get(logIndex).add(tokens);
    }
    
    public boolean contains(int logIndex, int token) {
        return isValidBitmapIndex(logIndex) && !tokenBitmap.isEmpty() && tokenBitmap.get(logIndex).contains(token);
    }
    
    public IntSet getElementIndexes(int logIndex) {
        return getTokens(logIndex)
                .collectInt(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex), IntSets.mutable.empty());
    }
    
    public IntSet getCaseIndexes(int logIndex) {
        return getTokens(logIndex)
              .collectInt(tokenIndex -> animationIndexes.get(logIndex).getTraceIndex(tokenIndex), IntSets.mutable.empty());
    }
    
    public IntSet getTokens(int logIndex) {
        if (!isValidLog(logIndex)) return IntSets.immutable.empty();
        return IntSets.immutable.of(tokenBitmap.get(logIndex).toArray());
    }
    
    public IntSet getTokensByElement(int logIndex, int elementIndex) {
        return getTokens(logIndex).select(tokenIndex -> animationIndexes.get(logIndex).getElementIndex(tokenIndex) == elementIndex);
    }
    
    /**
     * Get token clusters on a modelling element
     * @param logIndex: log index
     * @param elementIndex: element index
     * @return: set of merged token
     */
    public List<TokenCluster> getTokenClusters(int logIndex, int elementIndex) {
        return tokenClustering.getClusters(logIndex, elementIndex);
    }

    /**
     * This is the number of frames from the starting token to this token on the same element
     * This distance is suitable for calculating small gap between tokens. The accuracy is not affected
     * by small and large numbers, i.e. 0.001 gap vs. 1000 gap.
     */
    private double getAbsoluteTokenDistance(int logIndex, int token) {
        return frameIndex - animationIndexes.get(logIndex).getStartFrameIndex(token);
    }
    
    /**
     * Merge token continuing from a preceding frame
     * @param logIndex
     * @param elementIndex
     * @param precedingFrame
     */
    private void mergeTokensContinuous(int logIndex, int elementIndex, Frame precedingFrame) {
//        if (precedingFrame == this) {
//            mergeTokensIndependent(logIndex, elementIndex);
//            return;
//        }
//
//        tokenClustering.clear();
//        tokenClustering = precedingFrame.copyTokenClustering();
//        for (TokenCluster tokenCluster : tokenClustering.get) {
//            IntSet copyCluster = IntSets.mutable.ofAll(tokenCluster.select(token -> this.contains(logIndex, token)));
//        }
    }
    
    private int findClosestCluster(int logIndex, int elementIndex, double distance) {
        double minDiff = Double.MAX_VALUE;
        int foundIndex = 0;
        int i = 0;
        for (TokenCluster cluster : getTokenClusters(logIndex, elementIndex)) {
            double diff = Math.abs(cluster.getDistance() - distance);
            if (diff < minDiff) {
                minDiff = diff;
                foundIndex = i;
            }
            else {
                break;
            }
            i++;
        }
        return foundIndex;
    }
    
    /**
     * Cluster tokens on the same modelling element. Tokens on different modelling elements (node/arc)
     * cannot be clustered. The token count will be updated.
     * @param elementIndex
     */
    private void mergeTokensIndependent(int logIndex, int elementIndex) {
        // Collect tokens and their distances
        MutableList<IntDoublePair> tokenDistances = Lists.mutable.empty();
        for (int token : getTokensByElement(logIndex, elementIndex).toArray()) {
            tokenDistances.add(PrimitiveTuples.pair(token, getAbsoluteTokenDistance(logIndex, token)));
        }
        tokenDistances.sortThisBy(pair -> pair.getTwo()); // sort by distance
        
        // Group tokens with close distances
        List<Pair<IntSet, Double>> tokenGroups = new ArrayList<>();
        MutableIntSet tokenGroup = IntSets.mutable.empty();
        double tokenGroupTotalDist = 0;
        double tokenGroupDistance = 0;
        for (IntDoublePair tokenPair : tokenDistances) {
            double tokenDistance = tokenPair.getTwo();
            double diff = tokenGroup.isEmpty() ? 0 : Math.abs(tokenDistance - tokenGroupDistance);
            if (diff <= CLUSTERING_GAP) { //tokens within 1 second apart can be merged
                tokenGroup.add(tokenPair.getOne());
                tokenGroupTotalDist += tokenDistance;
                tokenGroupDistance = tokenGroupTotalDist/tokenGroup.size();
                if (tokenPair == tokenDistances.getLast()) tokenGroups.add(Tuples.pair(tokenGroup, tokenGroupDistance));
            }
            else {
                tokenGroups.add(Tuples.pair(tokenGroup, tokenGroupDistance));
                tokenGroup = IntSets.mutable.empty();
                tokenGroupTotalDist = 0;
            }
        }
        
        // TokenCluster is added in the increasing order of distance
        for (Pair<IntSet, Double> pair : tokenGroups) {
            tokenClustering.addCluster(logIndex, elementIndex, TokenCluster.of(pair.getOne(), pair.getTwo()));
        }
    }
    
    public void mergeTokens(int logIndex, Frame precedingFrame) {
        if (!isValidLog(logIndex)) return;
        Objects.nonNull(precedingFrame);
        
        if (precedingFrame == this) {
            getElementIndexes(logIndex).forEach(elementIndex -> mergeTokensIndependent(logIndex, elementIndex));
            return;
        }
        
        // Copy token clusters
        tokenClustering.clear();
        for (int elementIndex : getElementIndexes(logIndex).toArray()) {
            for (TokenCluster tokenCluster : precedingFrame.getTokenClusters(logIndex, elementIndex)) {
                double newDistance = tokenCluster.getDistance() + 1;
                IntIterable selectTokens = tokenCluster.getTokens().select(token -> this.contains(logIndex, token));
                if (!selectTokens.isEmpty()) {
                    TokenCluster copyCluster = TokenCluster.of(selectTokens.toSet(), newDistance);
                    tokenClustering.addCluster(logIndex, elementIndex, copyCluster);
                }
            }
        }
        
        // Add new tokens to the clusters
        IntSet preTokens = precedingFrame.getTokens(logIndex);
        IntIterable newTokens = getTokens(logIndex).select(token -> !preTokens.contains(token));
        for (int token : newTokens.toArray()) {
            double tokenDistance = getAbsoluteTokenDistance(logIndex, token);
            int elementIndex = animationIndexes.get(logIndex).getElementIndex(token);
            int pos = findClosestCluster(logIndex, elementIndex, tokenDistance);
            List<TokenCluster> clusters = getTokenClusters(logIndex, elementIndex);
            double gap = clusters.get(pos).getDistance() - tokenDistance;
            if (Math.abs(gap) <= CLUSTERING_GAP) {
                clusters.get(pos).addToken(token);
            }
            else {
                TokenCluster newCluster = TokenCluster.of(IntSets.mutable.of(token), tokenDistance);
                    tokenClustering.insertClusterAt(logIndex, elementIndex, pos, newCluster);
                
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
            for (int elementIndex : getElementIndexes(logIndex).toArray()) {
                JSONArray casesJSON = new JSONArray();
                for (TokenCluster cluster : getTokenClusters(logIndex, elementIndex)) {
                    casesJSON.put((new JSONObject()).put("0", getTokenJSON(logIndex, elementIndex, cluster)));
                }
                elementsJSON.put((new JSONObject()).put(elementIndex+"", casesJSON));
            }
        }
        frameJSON.put("elements", elementsJSON);
        return frameJSON;
    }
    
    private JSONArray getTokenJSON(int logIndex, int elementIndex, TokenCluster cluster) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(logIndex);
        attJSON.put(df.format(cluster.getDistance()));
        attJSON.put(cluster.size());
        return attJSON;
    }
    
    public void cleanUpTokens() {
        tokenBitmap.forEach(tokenBits -> tokenBits.clear());
        tokenBitmap.clear();
    }
    
//    private JSONArray getTokenJSON(int logIndex, int tokenIndex) throws JSONException {
//        JSONArray attJSON = new JSONArray();
//        DecimalFormat df = new DecimalFormat("#.###");
//        attJSON.put(logIndex);
//        attJSON.put(df.format(getRelativeTokenDistance(logIndex, tokenIndex)));
//        attJSON.put(this.getTokenCount(logIndex, tokenIndex));
//        return attJSON;
//    }
}
