/**
 * #%L
 * This file is part of "Apromore Core".
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
package org.apromore.service.loganimation2.enablement;

import one.util.streamex.StreamEx;
import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagramMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>TaskEnablementConverter</b> is used to convert enablement data to match a BPMN diagram.
 * For example, it is used to convert enablement data from aligning a BPMN diagram with XOR gateways
 * to the same BPMN diagram without XOR gateways (graph).
 */
public class TaskEnablementConverter {
    /**
     * Convert log enablements to match a diagram
     * @param enablements the log enablement to convert
     * @param diagram the diagram to match
     * @return new log enablement
     */
    public static Map<String, List<EnablementResult>> convertLogEnablement(Map<String, List<EnablementResult>> enablements,
                                                                          BPMNDiagram diagram) {
        return enablements.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> convertCaseEnablement(entry.getValue(), new BPMNDiagramMapping(diagram))));
    }

    // Convert
    private static List<EnablementResult> convertCaseEnablement(List<EnablementResult> caseEnablement,
                                                                BPMNDiagramMapping diagramMapping) {
        // Select tasks
        List<EnablementResult> taskEnablements = caseEnablement.stream()
                .filter(e -> diagramMapping.getTaskIDs().contains(e.getElementId()))
                .collect(Collectors.toList());

        // Add the only one
        List<EnablementResult> newCaseEnablement = taskEnablements.size() == 1
                ? new ArrayList<>(List.of(taskEnablements.get(0)))
                : new ArrayList<>();

        // Add pairs of source and outgoing flow if there are more than one
        newCaseEnablement.addAll(
                StreamEx.of(taskEnablements.stream())
                        .pairMap(Map::entry)
                        .map(p -> collectSourceAndFlow(p, diagramMapping))
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));

        // Add the last one if there are more than one
        newCaseEnablement.addAll(taskEnablements.size() > 1
                ? List.of(taskEnablements.get(taskEnablements.size()-1))
                : List.of());


        return newCaseEnablement;
    }

    /**
     * Collect the source task and its outgoing flow enablement from a pair of tasks
     * @param pair pair of executive task EnablementResult including the source and target
     * @return list of enablement result of the source and its outgoing flow
     */
    private static List<EnablementResult> collectSourceAndFlow(Map.Entry<EnablementResult, EnablementResult> pair,
                                                               BPMNDiagramMapping diagramMapping) {
        return List.of(
                pair.getKey(),
                new EnablementResult(
                        pair.getKey().getCaseId(),
                        diagramMapping.getEdgeId(pair.getKey().getElementId(), pair.getValue().getElementId()),
                        pair.getKey().getEnablementTimestamp(),
                        pair.getKey().getEnablementTimestamp(),
                        true, false));
    }
}
