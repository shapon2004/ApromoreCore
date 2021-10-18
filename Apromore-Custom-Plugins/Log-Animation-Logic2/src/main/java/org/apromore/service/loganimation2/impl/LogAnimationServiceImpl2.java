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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

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
            if (isGraph) enablements = convertEnablements(enablements, diagram, diagramWithGateways);
            return new EnablementLog(log, enablements);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }

    // Convert elementId in the enablements to the graph diagram
    private Map<String, List<EnablementResult>> convertEnablements(Map<String, List<EnablementResult>> enablements,
                                                BPMNDiagram diagram, BPMNDiagram diagramWithGateways) {
        Map<String, String> taskNodeMapping = getTaskNodeIDMapping(diagram, diagramWithGateways);
        BPMNDiagramMapping diagramMapping = new BPMNDiagramMapping(diagram);
        Map<String, List<EnablementResult>> newEnablements = new HashMap<>();
        for (Map.Entry<String, List<EnablementResult>> caseEnablement : enablements.entrySet()) {
            String caseID = caseEnablement.getKey();
            List<EnablementResult> newCaseEnablement = new ArrayList<>();
            List<EnablementResult> taskEnablements = caseEnablement.getValue()
                    .stream()
                    .filter(e -> taskNodeMapping.containsKey(e.getElementId()))
                    .collect(Collectors.toList());
            if (taskEnablements.size() == 1) newCaseEnablement.addAll(taskEnablements);
            for (int i=1; i<taskEnablements.size(); i++) {
                newCaseEnablement.add(taskEnablements.get(i - 1));
                String sourceId = taskNodeMapping.get(taskEnablements.get(i - 1).getElementId());
                String targetId = taskNodeMapping.get(taskEnablements.get(i).getElementId());
                String edgeId = diagramMapping.getEdgeId(sourceId, targetId);
                newCaseEnablement.add(new EnablementResult(caseID, edgeId,
                        taskEnablements.get(i - 1).getEnablementTimestamp(),
                        taskEnablements.get(i - 1).getEnablementTimestamp(),
                        true, false));
                if (i == (taskEnablements.size()-1)) newCaseEnablement.add(taskEnablements.get(i));
            }
            newEnablements.put(caseID, newCaseEnablement);
        }
        return newEnablements;
    }

    /**
     * Get task node ID mapping from otherDiagram  to diagram.
     * This is used for converting from the diagram with XORs to the original graph diagram.
     * @param diagram: diagram without gateways
     * @param otherDiagram: diagram with gateways
     * @return mapping
     */
    private Map<String,String> getTaskNodeIDMapping(BPMNDiagram diagram, BPMNDiagram otherDiagram) {
        BPMNDiagramMapping diagramMapping = new BPMNDiagramMapping(diagram);
        BPMNDiagramMapping otherDiagramMapping = new BPMNDiagramMapping(otherDiagram);
        Map<String,String> mapping = new HashMap<>();
        otherDiagramMapping.getNodeLabels().forEach(label -> {
            String id = diagramMapping.getNodeIdFromLabel(label);
            String otherId = otherDiagramMapping.getNodeIdFromLabel(label);
            if (!id.isEmpty() && !otherId.isEmpty()) mapping.put(otherId, id);
        });
        return mapping;
    }

}
