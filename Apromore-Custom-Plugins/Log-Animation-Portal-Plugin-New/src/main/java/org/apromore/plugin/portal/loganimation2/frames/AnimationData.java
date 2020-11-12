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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apromore.plugin.portal.loganimation2.AnimationContext;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.apromore.service.loganimation2.replay.ReplayTrace;
import org.apromore.service.loganimation2.replay.TraceNode;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.roaringbitmap.RoaringBitmap;

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
public class AnimationData {
    private AnimationContext animationContext;
    
    // Indexes of replay elements
    private MutableIntIntMap replayElementToElement = IntIntMaps.mutable.empty(); // replay element index => element index
    private MutableIntIntMap replayElementToTrace = IntIntMaps.mutable.empty(); // replay element index => trace index
    private MutableIntObjectMap<IntIntPair> replayElementToFrames = IntObjectMaps.mutable.empty(); //replayElement index => start/end Frame index
    
    // A cell at (i,j) is a token in a frame on an element, i is the frame index, j is the replayed element index
    // true: there is a token at (i,j), false: no token
    private RoaringBitmap[] frameElementMap;
    
    // Map from a token index to the number of tokens in a cluster that it represents 
    private MutableObjectIntMap<IntIntPair> frameTokenCountMap = ObjectIntMaps.mutable.empty(); 
    
    public AnimationData(AnimationLog log, AnimationContext animateContext) {
        this.animationContext = animateContext;
        
        frameElementMap = new RoaringBitmap[animateContext.getMaxNumberOfFrames()];
        for (int i=0; i<frameElementMap.length; i++) {
            frameElementMap[i] = new RoaringBitmap();
        } 
        
        int replayElementIndex = 0;
        for (ReplayTrace trace : log.getTracesWithOriginalOrder()) {
            for (SequenceFlow flow : trace.getSequenceFlows()) {
                long start = ((TraceNode)flow.getSourceRef()).getComplete().getMillis();
                long end = ((TraceNode)flow.getTargetRef()).getStart().getMillis();  
                int flowIndex = log.getElementIndexFromId(flow.getId());
                computeReplayElementData(replayElementIndex, flowIndex, log.getCaseIndexFromId(trace.getId()), start, end);
                replayElementIndex++;
            }   
            for (TraceNode node : trace.getNodes()) {
                long start = node.getStart().getMillis();
                long end = node.getComplete().getMillis();
                int nodeIndex = !node.isActivitySkipped() ? log.getElementIndexFromId(node.getId()) : log.getElementSkipIndexFromId(node.getId());
                computeReplayElementData(replayElementIndex, nodeIndex, log.getCaseIndexFromId(trace.getId()), start, end);
                replayElementIndex++;
            }
        }
        
//        System.out.println("Start clustering tokens");
//        
//        for (int frameIndex=0; frameIndex<frameElementMap.length; frameIndex++) {
//            clusterFrameTokens(frameIndex);
//        }
    }
    
    private double getDistance(int frameIndex, int startFrameIndex, int endFrameIndex) {
        return (endFrameIndex <= startFrameIndex) ? 0 : (double)(frameIndex - startFrameIndex)/(endFrameIndex - startFrameIndex);
    }
    
    private void computeReplayElementData(int replayElementIndex, int elementIndex, int traceIndex, 
                                            long elementStart, long elementEnd) {
        int startFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementStart);
        int endFrameIndex = animationContext.getFrameIndexFromLogTimestamp(elementEnd);
        replayElementToElement.put(replayElementIndex, elementIndex);
        replayElementToTrace.put(replayElementIndex, traceIndex);
        replayElementToFrames.put(replayElementIndex, PrimitiveTuples.pair(startFrameIndex, endFrameIndex));
        for (int frameIndex=startFrameIndex; frameIndex<=endFrameIndex; frameIndex++) {
            addToken(frameIndex, replayElementIndex, getDistance(frameIndex, startFrameIndex, endFrameIndex));
        }
    }
    
    ///////////////////// FRAME TOKEN METHODS /////////////////////////////////////
    
    private void addToken(int frameIndex, int replayElementIndex, double distance) {
        if (!isValidFrameIndex(frameIndex) || !isValidReplayElementIndex(replayElementIndex)) return;
//        for (IntIntPair token : getFrameTokens(frameIndex)) {
//            if (Math.abs(getTokenDistance(token) - distance) <= 0.01) {
//                if (!frameTokenCountMap.containsKey(token)) frameTokenCountMap.put(token, 1);
//                frameTokenCountMap.put(token, frameTokenCountMap.get(token) + 1);
//                return;
//            }
//        }
        frameElementMap[frameIndex].add(replayElementIndex);
    }
    
    private void removeToken(int frameIndex, int replayElementIndex) {
        if (!isValidFrameIndex(frameIndex) || !isValidReplayElementIndex(replayElementIndex)) return;
        frameElementMap[frameIndex].remove(replayElementIndex);
    }
    
    private double getTokenDistance(IntIntPair token) {
        if (!isValidFrameIndex(token.getOne()) || !isValidReplayElementIndex(token.getTwo())) return 0;
        int startFrameIndex = getStartFrameIndex(token.getTwo());
        int numberOfFrames = getEndFrameIndex(token.getTwo()) - startFrameIndex;
        return (numberOfFrames == 0) ? 0 : (double)(token.getOne() - startFrameIndex)/numberOfFrames;
    }
    
    
    ///////////////////////////// JSON METHODS //////////////////////////////////////////////
    
    public String getChunkJSON(int startFrameIndex, int chunkSize) throws JSONException {
        JSONArray json = new JSONArray();
        if (!isValidFrameIndex(startFrameIndex) || chunkSize <= 0) {
            return json.toString();
        }
        
        int step = animationContext.getFrameSkip() + 1;
        for (int i=0; i < chunkSize; i++) {
            int stepIndex = startFrameIndex + i*step;
            int frameIndex = stepIndex < getNumberOfFrames() ? stepIndex : getNumberOfFrames()-1;
            json.put(getFrameJSON(frameIndex));
            if (stepIndex >= getNumberOfFrames()-1) {
                break;
            }
        }
        return json.toString();
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
    private JSONObject getFrameJSON(int frameIndex) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("index", frameIndex);
        JSONArray elements = new JSONArray();
        for (int elementIndex : getFrameElementIndexes(frameIndex)) {
            JSONArray cases = new JSONArray();
            for (int reIndex : this.getFrameReplayElementsByElement(frameIndex, elementIndex)) {
                cases.put((new JSONObject()).put(getTraceIndex(reIndex)+"", getFrameTokenJSON(frameIndex, reIndex)));
            }
            elements.put((new JSONObject()).put(elementIndex+"", cases));
        }
        json.put("elements", elements);
        return json;
    }
    
    private JSONArray getFrameTokenJSON(IntIntPair token) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(df.format(getTokenDistance(token)));
        attJSON.put(frameTokenCountMap.containsKey(token) ? frameTokenCountMap.get(token) : 1);
        return attJSON;
    }
    
    private JSONArray getFrameTokenJSON(int frameIndex, int reIndex) throws JSONException {
        return getFrameTokenJSON(PrimitiveTuples.pair(frameIndex, reIndex));
    }
 
    /////////////////////  REPLAY ELEMENTS METHODS /////////////////////////////////////////
    
    private boolean isValidReplayElementIndex(int reIndex) {
        return (reIndex >= 0 && reIndex < getNumberOfReplayedElements());
    }
    
    private int getNumberOfReplayedElements() {
        return replayElementToTrace.keySet().size();
    }
    
    private int getElementIndex(int replayElementIndex) {
        return replayElementToElement.get(replayElementIndex);
    }
    
    private int getTraceIndex(int replayElementIndex) {
        return replayElementToTrace.get(replayElementIndex);
    }
    
    private int getStartFrameIndex(int replayElementIndex) {
        return replayElementToFrames.containsKey(replayElementIndex) ? 
                replayElementToFrames.get(replayElementIndex).getOne() : -1;
    }
    
    private int getEndFrameIndex(int replayElementIndex) {
        return replayElementToFrames.containsKey(replayElementIndex) ? 
                replayElementToFrames.get(replayElementIndex).getTwo() : -1;
    }
    
    /////////////////////  FRAME METHODS /////////////////////////////////////////
    
    public int[] getFrameIndexes() {
        return IntStream.range(0, getNumberOfFrames()).toArray();
    }
    
    public int[] getFrameElementIndexes(int frameIndex) {
        return Arrays.stream(getFrameReplayElementIndexes(frameIndex))
                .map(reIndex -> getElementIndex(reIndex))
                .distinct().toArray();
    }
    
    public int[] getFrameReplayElementsByElement(int frameIndex, int elementIndex) {
        return Arrays.stream(getFrameReplayElementIndexes(frameIndex))
                .filter(reIndex -> getElementIndex(reIndex) == elementIndex)
                .toArray();
    }
    
    public int[] getFrameCases(int frameIndex) {
        return Arrays.stream(getFrameReplayElementIndexes(frameIndex))
                .map(reIndex -> getTraceIndex(reIndex))
                .distinct().toArray();
    }

    public int[] getFrameCasesByElement(int frameIndex, int elementIndex) {
        return Arrays.stream(getFrameReplayElementsByElement(frameIndex, elementIndex))
                .map(reIndex -> getTraceIndex(reIndex))
                .distinct().toArray();
    }
    
    public int[] getFrameReplayElementIndexes(int frameIndex) {
        return isValidFrameIndex(frameIndex) ? frameElementMap[frameIndex].toArray() : new int[] {};
    }
    
    public Set<IntIntPair> getFrameTokens(int frameIndex) {
        return Arrays.stream(getFrameReplayElementIndexes(frameIndex))
                .mapToObj(reIndex -> PrimitiveTuples.pair(frameIndex, reIndex))
                .collect(Collectors.toSet());
    }
    
    private int getNumberOfFrames() {
        return frameElementMap.length;
    }
    
    private boolean isValidFrameIndex(int frameIndex) {
        return (frameIndex >= 0 && frameIndex < getNumberOfFrames());
    }
    
    private void clusterFrameTokens(int frameIndex) {
        // Collect tokens and their distances
        MutableList<ObjectDoublePair<IntIntPair>> tokenDistances = Lists.mutable.empty();
        for (IntIntPair token : getFrameTokens(frameIndex)) {
            tokenDistances.add(PrimitiveTuples.pair(token, getTokenDistance(token)));
        }
        tokenDistances.sortThisBy(pair -> pair.getTwo()); // sort by distance
        
        // Group tokens with close distances
        Set<MutableList<IntIntPair>> tokenGroups = new HashSet<>();
        MutableList<IntIntPair> tokenGroup = Lists.mutable.empty();
        double tokenGroupTotalDist = 0;
        for (ObjectDoublePair<IntIntPair> tokenPair : tokenDistances) {
            double tokenDistance = tokenPair.getTwo();
            double diff = tokenGroup.isEmpty() ? 0 : Math.abs(tokenDistance - tokenGroupTotalDist/tokenGroup.size());
            if (diff <= 0.01) {
                tokenGroup.add(tokenPair.getOne());
                tokenGroupTotalDist += tokenDistance;
                if (tokenPair == tokenDistances.getLast()) tokenGroups.add(tokenGroup);
            }
            else {
                tokenGroups.add(tokenGroup);
                tokenGroup = Lists.mutable.empty();
                tokenGroupTotalDist = 0;
            }
        }
        
        // Collect representative token for each group
        for (List<IntIntPair> group : tokenGroups) {
            if (group.size() > 1) {
                frameTokenCountMap.put(group.get(0), group.size());
                group.forEach(token -> {
                   if (token != group.get(0)) removeToken(token.getOne(), token.getTwo());
                });
            }
        }

    }
    
}
