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
package org.apromore.apmlog.immutable;


import org.apromore.apmlog.*;
import org.apromore.apmlog.filter.PLog;
import org.apromore.apmlog.filter.PTrace;
import org.apromore.apmlog.stats.CaseAttributeValue;
import org.apromore.apmlog.stats.EventAttributeValue;
import org.apromore.apmlog.stats.StatsUtil;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImmutableLog extends LaLog {

    public ImmutableLog() {
        traceList.clear();
        immutableTraces.clear();
        eventAttributeOccurMap = new UnifiedMap<>();
        activityNameBiMap = new HashBiMap<>();
        caseAttributeValues = new UnifiedMap<>();
        eventAttributeValues = new UnifiedMap<>();
        variantIdFreqMap = new UnifiedMap<>();
    }

    public ImmutableLog(List<ATrace> traceList) {
        setTraces(traceList);
        setImmutableTraces(traceList);
        eventAttributeOccurMap = new UnifiedMap<>();
        activityNameBiMap = new HashBiMap<>();
        caseAttributeValues = new UnifiedMap<>();
        eventAttributeValues = new UnifiedMap<>();
        variantIdFreqMap = new UnifiedMap<>();
    }


    @Override
    public APMLog clone() {

        List<ATrace> traceListForClone = new ArrayList<>();

        for (ATrace aTrace : this.traceList) {
            ATrace clone = aTrace.clone();
            traceListForClone.add(clone);
        }

        UnifiedMap<Integer, Integer> variIdFreqMapForClone = new UnifiedMap<>();

        for (int key : this.variantIdFreqMap.keySet()) {
            variIdFreqMapForClone.put(key, this.variantIdFreqMap.get(key));
        }

        UnifiedMap<String, UnifiedSet<EventAttributeValue>> eventAttrValsClone = new UnifiedMap<>();

        for (String key : eventAttributeValues.keySet()) {
            UnifiedSet<EventAttributeValue> valSet = eventAttributeValues.get(key);
            UnifiedSet<EventAttributeValue> valSetClone = new UnifiedSet<>(valSet.size());
            for (EventAttributeValue eav : valSet) {
                valSetClone.add(eav.clone());
            }
            eventAttrValsClone.put(key, valSetClone);
        }

        UnifiedMap<String, UnifiedSet<CaseAttributeValue>> caseAttrValsClone = new UnifiedMap<>();

        for (String key : caseAttributeValues.keySet()) {
            UnifiedSet<CaseAttributeValue> valSet = caseAttributeValues.get(key);
            UnifiedSet<CaseAttributeValue> valSetClone = new UnifiedSet<>(valSet.size());
            for (CaseAttributeValue cav : valSet) {
                valSetClone.add(cav.clone());
            }
            caseAttrValsClone.put(key, valSetClone);
        }

        UnifiedMap<String, Integer> activityMaxOccurMapForClone = new UnifiedMap<>();

        for (String key : this.activityMaxOccurMap.keySet()) {
            activityMaxOccurMapForClone.put(key, this.activityMaxOccurMap.get(key));
        }

        DoubleArrayList caseDurationListClone = new DoubleArrayList(caseDurationList.toArray());


        ImmutableLog logClone = new ImmutableLog(traceListForClone,
                variIdFreqMapForClone,
                eventAttrValsClone,
                caseAttrValsClone,
                caseDurationListClone,
                this.timeZone,
                this.startTime,
                this.endTime,
                this.eventSize,
                this.activityNameMapper,
                activityMaxOccurMapForClone);
        logClone.setEventAttributeOccurMap(new UnifiedMap<>(this.eventAttributeOccurMap));
        logClone.setActivityNameBiMap(new HashBiMap<>(this.activityNameBiMap));

        return logClone;
    }

    public ImmutableLog(List<ATrace> traceList,
                        UnifiedMap<Integer, Integer> variantIdFreqMap,
                        UnifiedMap<String, UnifiedSet<EventAttributeValue>> eventAttributeValues,
                        UnifiedMap<String, UnifiedSet<CaseAttributeValue>> caseAttributeValues,
                        DoubleArrayList caseDurationList,
                        String timeZone,
                        long startTime,
                        long endTime,
                        long eventSize,
                        ActivityNameMapper activityNameMapper,
                        UnifiedMap<String, Integer> activityMaxOccurMap) {
        setImmutableTraces(traceList);
        setTraces(traceList);
        this.variantIdFreqMap = variantIdFreqMap;
        this.eventAttributeValues = eventAttributeValues;
        this.caseAttributeValues = caseAttributeValues;
        this.caseDurationList = caseDurationList;
        this.timeZone = timeZone;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventSize = eventSize;
        this.activityNameMapper = activityNameMapper;
        this.activityMaxOccurMap = activityMaxOccurMap;
        if (traceList.size() > 0) {
            if (traceList.get(0).getDuration() > 0) {
                defaultChartDataCollection = new DefaultChartDataCollection(this);
            }
        }
    }

    public ImmutableLog(PLog pLog) {
        List<ATrace> traces = pLog.getPTraceList().stream()
                .map(PTrace::toATrace)
                .collect(Collectors.toList());

//        List<ATrace> traces = pLog.getPTraceList().stream().collect(Collectors.toList());

        setImmutableTraces(traces);
        setTraces(traces);

        // =====================================================
        // PLog does not update its start time and end time.
        // Such values need to be reproduced.
        // =====================================================
        LongSummaryStatistics allST = traces.stream()
                .collect(Collectors.summarizingLong(ATrace::getStartTimeMilli));

        LongSummaryStatistics allET = traces.stream()
                .collect(Collectors.summarizingLong(ATrace::getEndTimeMilli));

        this.variantIdFreqMap = pLog.getVariantIdFreqMap();

        this.eventAttributeValues = StatsUtil.getEventAttributeValues(traces);
        this.caseAttributeValues = StatsUtil.getCaseAttributeValues(traces);


        this.caseDurationList = pLog.getCaseDurations();
        this.timeZone = pLog.getTimeZone();
        this.startTime = allST.getMin();
        this.endTime = allET.getMax();
        this.eventSize = pLog.getEventSize();
        this.activityNameMapper = pLog.getActivityNameMapper();
        this.activityMaxOccurMap = pLog.getActivityMaxOccurMap();

        if (traceList.size() > 0 && traceList.get(0).getDuration() > 0) {
            defaultChartDataCollection = new DefaultChartDataCollection(this);
        }

        updateCaseVariants();
    }

    public void updateLocalStats() {
        startTime = 0;
        endTime = 0;

        actNameIdxCId = new HashBiMap<>();

        variantIdFreqMap = new UnifiedMap<>();

        List<IntArrayList> traceActNameIndexes = new ArrayList<>();

        eventSize = 0;

        variantIdFreqMap = new UnifiedMap<>();

        this.caseDurationList = new DoubleArrayList(traceList.size());

        UnifiedMap<String, UnifiedMap<String, IntArrayList>> caseAttrValOccurMap = new UnifiedMap<>();

        int index = 0;

        for (ATrace trace : traceList) {

            UnifiedMap<String, String> tAttrMap = trace.getAttributeMap();

            for (String attrKey : tAttrMap.keySet()) {
                String val = trace.getAttributeMap().get(attrKey);

                if (caseAttrValOccurMap.keySet().contains(attrKey)) {
                    UnifiedMap<String, IntArrayList> valOccurMap = caseAttrValOccurMap.get(attrKey);
                    if (valOccurMap.containsKey(val)) {
                        valOccurMap.get(val).add(index);
                    } else {
                        IntArrayList indexes = new IntArrayList();
                        indexes.add(index);
                        valOccurMap.put(val, indexes);
                    }
                } else {
                    IntArrayList indexes = new IntArrayList();
                    indexes.add(index);
                    UnifiedMap<String, IntArrayList> valOccurMap = new UnifiedMap<>();
                    valOccurMap.put(val, indexes);
                    caseAttrValOccurMap.put(attrKey, valOccurMap);
                }
            }

            caseDurationList.add(trace.getDuration());

//            trace.setMutableIndex(index);

            int vari = trace.getCaseVariantId();

            if (variantIdFreqMap.containsKey(vari)) {
                int freq = variantIdFreqMap.get(vari) + 1;
                variantIdFreqMap.put(vari, freq);
            } else variantIdFreqMap.put(vari, 1);

            IntArrayList actNameIndexes = getActivityNameIndexes(trace);
            traceActNameIndexes.add(actNameIndexes);

            if (startTime < 1 || trace.getStartTimeMilli() < startTime) startTime = trace.getStartTimeMilli();
            if (trace.getEndTimeMilli() > endTime) endTime = trace.getEndTimeMilli();

            eventSize += trace.getEventSize();

            index += 1;
        }

        caseAttributeValues = new UnifiedMap<>();

        for (String attrKey : caseAttrValOccurMap.keySet()) {
            UnifiedMap<String, IntArrayList> valOccurMap = caseAttrValOccurMap.get(attrKey);
            UnifiedSet<CaseAttributeValue> cavSet = new UnifiedSet<>();

            int[] arr = valOccurMap.entrySet().stream().mapToInt(x -> x.getValue().size()).toArray();
            IntArrayList ial = new IntArrayList(arr);

            int maxOccurSize = ial.max();

            for (String val : valOccurMap.keySet()) {
                CaseAttributeValue cav = new CaseAttributeValue(val, valOccurMap.get(val), traceList.size());
                cav.setRatio(100 * ( (double) cav.getCases() / maxOccurSize));
                cavSet.add(cav);
            }
            caseAttributeValues.put(attrKey, cavSet);
        }

        updateEventAttributeOccurMap();

        eventAttributeValues = new UnifiedMap<>();

        for (String key : eventAttributeOccurMap.keySet()) {  // !!!!! performance issue
            UnifiedMap<String, UnifiedSet<AActivity>> valOccurMap = eventAttributeOccurMap.get(key);

            UnifiedMap<String, Integer> valCaseFreqMap = new UnifiedMap<>(valOccurMap.size());

            UnifiedSet<EventAttributeValue> attrVals = new UnifiedSet<>();
            int maxCasesOfCSEventAttrVal = 0;


            for (String val : valOccurMap.keySet()) {

                UnifiedSet<AActivity> occurSet = valOccurMap.get(val);

                int[] array = occurSet.stream().mapToInt(s -> s.getMutableTraceIndex()).toArray();
                List<Integer> traceIndexList = IntStream.of(array).boxed().collect(Collectors.toList());
                UnifiedSet<Integer> uniqueTraceIndexes = new UnifiedSet<>(traceIndexList);
                List<Integer> uniqueTraceIndexList = new ArrayList<>(uniqueTraceIndexes);

                int[] array2 = uniqueTraceIndexList.stream().mapToInt(s -> s).toArray();

                IntArrayList traceIndexes = new IntArrayList(array2);

                valCaseFreqMap.put(val, traceIndexes.size());

                try {
                    attrVals.add(new EventAttributeValue(val, traceIndexes, traceList.size(), occurSet));
                } catch (Exception e) {
                    System.out.println("");
                }
                if (traceIndexes.size() > maxCasesOfCSEventAttrVal) maxCasesOfCSEventAttrVal = traceIndexes.size();
            }

            for (EventAttributeValue v : attrVals) {
                v.setRatio(100 * ( (double) v.getCases() / maxCasesOfCSEventAttrVal));
            }
            eventAttributeValues.put(key, attrVals);
        }
    }

    private void updateEventAttributeOccurMap() {
        eventAttributeOccurMap = new UnifiedMap<>();

        for (ATrace trace: traceList) {
            for (AActivity activity: trace.getActivityList()) {
                LogFactory.fillAttributeOccurMap(activity, eventAttributeOccurMap);
            }
        }

        Set<String> tobeRemoved = eventAttributeOccurMap.entrySet().stream()
                .filter(x -> x.getValue().size() > 1000)
                .map(x -> x.getKey())
                .collect(Collectors.toSet());

        eventAttributeOccurMap.keySet().removeAll(tobeRemoved);

    }
}
