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

import org.eclipse.collections.api.map.primitive.MutableIntDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Frame2 {
    protected int index;
    protected int numberOfElements;
    protected int numberOfCases;
    private MutableIntObjectMap<MutableIntSet> tokens = IntObjectMaps.mutable.empty(); // element index => set of case indexes
    protected MutableIntDoubleMap tokenDistances = IntDoubleMaps.mutable.empty(); // token index => distance
    
    public Frame2(int index, int numberOfElements, int numberOfCases) throws InvalidFrameParamsException {
        if (index < 0 || numberOfElements <= 0 || numberOfCases <= 0) {
            throw new InvalidFrameParamsException();
        }
        this.index = index;
        this.numberOfElements = numberOfElements;
        this.numberOfCases = numberOfCases;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public int getNumberOfCases() {
        return this.numberOfCases;
    }
    
    public int getNumberOfElements() {
        return this.numberOfElements;
    }
    
    public void addToken(int elementIndex, int caseIndex, double distance) {
        if (!validElementIndex(elementIndex) || !validCaseIndex(caseIndex)) return;
        if (!containsElement(elementIndex)) tokens.put(elementIndex, IntSets.mutable.empty());
        tokens.get(elementIndex).add(caseIndex);
        tokenDistances.put(getTokenIndex(elementIndex, caseIndex), distance);
    }
    
    public boolean containsElement(int elementIndex) {
        return tokens.containsKey(elementIndex);
    }
    
    public int[] getElementIndexes() {
        return tokens.keySet().toArray();
    }
    
    public int[] getCaseIndexes() {
        MutableIntSet allCases = IntSets.mutable.empty();
        tokens.values().forEach(cases -> allCases.addAll(cases));
        return allCases.toArray();
    }
    
    public int[] getCasesByElementIndex(int elementIndex) {
        return containsElement(elementIndex) ? tokens.get(elementIndex).toArray() : new int[]{};
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
        JSONObject json = new JSONObject();
        json.put("index", index);
        JSONArray elements = new JSONArray();
        for (int elementIndex : this.getElementIndexes()) {
            JSONArray cases = new JSONArray();
            for (int caseIndex : this.getCasesByElementIndex(elementIndex)) {
                int tokenIndex = getTokenIndex(elementIndex, caseIndex);
                cases.put((new JSONObject()).put(caseIndex+"", getAttributesJSON(tokenIndex)));
            }
            elements.put((new JSONObject()).put(elementIndex+"", cases));
        }
        json.put("elements", elements);
        return json;
    }
    
    protected JSONArray getAttributesJSON(int tokenIndex) throws JSONException {
        JSONArray attJSON = new JSONArray();
        DecimalFormat df = new DecimalFormat("#.###");
        attJSON.put(df.format(tokenDistances.get(tokenIndex)));
        return attJSON;
    }   
    
    protected int getTokenIndex(int elementIndex, int caseIndex) {
        return elementIndex*numberOfCases + caseIndex;
    }
    
    public int[] getTokensByElementIndex(int elementIndex) {
        MutableIntSet tokenIndexes = IntSets.mutable.empty();
        if (containsElement(elementIndex)) tokens.get(elementIndex).forEach(caseIndex -> tokenIndexes.add(getTokenIndex(elementIndex, caseIndex)));
        return tokenIndexes.toArray();
    }
    
    public double getTokenDistance(int tokenIndex) {
        return tokenDistances.get(tokenIndex);
    }
    
    protected boolean validCaseIndex(int caseIndex) {
        return (caseIndex >=0 && caseIndex < numberOfCases);
    }
    
    protected boolean validElementIndex(int elementIndex) {
        return (elementIndex >=0 && elementIndex < numberOfElements);
    }
    
    protected int getElementIndex(int tokenIndex) {
        return tokenIndex/numberOfCases;
    }
    
    protected int getCaseIndex(int tokenIndex) {
        return tokenIndex%numberOfCases;
    }
}
