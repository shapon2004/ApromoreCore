package org.apromore.processmining.models.graphbased.directed.bpmn;

import lombok.NonNull;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Event;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BPMNDiagramMapping {
    private final Map<String, BPMNNode> nodeIdToNodeMapping;
    private final Map<String, BPMNNode> nodeLabelToNodeMapping;
    private final Map<Map.Entry<String,String>, BPMNEdge> nodeIdToEdgeMapping;

    public BPMNDiagramMapping(@NonNull BPMNDiagram d) {
        nodeIdToNodeMapping = d.getNodes().stream().collect(Collectors.toMap((BPMNNode n) -> n.getId().toString(), Function.identity()));
        nodeLabelToNodeMapping = d.getNodes().stream()
                                    .filter(n -> n.getLabel() != null && !n.getLabel().isEmpty())
                                    .collect(Collectors.toMap(BPMNNode::getLabel, Function.identity()));
        nodeIdToEdgeMapping = d.getEdges().stream().collect(Collectors.toMap(
                (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> e) ->
                        Map.entry(e.getSource().getId().toString(), e.getTarget().getId().toString()),
                Function.identity()));
    }

    public BPMNNode getNodeFromId(@NonNull String nodeId) {
        return nodeIdToNodeMapping.get(nodeId);
    }

    public BPMNNode getNodeFromLabel(@NonNull String nodeLabel) {
        return nodeLabelToNodeMapping.get(nodeLabel);
    }

    public String getNodeIdFromLabel(@NonNull String nodeLabel) {
        BPMNNode node = getNodeFromLabel(nodeLabel);
        return node == null ? "" : node.getLabel();
    }

    public String getNodeLabelFromId(@NonNull String nodeId) {
        BPMNNode node = getNodeFromId(nodeId);
        return node == null ? "" : node.getLabel();
    }

    public Set<String> getNodeIDs() {
        return nodeIdToNodeMapping.keySet();
    }

    public Set<String> getNodeLabels() {
        return nodeLabelToNodeMapping.keySet();
    }

    public BPMNEdge getEdge(String sourceId, String targetId) {
        return nodeIdToEdgeMapping.get(Map.entry(sourceId, targetId));
    }

    public String getEdgeId(String sourceId, String targetId) {
        BPMNEdge edge = getEdge(sourceId, targetId);
        return edge == null ? "" : edge.getEdgeID().toString();
    }

    public Collection<String> getTaskIDs() {
        return nodeIdToNodeMapping.values().stream()
                .filter(n -> n instanceof Activity || n instanceof Event)
                .map(n -> n.getId().toString())
                .collect(Collectors.toSet());
    }
}
