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

import lombok.NonNull;
import org.apromore.alignmentautomaton.ApromLog;
import org.apromore.alignmentautomaton.CaseVariant;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.logman.attribute.log.variants.AttributeTraceVariants;

import java.util.List;

public class ApromLogAdapter {
    public static ApromLog adaptLog(@NonNull AttributeLog attLog) {
        ApromLog apromLog = new ApromLog();
        attLog.getTraces().forEach(trace -> {
            trace.getValueTrace().forEachWithIndex((index, value) -> {
                apromLog.addEntry(trace.getTraceId(),
                                attLog.getStringFromValue(trace.getAttributeValueAtIndex(index)),
                                String.valueOf(trace.getStartTimeAtIndex(index)),
                                String.valueOf(trace.getEndTimeAtIndex(index)));
            });
        });
        return apromLog;
    }

    public static List<CaseVariant> adaptCaseVariants(@NonNull AttributeLog attLog) {
        AttributeTraceVariants variants = attLog.getVariantView().getActiveVariants();
        return variants.getVariants().collectWithIndex((variant, index) -> new CaseVariant(index,
                                                variants.getActivityNamesOfVariantIndex(index),
                                                variants.getCaseIdsOfVariantIndex(index),
                                                variants.getFrequency(variant))).toList();

    }

}