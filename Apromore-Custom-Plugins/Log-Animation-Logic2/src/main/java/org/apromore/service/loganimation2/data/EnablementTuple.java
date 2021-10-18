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

import lombok.Getter;
import lombok.NonNull;

/**
 * <b>EnablementTuple</b> represents enablement data for one modelling element on BPMN diagrams
 */
@Getter
public class EnablementTuple {
    private final String elementId;
    private final boolean isSkip;
    private final long startTimestamp;
    private final long endTimestamp;

    public static EnablementTuple valueOf(@NonNull String elementId, boolean isSkip,
                                          long startTimestamp, long endTimestamp) {
        return new EnablementTuple(elementId, isSkip, startTimestamp, endTimestamp);
    }

    private EnablementTuple(String elementId, boolean isSkip, long startTimestamp, long endTimestamp) {
        this.elementId = elementId;
        this.isSkip = isSkip;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }
}
