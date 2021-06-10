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

import org.apromore.apmlog.logobjects.ActivityInstance;
import org.apromore.apmlog.util.Util;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EventAttributeValue implements AttributeValue, Serializable {
    private String value;

    private double valueFrequency;

    // the percentage of cases
    private String frequency;

    // ratio is: the cases of this value / the max cases among all attribute values
    private double ratio;

    // the number of events that contain this value
    private long total;

    private long totalCases;

    // for default sorting
    private double oppCases;
    private double oppActivitySize;

    private double percent; // case section

//    private IntArrayList occurCaseIndexes;
    private UnifiedSet<ActivityInstance> occurActivities;
    private Set<Integer> occurCasesIndexSet;

    public EventAttributeValue(String value, UnifiedSet<ActivityInstance> occurActivities, long totalCasesOfLog) {
        this.value = value;
        this.occurActivities = occurActivities;
        occurCasesIndexSet = occurActivities.stream()
                .map(ActivityInstance::getImmutableTraceIndex)
                .collect(Collectors.toSet());
        this.totalCases = totalCasesOfLog;
        this.percent = 100 * ((double) occurCasesIndexSet.size() / totalCases);
        this.frequency = String.format("%.2f", percent );
        this.oppCases = totalCasesOfLog - occurCasesIndexSet.size();
        this.total = occurActivities.stream().collect(Collectors.summingLong(ActivityInstance::getEventSize));
        int[] array = occurCasesIndexSet.stream().mapToInt(x->x).toArray();
//        occurCaseIndexes = new IntArrayList(array);
    }

//    public EventAttributeValue(String value, IntArrayList occurCaseIndexes, long totalCases,
//                               UnifiedSet<ActivityInstance> occurActivities) {
//        this.value = value.intern();
//        this.occurCaseIndexes = occurCaseIndexes;
////        this.total = total;
//        this.percent = 100 * ((double) occurCaseIndexes.size() / totalCases);
//        this.frequency = String.format("%.2f", percent );
//        this.totalCases = totalCases;
//        this.oppCases = totalCases - occurCaseIndexes.size();
//        this.occurActivities = occurActivities;
//
//        long sum = 0;
//        for (ActivityInstance act : occurActivities) {
//            sum += act.getEventSize();
//        }
//        this.total = sum;
//    }

    public Set<Integer> getOccurCasesIndexSet() {
        if (occurCasesIndexSet == null) {
            occurCasesIndexSet = occurActivities.stream()
                    .map(ActivityInstance::getImmutableTraceIndex)
                    .collect(Collectors.toSet());
        }
        return occurCasesIndexSet;
    }

//    public void setRatio(double ratio) {
//        this.ratio = ratio;
//    }

//    public void setTotal(long total) {
//
//        this.total = total;
//    }

    public String getValue() {
        return value;
    }

    public long getCases() {
        return occurCasesIndexSet.size();
    }

    public long getCases(BitSet validCaseIndexes) {
        return occurCasesIndexSet.stream().filter(x -> validCaseIndexes.get(x)).collect(Collectors.toSet()).size();
//        return getOccurCasesIndexSet().size();
    }

    public String getFrequency() {
        return frequency;
    }

    public long getTotal() {
        return total;

    }

//    public void setTotalCases(long totalCases) {
//        this.totalCases = totalCases;
//    }

    public double getRatio() {
        return ratio;
    }

    public double getOppCases() {
        return oppCases;
    }

//    public IntArrayList getOccurCaseIndexes() {
//        if (occurCaseIndexes == null) {
//            int[] array = occurCasesIndexSet.stream().mapToInt(x -> x).toArray();
//            occurCaseIndexes =new IntArrayList(array);
//        }
//        return occurCaseIndexes;
//    }

    public double getPercent() {
        return percent;
    }

    public UnifiedSet<ActivityInstance> getOccurActivities(UnifiedSet<ActivityInstance> validActivities) {
        return occurActivities.stream().filter(x -> validActivities.contains(x))
                .collect(Collectors.toCollection(UnifiedSet::new));
    }

    public int getActivitySize(UnifiedSet<ActivityInstance> validActivities) {
        return occurActivities.stream().filter(x -> validActivities.contains(x)).collect(Collectors.toList()).size();
    }

    public double getTotalDuration(UnifiedSet<ActivityInstance> validActivities) {
        double[] array = occurActivities.stream()
                .filter(x -> validActivities.contains(x))
                .mapToDouble(s -> s.getDuration())
                .toArray();
        DoubleArrayList dal = new DoubleArrayList(array);
        return dal.sum();
    }

    public Set<Double> getUniqueDurations(UnifiedSet<ActivityInstance> validActivities) {
        return occurActivities.stream()
                .filter(x -> validActivities.contains(x))
                .map(s -> s.getDuration()).collect(Collectors.toSet());
    }

    public DoubleArrayList getAllDurations(UnifiedSet<ActivityInstance> validActivities) {
        double[] array = occurActivities.stream()
                .filter(x -> validActivities.contains(x))
                .mapToDouble(s -> s.getDuration()).toArray();
        return new DoubleArrayList(array);
    }

    public double getInterCasesDoubleValue(BitSet validCaseIndexes) {
        return getValueInDouble() * getCases(validCaseIndexes);
    }

    public double getInterCasesDoubleValue() {
        return getValueInDouble() * getCases();
    }

//    // ========================================================
//    // Set by GUI; used by GUI
//    // ========================================================
//    public void setValueFrequency(double valueFrequency) {
//        this.valueFrequency = valueFrequency;
//    }
//
//    // ========================================================
//    // Set by GUI; used by GUI
//    // ========================================================
//    public double getValueFrequency() {
//        return valueFrequency;
//    }
//
//    // ========================================================
//    // Set by GUI; used by GUI
//    // ========================================================
//    public String getValueFrequencyString() {
//        return Util.df2.format(100 * valueFrequency) + "%";
//    }
//
//    // ========================================================
//    // Set by GUI; used by GUI
//    // ========================================================
//    public void setOppActivitySize(double oppActivitySize) {
//        this.oppActivitySize = oppActivitySize;
//    }
//
//    // ========================================================
//    // Set by GUI; used by GUI
//    // ========================================================
//    public double getOppActivitySize() {
//        return oppActivitySize;
//    }

    @Override
    public double getValueInDouble() {
        if (!Util.isNumeric(value)) return -1;
        else return Double.valueOf(value);
    }

    public EventAttributeValue clone() {
//        IntArrayList occurCaseIndexesClone = new IntArrayList(occurCaseIndexes.size());
//        for (int i = 0; i < occurCaseIndexes.size(); i++) {
//            occurCaseIndexesClone.add(occurCaseIndexes.get(i));
//        }

        return new EventAttributeValue(
                value, new UnifiedSet<>(occurActivities), totalCases );
    }

//    public JSONObject toJSONObject() {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("value", value);
//        jsonObject.put("ratio", ratio);
//        jsonObject.put("total", total);
//        jsonObject.put("totalCases", totalCases);
//        jsonObject.put("oppCases", oppCases);
//        jsonObject.put("percent", percent);
//
//        JSONArray jsonOccurCaseIndexes = new JSONArray();
//        for (int i = 0; i < occurCaseIndexes.size(); i++) {
//            jsonOccurCaseIndexes.add(occurCaseIndexes.get(i));
//        }
//
//        jsonObject.put("occurCaseIndexes", jsonOccurCaseIndexes);
//
//
//    }

    //private String value;
    //
    //    // the percentage of cases
    //    private String frequency;
    //
    //    // ratio is: the cases of this value / the max cases among all attribute values
    //    private double ratio;
    //
    //    // the number of events that contain this value
    //    private long total;
    //
    //    private long totalCases;
    //
    //    // for default sorting
    //    private double oppCases;
    //
    //    private double percent; // case section
    //
    //    private IntArrayList occurCaseIndexes;
    //    private UnifiedSet<AActivity> occurActivities;
    //    private Set<Integer> occurCasesIndexSet;
}
