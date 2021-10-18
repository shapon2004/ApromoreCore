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

import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.apromore.processmining.models.graphbased.directed.bpmn.elements.Gateway;

public class BPMNDiagramHelper {
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
