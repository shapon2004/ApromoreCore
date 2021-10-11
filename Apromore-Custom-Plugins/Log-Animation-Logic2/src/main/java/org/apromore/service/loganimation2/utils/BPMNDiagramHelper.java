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

package org.apromore.service.loganimation2.utils;

import de.hpi.bpmn2_0.model.*;
import de.hpi.bpmn2_0.model.Process;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.model.event.EndEvent;
import de.hpi.bpmn2_0.model.event.StartEvent;
import de.hpi.bpmn2_0.model.gateway.ExclusiveGateway;
import de.hpi.bpmn2_0.model.gateway.InclusiveGateway;
import de.hpi.bpmn2_0.model.gateway.ParallelGateway;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.*;
import java.util.stream.Collectors;

public class BPMNDiagramHelper {
    private ModelChecker checker = new ModelChecker();
    private Map<String, FlowNode> idToNode = new HashMap<>();
    private Map<Pair<String,String>, SequenceFlow> nodeNamesToSequenceFlow = new HashMap<>();
    private Map<FlowNode,Set<FlowNode>> targets = new HashMap();
    private Map<FlowNode,Set<FlowNode>> sources = new HashMap();
    private StartEvent startEvent;
    private EndEvent endEvent;

    public BPMNDiagramHelper(Definitions definition) {
        List<BaseElement> rootElements = definition.getRootElement();
        if (rootElements.size() == 1) {
            BaseElement rootElement = rootElements.get(0);

            if (rootElement instanceof Process) {
                Process process = (Process)rootElement;
                for (FlowElement element : process.getFlowElement()) {
                    element.acceptVisitor(checker);
                    if (element instanceof StartEvent) {
                        startEvent = (StartEvent)element;
                    }
                    else if (element instanceof EndEvent) {
                        endEvent = (EndEvent)element;
                    }
                    else if (element instanceof FlowNode) {
                        idToNode.put(element.getId(), (FlowNode)element);
                        targets.put((FlowNode)element, ((FlowNode)element).getOutgoingSequenceFlows()
                                        .stream()
                                        .map(f -> (FlowNode)f.getTargetRef())
                                        .collect(Collectors.toSet()));
                        sources.put((FlowNode)element, ((FlowNode)element).getIncomingSequenceFlows()
                                .stream()
                                .map(f -> (FlowNode)f.getSourceRef())
                                .collect(Collectors.toSet()));
                    }
                    else if (element instanceof SequenceFlow) {
                        SequenceFlow flow = (SequenceFlow)element;
                        nodeNamesToSequenceFlow.put(Tuples.pair(flow.getSourceRef().getName(), flow.getTargetRef().getName()), flow);
                    }
                }
            }
        }
    }

    public boolean isValidModel() {
        return checker.isValid();
    }

    public FlowNode getNode(String nodeName) {
        return idToNode.get(nodeName);
    }

    public SequenceFlow getSequenceFlow(String sourceNode, String targetNode) {
        return nodeNamesToSequenceFlow.get(Tuples.pair(sourceNode, targetNode));
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

    public Set<FlowNode> getTargets(FlowNode element) {
        return targets.containsKey(element) ? targets.get(element) : new HashSet();
    }

    public Set<FlowNode> getSources(FlowNode element) {
        return sources.containsKey(element) ? sources.get(element) : new HashSet();
    }

    public FlowNode getStartEvent() {
        return startEvent;
    }

    public FlowNode getEndEvent() {
        return endEvent;
    }

    public static BPMNDiagram createBPMNDiagramWithGateways(BPMNDiagram diagram) {
        BPMNDiagram newDiagram = BPMNDiagramFactory.cloneBPMNDiagram(diagram);

        for (BPMNNode node : newDiagram.getNodes()) {
            // Add XOR-Split
            if (newDiagram.getOutEdges(node).size() > 1) {
                Gateway split = newDiagram.addGateway("", Gateway.GatewayType.DATABASED);
                for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : newDiagram.getOutEdges(node)) {
                    newDiagram.addFlow(split, edge.getTarget(), "");
                    newDiagram.removeEdge(edge);
                }
                newDiagram.addFlow(node, split, "");
            }

            // Add XOR-Join
            if (newDiagram.getInEdges(node).size() > 1) {
                Gateway join = newDiagram.addGateway("", Gateway.GatewayType.DATABASED);
                for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : newDiagram.getInEdges(node)) {
                    newDiagram.addFlow(edge.getSource(), join, "");
                    newDiagram.removeEdge(edge);
                }
                newDiagram.addFlow(join, node, "");
            }
        }

        return newDiagram;
    }
}
