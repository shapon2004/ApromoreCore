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
package org.apromore.service.loganimation2.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * <b>AnimationData</b> contains data to be used for creating animation movies.<br>
 * It consists of EnablementLog for each log (if animating multiple logs), unique indexes for element IDs
 * on the BPMN diagram (an encoded form of of element ID references), and contains JSON setup data for the animation.
 */
@Getter
public class AnimationData {
    @Getter (AccessLevel.PRIVATE)
    private final ElementIdIndexing elementIdIndexing;

    private final List<EnablementLog> enablementLogs;
    private long startTimestamp = Long.MAX_VALUE;
    private long endTimestamp = Long.MIN_VALUE;

    public AnimationData(@NonNull BPMNDiagram diagram, @NonNull @NotEmpty List<EnablementLog> logs) {
        this.elementIdIndexing = new ElementIdIndexing(diagram);
        this.enablementLogs = logs;
        for (EnablementLog log: logs) {
            if (startTimestamp > log.getStartTimestamp()) startTimestamp = log.getStartTimestamp();
            if (endTimestamp < log.getEndTimestamp()) endTimestamp = log.getEndTimestamp();
        }
    }

    public int getElementIndex(String elementId, boolean isSkip) {
        return isSkip ? elementIdIndexing.getSkipIndex(elementId) : elementIdIndexing.getIndex(elementId);
    }
}
