package org.apromore.service.loganimation2.enablement;

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
