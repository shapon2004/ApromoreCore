/*-
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
package org.apromore.apmlog.stats;

import org.apromore.apmlog.APMLog;
import org.apromore.apmlog.ATrace;
import org.apromore.apmlog.logobjects.ActivityInstance;
import org.apromore.apmlog.filter.PLog;
import org.apromore.apmlog.filter.PTrace;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.stream.Collectors;

public class LogStatsAnalyzer {

    // ===================================================================================================
    // Common data
    // ===================================================================================================
    public static UnifiedMap<String, UnifiedSet<EventAttributeValue>> getEventAttributeValues(
            List<ActivityInstance> activities, long tracesSize) {

        UnifiedMap<String, UnifiedMap<String, UnifiedSet<ActivityInstance>>> eavaMap = new UnifiedMap<>();

        for (ActivityInstance activity : activities) {
            UnifiedMap<String, String> attributes = activity.getAttributes();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!eavaMap.containsKey(entry.getKey())) {
                    UnifiedSet<ActivityInstance> actSet = new UnifiedSet<>();
                    actSet.add(activity);
                    UnifiedMap<String, UnifiedSet<ActivityInstance>> eavvMap = new UnifiedMap<>();
                    eavvMap.put(entry.getValue(), actSet);
                    eavaMap.put(entry.getKey(), eavvMap);
                } else {
                    UnifiedMap<String, UnifiedSet<ActivityInstance>> eavvMap = eavaMap.get(entry.getKey());
                    if (!eavvMap.containsKey(entry.getValue())) {
                        UnifiedSet<ActivityInstance> actSet = new UnifiedSet<>();
                        actSet.add(activity);
                        eavvMap.put(entry.getValue(), actSet);
                    } else {
                        eavvMap.get(entry.getValue()).put(activity);
                    }
                }
            }
        }

        UnifiedMap<String, UnifiedSet<EventAttributeValue>> eavMap = new UnifiedMap<>();

        for (Map.Entry<String, UnifiedMap<String, UnifiedSet<ActivityInstance>>> entry : eavaMap.entrySet()) {

            eavMap.put(entry.getKey(), new UnifiedSet<>(entry.getValue().size()));

            UnifiedMap<String, UnifiedSet<ActivityInstance>> vals = entry.getValue();
            for (Map.Entry<String, UnifiedSet<ActivityInstance>> valEntry : vals.entrySet()) {
                EventAttributeValue eav =
                        new EventAttributeValue(valEntry.getKey(), valEntry.getValue(), tracesSize);

                eavMap.get(entry.getKey()).put(eav);
            }
        }

        return eavMap;
    }


    public static UnifiedMap<String, UnifiedSet<CaseAttributeValue>> getCaseAttributeValues(List<ATrace> traces) {

        UnifiedSet<String> allKeys = new UnifiedSet<>();
        for (ATrace trace : traces) {
            allKeys.addAll(trace.getAttributes().keySet());
        }

        UnifiedMap<String, Map<String, List<ATrace>>> keyValCaseOccurMap = new UnifiedMap<>();

        for (String key : allKeys) {
            Map<String, List<ATrace>> grouped = traces.stream()
                    .filter(x -> x.getAttributes().containsKey(key))
                    .collect(Collectors.groupingBy(x -> x.getAttributes().get(key)));
            keyValCaseOccurMap.put(key, grouped);
        }

        // (3) create CaseAttributeValues
        UnifiedMap<String, UnifiedSet<CaseAttributeValue>> caseAttributeValues = new UnifiedMap<>();

        for (Map.Entry<String, Map<String, List<ATrace>>> entry : keyValCaseOccurMap.entrySet()) {
            String attrKey = entry.getKey();
            Map<String, List<ATrace>> valOccurMap = entry.getValue();

            UnifiedSet<CaseAttributeValue> cavSet = new UnifiedSet<>();

            int[] arr = valOccurMap.entrySet().stream().mapToInt(x -> x.getValue().size()).toArray();
            IntArrayList ial = new IntArrayList(arr);

            int maxOccurSize = ial.max();

            for (Map.Entry<String, List<ATrace>> voe: valOccurMap.entrySet()) {
                int[] occurredIndexes = voe.getValue().stream()
                        .mapToInt(ATrace::getImmutableIndex)
                        .toArray();
                IntArrayList indexes = new IntArrayList(occurredIndexes);

                CaseAttributeValue cav = new CaseAttributeValue(voe.getKey(), indexes, traces.size());
                cav.setRatio(100 * ( (double) cav.getOccurCasesIndexSet().size() / maxOccurSize));
                cavSet.add(cav);
            }
            caseAttributeValues.put(attrKey, cavSet);
        }

        return caseAttributeValues;
    }

    public static Map<Integer, List<ATrace>> getCaseVariantGroupMap(List<ATrace> traces) {
        Map<String, List<ATrace>> groups = traces.stream()
                .collect(Collectors.groupingBy(x -> x.getCaseVariantIndicator()));

        List<Map.Entry<String, List<ATrace>>> sorted = groups.entrySet().stream()
                .sorted( (f1, f2) -> Long.compare(f2.getValue().size(), f1.getValue().size()) )
                .collect(Collectors.toList());

        Map<Integer, List<ATrace>> caseVariantGroupMap = new UnifiedMap<>(sorted.size());

        int variId = 1;
        for (Map.Entry<String, List<ATrace>> entry : sorted) {

            caseVariantGroupMap.put(variId, entry.getValue());

            for (ATrace trace : entry.getValue()) {
                trace.setCaseVariantId(variId);
            }

            variId += 1;
        }

        return caseVariantGroupMap;
    }

    public static void updateCaseVariants(List<ATrace> traces) {
        Map<String, List<ATrace>> groups = traces.stream()
                .collect(Collectors.groupingBy(x -> x.getCaseVariantIndicator()));

        List<Map.Entry<String, List<ATrace>>> sorted = groups.entrySet().stream()
                .sorted( (f1, f2) -> Long.compare(f2.getValue().size(), f1.getValue().size()) )
                .collect(Collectors.toList());

        int variId = 1;
        for (Map.Entry<String, List<ATrace>> entry : sorted) {
            for (ATrace trace : entry.getValue()) {
                trace.setCaseVariantId(variId);
            }
            variId += 1;
        }
    }

    public static int getNodeDurationSize(String attributeKey, String value, APMLog apmLog) {
        if (!apmLog.getImmutableEventAttributeValues().containsKey(attributeKey)) return 0;

        EventAttributeValue eav = apmLog.getImmutableEventAttributeValues().get(attributeKey).stream()
                .filter(x -> x.getValue().equals(value))
                .findFirst()
                .orElse(null);

        if (eav == null) return 0;

        UnifiedSet<ActivityInstance> existActs = new UnifiedSet<>(apmLog.getActivityInstances());
        UnifiedSet<ActivityInstance> activitySet = eav.getOccurActivities(existActs);

        double[] allDurs = activitySet.stream()
                .mapToDouble(ActivityInstance::getDuration)
                .toArray();

        return allDurs.length;
    }

    public static int getArcDurationSize(String key, String sourceNode, String targetNode, APMLog apmLog) {

        UnifiedSet<EventAttributeValue> set = apmLog.getImmutableEventAttributeValues().get(key);

        EventAttributeValue sourceEav = set.stream()
                .filter(x -> x.getValue().equals(sourceNode))
                .findFirst()
                .orElse(null);

        if (sourceEav == null) return 0;

        // ================================
        // target activity, source activity
        // ================================
        Map<ActivityInstance, ActivityInstance> validFollows = getNextActivities(sourceEav, apmLog);

        List<CustomTriple> triples = new ArrayList<>(validFollows.size());
        for (Map.Entry<ActivityInstance, ActivityInstance> pair : validFollows.entrySet()) {
            // ===========================================================
            // target activity, source activity, target attribute value
            // ===========================================================
            triples.add(CustomTriple.of(pair.getKey(), pair.getValue(), pair.getValue().getAttributes().get(key)));
        }

        Map<String, List<CustomTriple>> groups =
                triples.stream().collect(Collectors.groupingBy(CustomTriple::getValue));

        List<CustomTriple> data = groups.get(targetNode);

        return data.stream().mapToDouble(CustomTriple::getDuration).toArray().length;

    }

    private static Map<ActivityInstance, ActivityInstance> getNextActivities(EventAttributeValue v, APMLog apmLog) {
        UnifiedSet<ActivityInstance> existActs = new UnifiedSet<>(apmLog.getActivityInstances());

        return v.getOccurActivities(existActs).stream()
                .collect(Collectors.toMap(x -> x, x -> getParentTrace(x.getParentTraceId(), apmLog).getNextOf(x)));
    }

    private static ATrace getParentTrace(String traceId, APMLog apmLog) {
        return apmLog.getTraces().stream().filter(x -> x.getCaseId().equals(traceId)).findFirst().orElse(null);
    }

    // ===================================================================================================
    // PLog data
    // ===================================================================================================
    public static PTrace findCaseById(String caseId, PLog pLog) {
        return pLog.getPTraces().stream()
                .filter(x -> x.getCaseId().equals(caseId))
                .findFirst()
                .orElse(null);
    }

    public static List<PTrace> getValidTraces(PLog pLog) {
        BitSet caseIndexBS = pLog.getValidTraceIndexBS();
        return pLog.getOriginalPTraces().stream()
                .filter(x -> caseIndexBS.get(x.getImmutableIndex()))
                .collect(Collectors.toList());
    }

    public static List<ActivityInstance> getValidActivitiesOf(PTrace trace) {
        BitSet validEventBS = trace.getValidEventIndexBS();
        return trace.getOriginalActivityInstances().stream()
                .filter(x -> validEventBS.get(x.getFirstEventIndex()))
                .collect(Collectors.toList());
    }


    public static long getUniqueEventAttributeValueSize(String key, PLog pLog) {
        if (!pLog.getImmutableLog().getImmutableEventAttributeValues().containsKey(key)) return 0;
        UnifiedSet<ActivityInstance> existActs =
                pLog.getActivityInstances().stream().collect(Collectors.toCollection(UnifiedSet::new));

        UnifiedSet<EventAttributeValue> eavSet =
                pLog.getImmutableLog().getImmutableEventAttributeValues().get(key).stream()
                .filter(x -> x.getOccurActivities(existActs).size() > 0)
                .collect(Collectors.toCollection(UnifiedSet::new));

        return eavSet.size();
    }

    public static long getUniqueCaseAttributeValueSize(String key, PLog pLog) {
        if (!pLog.getImmutableLog().getImmutableCaseAttributeValues().containsKey(key)) return 0;

        BitSet validTraceIndexes = pLog.getValidTraceIndexBS();

        UnifiedSet<CaseAttributeValue> cavSet =
                pLog.getImmutableLog().getImmutableCaseAttributeValues().get(key).stream()
                        .filter(x -> x.getCases(validTraceIndexes) > 0)
                        .collect(Collectors.toCollection(UnifiedSet::new));

        return cavSet.size();
    }

    public static Map<String, Number> getEventAttributeValueCaseFrequencies(String key, PLog pLog) {

        UnifiedSet<ActivityInstance> existActs =
                pLog.getActivityInstances().stream().collect(Collectors.toCollection(UnifiedSet::new));

        UnifiedSet<EventAttributeValue> eavSet = pLog.getImmutableEventAttributeValues().get(key);

        return eavSet.stream()
                .filter(x -> x.getOccurActivities(existActs).size() > 0)
                .collect(Collectors.toMap(x -> x.getValue(), x -> getCases(x.getOccurActivities(existActs), pLog)));
    }

    private static long getCases(UnifiedSet<ActivityInstance> validActs, PLog pLog) {
        return validActs.stream()
                .filter(x -> pLog.getValidTraceIndexBS().get(x.getImmutableTraceIndex()))
                .filter(x -> pLog.getPTracesMap().containsKey(x.getParentTraceId()))
                .map(x -> pLog.getPTracesMap().get(x.getParentTraceId()))
                .collect(Collectors.toSet()).size();
    }

    public static Map<String, Number> getEventAttributeValueTotalFrequencies(String key, PLog pLog) {

        UnifiedSet<ActivityInstance> existActs =
                pLog.getActivityInstances().stream().collect(Collectors.toCollection(UnifiedSet::new));

        UnifiedSet<EventAttributeValue> eavSet = pLog.getImmutableEventAttributeValues().get(key);

        return eavSet.stream()
                .filter(x -> x.getOccurActivities(existActs).size() > 0)
                .collect(Collectors.toMap(x -> x.getValue(), x -> x.getOccurActivities(existActs).size()));
    }

    public static Map<String, List<CustomTriple>> getTargetNodeDataBySourceNode(String key,
                                                                                String sourceNode,
                                                                                PLog pLog,
                                                                                UnifiedSet<EventAttributeValue> set) {

        EventAttributeValue sourceEav = set.stream()
                .filter(x -> x.getValue().equals(sourceNode))
                .findFirst()
                .orElse(null);

        if (sourceEav == null) return null;

        // ================================
        // target activity, source activity
        // ================================
        Map<ActivityInstance, ActivityInstance> validFollows = getValidNextActivities(key, sourceEav, pLog);

        List<CustomTriple> triples = new ArrayList<>(validFollows.size());
        for (Map.Entry<ActivityInstance, ActivityInstance> pair : validFollows.entrySet()) {
            // ===========================================================
            // target activity, source activity, target attribute value
            // ===========================================================
            triples.add(CustomTriple.of(pair.getKey(), pair.getValue(), pair.getValue().getAttributes().get(key)));
        }

        return triples.stream().collect(Collectors.groupingBy(CustomTriple::getValue));
    }

    private static Map<ActivityInstance, ActivityInstance> getValidNextActivities(String key,
                                                                                  EventAttributeValue v,
                                                                                  PLog pLog) {
        UnifiedSet<ActivityInstance> existActs =
                pLog.getActivityInstances().stream().collect(Collectors.toCollection(UnifiedSet::new));

        return v.getOccurActivities(existActs).stream()
                .filter(x -> existActs.contains(x))
                .filter(x -> pLog.getValidTraceIndexBS().get(x.getImmutableTraceIndex()))
                .filter(x -> pLog.getPTracesMap().get(x.getParentTraceId()).size() > 1)
                .filter(x -> x != pLog.getPTracesMap().get(x.getParentTraceId()).getLast())
                .filter(x -> pLog.getPTracesMap().get(x.getParentTraceId()).getNextOf(x) != null)
                .filter(x -> pLog.getPTracesMap().get(x.getParentTraceId()).getNextOf(x).getAttributes().containsKey(key))
                .collect(Collectors.toMap(x -> x, x -> pLog.getPTracesMap().get(x.getParentTraceId()).getNextOf(x)));
    }
}
