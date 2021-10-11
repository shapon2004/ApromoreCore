package org.apromore.service.loganimation2.replay;

import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Gateway;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Marking extends HashSet<BPMNEdge> {
    public static Marking EMPTY = new Marking();

    private Marking() {
        super(Collections.emptySet());
    }

    private Marking(Marking copyFrom) {
        super(copyFrom);
    }

    public Marking fire(BPMNNode fireNode, BPMNDiagram diagram) {
        List<BPMNEdge> fireArcs = this.stream().filter(a -> a.getTarget() == fireNode).collect(Collectors.toList());
        if (fireArcs.isEmpty()) return Marking.EMPTY;
        BPMNEdge fireArc = fireArcs.get(0);
        this.remove(fireArc);
        BPMNNode node = (BPMNNode) fireArc.getTarget();
        if (node instanceof Activity) {
            this.addAll(diagram.getOutEdges(node));
        }
        else if (node instanceof Gateway) {
//            if (((Gateway)node).get
        }
        return null;
    }

}
