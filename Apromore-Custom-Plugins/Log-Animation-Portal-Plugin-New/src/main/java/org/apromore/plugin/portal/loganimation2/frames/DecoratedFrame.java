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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * DecoratedFrame is derived from a {@link Frame} with added attributes for visualization of the token.
 * The token in a DecoratedFrame can be combined from multiple tokens in the corresponding Frame.
 * The visual attributes can be token size, color, shape.
 * 
 * @author Bruce Nguyen
 *
 */
public class DecoratedFrame extends Frame {
	private MutableIntIntMap counts = IntIntMaps.mutable.empty();
	
	public DecoratedFrame(Frame frame) throws InvalidFrameParamsException {
		super(frame.getIndex(), frame.getNumberOfElements(), frame.getNumberOfCases());
		for (int elementIndex : frame.getElementIndexes()) {
			Map<Integer,Double> tokenDistanceMap = new HashMap<>();
			for (int tokenIndex : frame.getTokensByElementIndex(elementIndex)) {
				tokenDistanceMap.put(tokenIndex, frame.getTokenDistance(tokenIndex));
			}
			
			// Sort tokens by the distance
			Map<Integer,Double> sortedTokenDistanceMap = tokenDistanceMap.entrySet()
																	.stream()
																	.sorted(Map.Entry.comparingByValue())
																	.collect(Collectors.toMap(Map.Entry::getKey, 
																			Map.Entry::getValue,
																			(oldValue, newValue) -> oldValue, 
																			LinkedHashMap::new));
			// Group tokens with close distances
			Set<List<Integer>> tokenBags = new HashSet<>();
			List<Integer> tokenBag = new ArrayList<>();
			double tokenBagTotal = 0;
			for (Integer token : sortedTokenDistanceMap.keySet()) {
				double tokenDistance = sortedTokenDistanceMap.get(token);
				double diff = tokenBag.isEmpty() ? 0 : Math.abs(tokenDistance - tokenBagTotal/tokenBag.size());
				if (diff <= 0.01) {
					tokenBag.add(token);
					tokenBagTotal += tokenDistance;
				}
				else {
					tokenBags.add(tokenBag);
					tokenBag = new ArrayList<>();
					tokenBagTotal = 0;
				}
			}
			if (!tokenBag.isEmpty()) tokenBags.add(tokenBag); // the last bag
			
			// Collect representative tokens and its count
			Map<Integer,Integer> repTokenCountMap = new HashMap<>();
			for (List<Integer> bag : tokenBags) {
				repTokenCountMap.put(bag.get(0), bag.size());
			}

			// Add representative tokens, its distance and count
			for (Integer token : repTokenCountMap.keySet()) {
				this.addToken(elementIndex, getCaseIndex(token), sortedTokenDistanceMap.get(token), repTokenCountMap.get(token));
			}
		}
	}
	
	public void addToken(int elementIndex, int caseIndex, double distance, int size) {
		if (validElementIndex(elementIndex) && validCaseIndex(caseIndex)) {
			int tokenIndex = getTokenIndex(elementIndex, caseIndex);
			tokenBitmap[elementIndex].add(caseIndex);
			distances.put(tokenIndex, distance);
			counts.put(tokenIndex, size);
		}
	}
	
	@Override
	protected JSONArray getAttributesJSON(int tokenIndex) throws JSONException {
		JSONArray attJSON = new JSONArray();
		DecimalFormat df = new DecimalFormat("#.###");
		attJSON.put(df.format(distances.get(tokenIndex)));
		attJSON.put(counts.get(tokenIndex));
		return attJSON;
	}
}
