/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2017 Queensland University of Technology.
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

package org.apromore.service.loganimation2.impl;

import one.util.streamex.StreamEx;
import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.plugin.DefaultParameterAwarePlugin;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagramMapping;
import org.apromore.service.loganimation2.AnimationException;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.ParamsReader;
import org.apromore.service.loganimation2.data.AnimationData;
import org.apromore.service.loganimation2.data.AnimationParams;
import org.apromore.service.loganimation2.data.ApromLogAdapter;
import org.apromore.service.loganimation2.data.EnablementLog;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.eclipse.collections.api.tuple.Pair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("logAnimationService2")
@Qualifier("logAnimationService2")
public class LogAnimationServiceImpl2 extends DefaultParameterAwarePlugin implements LogAnimationService2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimationServiceImpl2.class);
    private final AlignmentClient alignmentServiceClient;

    public LogAnimationServiceImpl2(AlignmentClient alignmentServiceClient) {
        this.alignmentServiceClient = alignmentServiceClient;
    }

    @Override
    public AnimationData createAnimationData(BPMNDiagram bpmnDiagram, List<AttributeLog> logs) throws AnimationException {
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateAnimation(bpmnDiagram, logs, animationParams, false);
    }

    @Override
    public AnimationData createAnimationDataForGraph(BPMNDiagram bpmnDiagramNoGateways, List<AttributeLog> logs)
            throws AnimationException {
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateAnimation(bpmnDiagramNoGateways, logs, animationParams, true);
    }

    @Override
    public JSONObject createSetupData(AnimationData animationData, AnimationParams params) throws Exception {
        JSONObject setupData = AnimationJSONBuilder2.parseLogCollection(animationData, params);
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }

    private AnimationData doCreateAnimation(BPMNDiagram diagram, List<AttributeLog> logs, AnimationParams params, boolean isGraph) throws AnimationException {
        List<EnablementLog> enablmentLogs = logs.stream()
                .map(log -> this.align(log, diagram, isGraph))
                .collect(Collectors.toList());
        return new AnimationData(diagram, enablmentLogs);
    }

    private EnablementLog align(@NotNull AttributeLog log, BPMNDiagram diagram, boolean isGraph) throws AnimationException {
        BPMNDiagram diagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram) : diagram;
        try {
            Map<String, List<EnablementResult>> enablements = alignmentServiceClient.computeEnablement(diagramWithGateways,
                    "",
                    ApromLogAdapter.adaptLog(log),
                    ApromLogAdapter.adaptCaseVariants(log),
                    Collections.emptyMap());
            if (isGraph) enablements = convertEnablements(enablements, diagram);
            return new EnablementLog(log, enablements);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }

    /**
     * Convert log enablements to match a diagram
     * @param enablements the log enablement to convert
     * @param diagram the diagram to match
     * @return new log enablement
     */
    private Map<String, List<EnablementResult>> convertEnablements(Map<String, List<EnablementResult>> enablements,
                                                BPMNDiagram diagram) {
        return enablements.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> convertCaseEnablement(entry.getValue(), new BPMNDiagramMapping(diagram))));
    }

    // Convert
    private List<EnablementResult> convertCaseEnablement(List<EnablementResult> caseEnablement,
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
     * Collect source, flow, target enablement from a pair
     * @param pair pair of EnablementResult including source and target
     * @return list of enablement result including source, flow and target
     */
    private List<EnablementResult> collectSourceAndFlow(Map.Entry<EnablementResult, EnablementResult> pair,
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

    /**
     * Get task node ID mapping from otherDiagram to diagram.
     * This is used for converting from the diagram with XORs to the original graph diagram.
     * @param diagram: diagram without gateways
     * @param otherDiagram: diagram with gateways
     * @return mapping
     */
    private Map<String,String> getTaskNodeIDMapping(BPMNDiagram diagram, BPMNDiagram otherDiagram) {
        BPMNDiagramMapping diagramMapping = new BPMNDiagramMapping(diagram);
        BPMNDiagramMapping otherDiagramMapping = new BPMNDiagramMapping(otherDiagram);
        return otherDiagramMapping.getNodeLabels().stream()
                .map(label -> Map.entry(diagramMapping.getNodeIdFromLabel(label), otherDiagramMapping.getNodeIdFromLabel(label)))
                .filter(entry -> !entry.getKey().isEmpty() && !entry.getValue().isEmpty())
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

}
