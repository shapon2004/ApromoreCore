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
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.Serializable;
import java.util.BitSet;
import java.util.List;
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
    }


    public Set<Integer> getOccurCasesIndexSet() {
        if (occurCasesIndexSet == null) {
            occurCasesIndexSet = occurActivities.stream()
                    .map(ActivityInstance::getImmutableTraceIndex)
                    .collect(Collectors.toSet());
        }
        return occurCasesIndexSet;
    }

    public String getValue() {
        return value;
    }

    public long getCases() {
        return occurCasesIndexSet.size();
    }

    public long getCases(BitSet validCaseIndexes) {
        return occurCasesIndexSet.stream().filter(x -> validCaseIndexes.get(x)).collect(Collectors.toSet()).size();
    }

    public String getFrequency() {
        return frequency;
    }

    public long getTotal() {
        return total;

    }

    public double getRatio() {
        return ratio;
    }

    public double getOppCases() {
        return oppCases;
    }

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

    @Override
    public double getValueInDouble() {
        if (!Util.isNumeric(value)) return -1;
        else return Double.valueOf(value);
    }

    public EventAttributeValue clone() {
        return new EventAttributeValue(
                value, new UnifiedSet<>(occurActivities), totalCases );
    }
}
