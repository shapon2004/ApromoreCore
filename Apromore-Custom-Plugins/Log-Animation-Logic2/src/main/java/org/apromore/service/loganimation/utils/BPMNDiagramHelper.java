/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2015 - 2017 Queensland University of Technology.
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

package org.apromore.service.loganimation.utils;

import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.model.gateway.ExclusiveGateway;
import de.hpi.bpmn2_0.model.gateway.InclusiveGateway;
import de.hpi.bpmn2_0.model.gateway.ParallelGateway;
import org.jgrapht.DirectedGraph;

import java.util.*;

public class BPMNDiagramHelper {
    
    // key: taskId, value is reference to the node
    private Map<String, FlowNode> allNodes = new HashMap();
    
    // key: concatenation of taskId of source and target node
    // value: reference to edge object
    private Map<String, SequenceFlow> allSequenceFlows = new HashMap();

    private Set<FlowNode> joins = new HashSet();

    private DirectedGraph directedGraph = null;
    private List<List<FlowNode>> bpmnCycles = null;

    public BPMNDiagramHelper() {
    }

    public Collection<FlowNode> getAllNodes()  {
        return allNodes.values();
    }
    
    public Set<FlowNode> getAllJoins() {
        return joins;
    }
    
    public Collection<SequenceFlow> getAllSequenceFlows() {
        return allSequenceFlows.values();
    }
    
    public static boolean isJoin(FlowNode node) {
        return (node instanceof ParallelGateway &&
                node.getOutgoingSequenceFlows().size() == 1 &&
                node.getIncomingSequenceFlows().size() > 1);
    }
    
    public static boolean isFork(FlowNode node) {
        return (node instanceof ParallelGateway &&
                node.getOutgoingSequenceFlows().size() > 1 &&
                node.getIncomingSequenceFlows().size() == 1);
    }
    
    public static boolean isDecision(FlowNode node) {
        return (node instanceof ExclusiveGateway &&
                node.getOutgoingSequenceFlows().size() > 1 &&
                node.getIncomingSequenceFlows().size() == 1);
    }
    
    public static boolean isMerge(FlowNode node) {
        return (node instanceof ExclusiveGateway &&
                node.getOutgoingSequenceFlows().size() == 1 &&
                node.getIncomingSequenceFlows().size() > 1);
    }
    
    public static boolean isORSplit(FlowNode node) {
        return (node instanceof InclusiveGateway &&
                node.getOutgoingSequenceFlows().size() > 1 &&
                node.getIncomingSequenceFlows().size() == 1);
    }
    
    public static boolean isORJoin(FlowNode node) {
        return (node instanceof InclusiveGateway &&
                node.getOutgoingSequenceFlows().size() == 1 &&
                node.getIncomingSequenceFlows().size() > 1);
    }
    
}
