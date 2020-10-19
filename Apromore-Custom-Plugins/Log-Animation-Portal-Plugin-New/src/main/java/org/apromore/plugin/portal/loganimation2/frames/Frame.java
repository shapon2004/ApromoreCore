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

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntDoubleMap;
import org.eclipse.collections.impl.factory.primitive.IntDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.roaringbitmap.RoaringBitmap;

/**
 * Each Frame is a matrix where the row index is the index of a path element  
 * and the column index is the index of a case. Path elements are SVG path element representing
 * a sequence flow, a path through the center of a node or a path through the edge of a node.
 * Each token is identified by a pair (elementIndex, caseIndex), so called token coordinate. 
 * As the number of cases can be extremely high, case indexes are stored in a compressed bitmap. 
 * From the token coordinate, it is possible to obtain a single index called token index. 
 * Token index = elementIndex*numberOfCases + caseIndex. 
 * The token index is used to point to token's attributes such as the distance of the dot from the element start.
 * 
 * @author Bruce Nguyen
 *
 */
public class Frame {
	protected int index;
	protected int numberOfElements;
	protected int numberOfCases;
	// Each array item represents a modelling element, the item index is the element index
	// Each bit in a bitmap represents a case, the bit index is the case index
	protected RoaringBitmap[] tokenBitmap;
	// Key = token's position index = elementIndex*numberOfTraces + caseIndex, 
	// Value = distance from the element start
	protected MutableIntDoubleMap distances = IntDoubleMaps.mutable.empty();
	
	public Frame(int index, int numberOfElements, int numberOfCases) throws InvalidFrameParamsException {
		if (index < 0 || numberOfElements <= 0 || numberOfCases <= 0) {
			throw new InvalidFrameParamsException();
		}
		this.numberOfElements = numberOfElements;
		this.numberOfCases = numberOfCases;
		tokenBitmap = new RoaringBitmap[numberOfElements];
		this.index = index;
		for (int i=0; i<numberOfElements; i++) {
			tokenBitmap[i] = new RoaringBitmap();
		}
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
		if (validElementIndex(elementIndex) && validCaseIndex(caseIndex)) {
			tokenBitmap[elementIndex].add(caseIndex);
			distances.put(getTokenIndex(elementIndex, caseIndex), distance);
		}
	}
	
	public void removeToken(int elementIndex, int caseIndex) {
		if (validElementIndex(elementIndex) && validCaseIndex(caseIndex)) {
			if (containsElement(elementIndex)) tokenBitmap[elementIndex].checkedRemove(caseIndex);
			distances.remove(getTokenIndex(elementIndex, caseIndex));
		}
	}
	
	protected boolean validElementIndex(int elementIndex) {
		return (elementIndex >=0 && elementIndex < numberOfElements);
	}
	
	protected boolean validCaseIndex(int caseIndex) {
		return (caseIndex >=0 && caseIndex < numberOfCases);
	}
	
	protected int getTokenIndex(int elementIndex, int caseIndex) {
		if (validElementIndex(elementIndex) && validCaseIndex(caseIndex)) {
			return elementIndex*numberOfCases + caseIndex;
		}
		else {
			return -1;
		}
	}
	
	protected int getElementIndex(int tokenIndex) {
		return tokenIndex/numberOfCases;
	}
	
	
	protected int getCaseIndex(int tokenIndex) {
		return tokenIndex%numberOfCases;
	}
	
	public boolean containsElement(int elementIndex) {
		return validElementIndex(elementIndex) && !tokenBitmap[elementIndex].isEmpty();
	}
	
	public boolean containsCase(int caseIndex) {
		for (int i=0; i<numberOfElements; i++) {
			if (tokenBitmap[i].contains(caseIndex)) {
				return true;
			}
		}
		return false;
	}
	
	public int[] getElementIndexes() {
		MutableIntList indexes = IntLists.mutable.empty();
		for (int i=0; i<numberOfElements; i++) {
			if (!tokenBitmap[i].isEmpty()) indexes.add(i);
		}
		return indexes.toArray();
	}
	
	public int[] getCaseIndexes() {
		RoaringBitmap scanElement = new RoaringBitmap();
		for (RoaringBitmap b : tokenBitmap) {
			scanElement.or(b);
		}
		return scanElement.toArray();
	}
	
	// Return case indexes from an element index
	public int[] getCasesByElementIndex(int elementIndex) {
		return validElementIndex(elementIndex) ? tokenBitmap[elementIndex].toArray() : new int[]{};
	}
	
	// Return element indexes from a case index
	public int[] getElementsByCaseIndex(int caseIndex) {
		MutableIntList elementIndexes = IntLists.mutable.empty();
		for (int i=0; i<numberOfElements; i++) {
			if (tokenBitmap[i].contains(caseIndex)) {
				elementIndexes.add(i);
			}
		}
		return elementIndexes.toArray();
	}
	
	public int[] getTokensByElementIndex(int elementIndex) {
		MutableIntList tokenIndexes = IntLists.mutable.empty();
		for (int caseIndex : this.getCasesByElementIndex(elementIndex)) {
			tokenIndexes.add(getTokenIndex(elementIndex, caseIndex));
		}
		return tokenIndexes.toArray();
	}
	
	public double getTokenDistance(int tokenIndex) {
		return distances.get(tokenIndex);
	}
	
	public long getTimestamp(AnimationContext context) {
        double frameMillis = this.index*context.getFrameInterval()*context.getTimelineRatio();
        return context.getStartTimestamp() + (long)frameMillis;
    }
	
	public boolean isEmpty() {
		for (int i=0; i<numberOfElements; i++) {
			if (!tokenBitmap[i].isEmpty()) return false;
		}
		return true;
	}
	
	/**
	 * A sample of frame JSON:
	 * {
	 * 	index: 100,
	 * 	elements: [
	 * 		{elementIndex1: [{caseIndex1:[0.1]}, {caseIndex2:[0.2]}, {caseIndex3:[0.1]}]},
	 * 		{elementIndex2: [{caseIndex1:[0.2]}, {caseIndex2:[0.5]}]},
	 * 		{elementIndex3: [{caseIndex4:[0.1]}]}
	 * 	]
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
		attJSON.put(df.format(distances.get(tokenIndex)));
		return attJSON;
	}
}
