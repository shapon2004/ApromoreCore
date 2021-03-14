/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.plugin.portal.processdiscoverer.animation;

import java.util.List;

import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hpi.bpmn2_0.model.BaseElement;
import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.FlowElement;
import de.hpi.bpmn2_0.model.Process;
import de.hpi.bpmn2_0.model.activity.Activity;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;

/**
 * Mapping from modelling element IDs to indexes and vice versa.
 * @author Bruce Nguyen
 *
 */
public class ModelMapping {
	private HashBiMap<String, Integer> elementIdToIndexMap = new HashBiMap<>();
	private HashBiMap<String, Integer> elementIdToSkipIndexMap = new HashBiMap<>();
	
	public ModelMapping(Definitions diagram) {
		int index=0;
		
	    List<BaseElement> rootElements = diagram.getRootElement();
	    if (rootElements.size() == 1) {
	        BaseElement rootElement = rootElements.get(0);
	        if (rootElement instanceof Process) {
	            Process process = (Process)rootElement;
	            for (FlowElement element : process.getFlowElement()) {
	                if (element instanceof Activity || element instanceof SequenceFlow) {
	                    elementIdToIndexMap.put(element.getId(), index);
	                    index++;
	                    if (element instanceof Activity) {
	                        elementIdToSkipIndexMap.put(element.getId(), index);
	                        index++;
	                    }
	                }                  
	            }
	        }
	    }
	}
	
	public int getIndex(String elementId) {
		return elementIdToIndexMap.getIfAbsent(elementId, () -> -1);
	}
	
    public int getSkipIndex(String elementId) {
        return elementIdToSkipIndexMap.getIfAbsent(elementId, () -> -1);
    }	
	
	public String getId(int index) {
		return elementIdToIndexMap.inverse().getIfAbsent(index, () -> "");
	}
	
	public String getIdFromSkipIndex(int index) {
	    return elementIdToSkipIndexMap.inverse().getIfAbsent(index, () -> "");
	}

	public int size() {
        return elementIdToIndexMap.size() + elementIdToSkipIndexMap.size();
    }
    
	public void clear() {
		this.elementIdToIndexMap.clear();
		this.elementIdToSkipIndexMap.clear();
	}
	
	// two arrays, first is the mapping from id to normal index
	// second is mapping from id to skip index
	public JSONArray getElementJSON() throws JSONException {
	    JSONArray jsonArray = new JSONArray();
        jsonArray.put(this.getJSONMap(this.elementIdToIndexMap));
        jsonArray.put(this.getJSONMap(this.elementIdToSkipIndexMap));
        return jsonArray;
	}
	
    private JSONObject getJSONMap(HashBiMap<String, Integer> idToIndexMap) throws JSONException {
        JSONObject json = new JSONObject();
        for (String elementId : idToIndexMap.keySet()) {
            json.put(idToIndexMap.get(elementId).toString(), elementId);
        }
        return json;
    }	
}
