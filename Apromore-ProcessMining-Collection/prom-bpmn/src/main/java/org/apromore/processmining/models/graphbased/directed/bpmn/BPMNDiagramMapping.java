package org.apromore.processmining.models.graphbased.directed.bpmn;

import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BPMNDiagramMapping {
    private Map<String, BPMNNode> nodeIdToNodeMapping = new HashMap<>();
    private Map<String, BPMNNode> nodeLabelToNodeMapping = new HashMap<>();
    private Map<Map.Entry<String,String>, BPMNEdge> nodeIdToEdgeMapping = new HashMap<>();

    public BPMNDiagramMapping(@NonNull BPMNDiagram d) {
        d.getNodes().forEach(n -> {
            nodeIdToNodeMapping.put(n.getId().toString(), n);
            if (!n.getLabel().isEmpty()) nodeLabelToNodeMapping.put(n.getLabel(), n);
        });
        d.getEdges().forEach(e -> nodeIdToEdgeMapping.put(Map.entry(e.getSource().getId().toString(),
                                                                    e.getTarget().getId().toString()), e));
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
}
