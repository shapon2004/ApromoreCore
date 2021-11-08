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

import lombok.NonNull;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.AnimationException;
import org.apromore.service.loganimation2.AnimationParams;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.json.JSONObject;
import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>CompositeLogEnablement</b> contains data to be used for multiple logs.<br>
 * It consists of {@link LogEnablement} for each log and unique indexes for element IDs
 * on the BPMN diagram (an encoded form of element ID references).
 */
public class CompositeLogEnablement {
    private final ElementIdIndexing elementIdIndexing;
    private final List<LogEnablement> enablements;
    private long startTimestamp = Long.MAX_VALUE;
    private long endTimestamp = Long.MIN_VALUE;

    public CompositeLogEnablement(@NonNull BPMNDiagram diagram,
                                   @NonNull @NotEmpty List<LogEnablement> enablements) {
        this.elementIdIndexing = new ElementIdIndexing(diagram);
        this.enablements = enablements;
        for (LogEnablement enablement: enablements) {
            if (startTimestamp > enablement.getStartTimestamp()) startTimestamp = enablement.getStartTimestamp();
            if (endTimestamp < enablement.getEndTimestamp()) endTimestamp = enablement.getEndTimestamp();
        }
    }

    public List<LogEnablement> getEnablements() {
        return Collections.unmodifiableList(enablements);
    }

    public int getElementIndex(String elementId, boolean isSkip) {
        return isSkip ? elementIdIndexing.getSkipIndex(elementId) : elementIdIndexing.getIndex(elementId);
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public long getEndTimestamp() {
        return this.endTimestamp;
    }

    public JSONObject createSetupData(AnimationParams params) {
        JSONObject setupData = AnimationJSONBuilder2.parseLogCollection(this, params);
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }
}
