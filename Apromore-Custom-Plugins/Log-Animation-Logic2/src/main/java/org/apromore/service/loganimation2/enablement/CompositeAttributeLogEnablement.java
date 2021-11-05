/**
 * #%L
 * This file is part of "Apromore Enterprise Edition".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.service.loganimation2.enablement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.AnimationException;
import org.apromore.service.loganimation2.AnimationParams;
import org.apromore.service.loganimation2.ParamsReader;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.json.JSONObject;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>CompositeAttributeLogEnablement</b> contains data to be used for multiple {@link AttributeLog}s.<br>
 * It consists of AttributeLogEnablement for each log and unique indexes for element IDs
 * on the BPMN diagram (an encoded form of element ID references).
 */
@Getter
public class CompositeAttributeLogEnablement {
    @Getter (AccessLevel.PRIVATE)
    private final ElementIdIndexing elementIdIndexing;
    private final List<AttributeLogEnablement> enablements;
    private long startTimestamp = Long.MAX_VALUE;
    private long endTimestamp = Long.MIN_VALUE;

    public static CompositeAttributeLogEnablement createEnablement(BPMNDiagram bpmnDiagram, List<AttributeLog> logs) throws AnimationException {
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateEnablement(bpmnDiagram, logs, animationParams, false);
    }

    public static CompositeAttributeLogEnablement createEnablementForGraph(BPMNDiagram bpmnDiagramNoGateways,
                                                                           List<AttributeLog> logs)
            throws AnimationException {
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateEnablement(bpmnDiagramNoGateways, logs, animationParams, true);
    }

    private static CompositeAttributeLogEnablement doCreateEnablement(BPMNDiagram diagram, List<AttributeLog> logs,
                                                                      AnimationParams params, boolean isGraph) throws AnimationException {
        List<AttributeLogEnablement> enablmentLogs = logs.stream()
                .map(log -> AttributeLogAligner.align(log, diagram, isGraph))
                .collect(Collectors.toList());
        return new CompositeAttributeLogEnablement(diagram, enablmentLogs);
    }

    private CompositeAttributeLogEnablement(@NonNull BPMNDiagram diagram,
                                            @NonNull @NotEmpty List<AttributeLogEnablement> enablements) {
        this.elementIdIndexing = new ElementIdIndexing(diagram);
        this.enablements = enablements;
        for (AttributeLogEnablement enablement: enablements) {
            if (startTimestamp > enablement.getStartTimestamp()) startTimestamp = enablement.getStartTimestamp();
            if (endTimestamp < enablement.getEndTimestamp()) endTimestamp = enablement.getEndTimestamp();
        }
    }

    public int getElementIndex(String elementId, boolean isSkip) {
        return isSkip ? elementIdIndexing.getSkipIndex(elementId) : elementIdIndexing.getIndex(elementId);
    }

    public JSONObject createSetupData(AnimationParams params) throws Exception {
        JSONObject setupData = AnimationJSONBuilder2.parseLogCollection(this, params);
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }
}
