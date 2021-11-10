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

import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.AnimationException;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttributeLogAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeLogAligner.class);

    private AlignmentClient alignmentServiceClient;

    public AttributeLogAligner() {
        alignmentServiceClient = new AlignmentClient("");
    }

    public CompositeLogEnablement createEnablement(BPMNDiagram bpmnDiagram, List<AttributeLog> logs) throws AnimationException {
        return doCreateEnablement(bpmnDiagram, logs, false);
    }

    public CompositeLogEnablement createEnablementForGraph(BPMNDiagram bpmnDiagramNoGateways,
                                                                  List<AttributeLog> logs)
            throws AnimationException {
        return doCreateEnablement(bpmnDiagramNoGateways, logs, true);
    }

    private CompositeLogEnablement doCreateEnablement(BPMNDiagram diagram, List<AttributeLog> logs,
                                                             boolean isGraph) throws AnimationException {
        List<LogEnablement> logEnablements = logs.stream()
                .map(log -> align(log, diagram, isGraph))
                .collect(Collectors.toList());
        return new CompositeLogEnablement(diagram, logEnablements);
    }

    private LogEnablement align(@NotNull AttributeLog log, BPMNDiagram diagram, boolean isGraph) throws AnimationException {
        BPMNDiagram diagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram) : diagram;
        try {
            Map<String, List<EnablementResult>> enablements = alignmentServiceClient.computeEnablement(diagramWithGateways,
                    "",
                    ApromLogAdapter.adaptLog(log),
                    ApromLogAdapter.adaptCaseVariants(log),
                    Collections.emptyMap());
            if (isGraph) enablements = TaskEnablementConverter.convertLogEnablement(enablements, diagram);
            return new LogEnablement(log, enablements);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }
}
