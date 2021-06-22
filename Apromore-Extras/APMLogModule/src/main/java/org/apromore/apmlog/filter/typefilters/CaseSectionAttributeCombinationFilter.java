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
package org.apromore.apmlog.filter.typefilters;

import org.apromore.apmlog.logobjects.ActivityInstance;
import org.apromore.apmlog.filter.PTrace;
import org.apromore.apmlog.filter.rules.LogFilterRule;
import org.apromore.apmlog.filter.rules.RuleValue;
import org.apromore.apmlog.filter.types.Choice;
import org.apromore.apmlog.filter.types.Inclusion;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.stream.Collectors;

public class CaseSectionAttributeCombinationFilter {
    public static boolean toKeep(PTrace trace, LogFilterRule logFilterRule) {
        Choice choice = logFilterRule.getChoice();
        switch (choice) {
            case RETAIN: return conformRule(trace, logFilterRule);
            default: return !conformRule(trace, logFilterRule);
        }
    }

    private static boolean conformRule(PTrace trace, LogFilterRule logFilterRule) {

        Set<String> primaryValues = logFilterRule.getPrimaryValuesInString();
        Set<RuleValue> secoRV = logFilterRule.getSecondaryValues();

        if (secoRV == null || secoRV.isEmpty()) return false;

        Set<String> secondaryValues = (Set<String>) secoRV.iterator().next().getObjectVal();

        String firstKey = logFilterRule.getPrimaryValues().iterator().next().getKey();
        String secondKey = secoRV.iterator().next().getKey();

        String sect1 = logFilterRule.getPrimaryValues().iterator().next().getCustomAttributes().get("section");
        String sect2 = secoRV.iterator().next().getCustomAttributes().get("section");

        Inclusion inclusion = logFilterRule.getInclusion();

        if (sect1.equals("case") && sect2.equals("case")) {
            return conformCaseToCaseAttrValue(trace, firstKey, secondKey, primaryValues, secondaryValues, inclusion);
        } else if (sect1.equals("case") && sect2.equals("event")) {
            return conformCaseToEventAttrValue(trace, firstKey, secondKey, primaryValues, secondaryValues, inclusion);
        } else if (sect1.equals("event") && sect2.equals("event")) {
            return confirmEventToEventAttrValues(trace, firstKey, secondKey, primaryValues, secondaryValues, inclusion);
        } else if (sect1.equals("event") && sect2.equals("case")) {
            return confirmEventToCaseAttrValues(trace, firstKey, secondKey, primaryValues, secondaryValues, inclusion);
        }

        return false;
    }

    private static boolean conformCaseToCaseAttrValue(PTrace trace, String firstKey, String secondKey,
                                                      Set<String> primaryValues, Set<String> secondaryValues,
                                                      Inclusion inclusion) {
        UnifiedMap<String, String> caseAttrMap = trace.getAttributes();
        if (!caseAttrMap.containsKey(firstKey)) return false;
        if (!caseAttrMap.containsKey(secondKey)) return false;
        else {
            if (!primaryValues.contains(caseAttrMap.get(firstKey))) return false;
            else {
                switch (inclusion) {
                    case ALL_VALUES:
                        Set<String> matchedValues = new HashSet<>();

                        for (String caseAttrKey : caseAttrMap.keySet()) {
                            if (secondaryValues.contains(caseAttrMap.get(caseAttrKey))) {
                                matchedValues.add(caseAttrMap.get(caseAttrKey));
                            }
                        }

                        return matchedValues.size() == secondaryValues.size();
                    case ANY_VALUE:
                        for (String caseAttrKey : caseAttrMap.keySet()) {
                            if (secondaryValues.contains(caseAttrMap.get(caseAttrKey))) {
                                return true;
                            }
                        }
                }
            }
        }
        return false;
    }

    private static boolean conformCaseToEventAttrValue(PTrace trace, String firstKey, String secondKey,
                                                       Set<String> primaryValues, Set<String> secondaryValues,
                                                       Inclusion inclusion) {
        UnifiedMap<String, String> caseAttrMap = trace.getAttributes();
        if (!caseAttrMap.containsKey(firstKey)) return false;
        else {
            String primVal = primaryValues.iterator().next();
            if (!caseAttrMap.get(firstKey).equals(primVal)) return false;

            Map<String, List<ActivityInstance>> grouped = trace.getActivityInstances().stream()
                    .filter(x -> x.getAttributes().containsKey(secondKey) &&
                            secondaryValues.contains(x.getAttributes().get(secondKey)))
                    .collect(Collectors.groupingBy(x -> x.getAttributes().get(secondKey)));

            LongSummaryStatistics valActSizes = grouped.entrySet().stream()
                    .collect(Collectors.summarizingLong(x -> x.getValue().size()));

            if (inclusion == Inclusion.ALL_VALUES) {
                return valActSizes.getMin() > 0 && valActSizes.getCount() == secondaryValues.size();
            } else return valActSizes.getMax() > 0;

        }
    }

    private static String getConfirmedActivityAttrValue(ActivityInstance activity, String attrKey, Set<String> values) {

        if (!activity.getAttributes().containsKey(attrKey)) return null;

        return values.contains(activity.getAttributes().get(attrKey)) ? activity.getAttributes().get(attrKey) : null;
    }

    private static boolean confirmEventToEventAttrValues(PTrace trace, String firstKey, String secondKey,
                                                         Set<String> primaryValues, Set<String> secondaryValues,
                                                         Inclusion inclusion) {
        List<ActivityInstance> activityList = trace.getActivityInstances();

        ActivityInstance priAct = activityList.stream()
                .filter(x -> x.getAttributes().containsKey(firstKey))
                .filter(x -> x.getAttributes().get(firstKey).equals(primaryValues.iterator().next()))
                .findFirst()
                .orElse(null);

        if (priAct == null) return false;

        switch (inclusion) {
            case ANY_VALUE:
                ActivityInstance ai = activityList.stream()
                        .filter(x -> x.getAttributes().containsKey(firstKey))
                        .filter(x -> x.getAttributes().get(firstKey).equals(primaryValues.iterator().next()))
                        .filter(x -> x.getAttributes().containsKey(secondKey))
                        .filter(x -> secondaryValues.contains(x.getAttributes().get(secondKey)))
                        .findFirst()
                        .orElse(null);

                return ai != null;
            case ALL_VALUES:
                Set<ActivityInstance> matchedVal = activityList.stream()
                        .filter(x -> x.getAttributes().containsKey(firstKey))
                        .filter(x -> x.getAttributes().get(firstKey).equals(primaryValues.iterator().next()))
                        .filter(x -> x.getAttributes().containsKey(secondKey))
                        .filter(x -> secondaryValues.contains(x.getAttributes().get(secondKey)))
                        .collect(Collectors.toSet());

//                if (matchedVal.size() >= secondaryValues.size()) {
//                    System.out.println("");
//                }

                return matchedVal.size() >= secondaryValues.size();

//                for (ActivityInstance act : activityList) {
////                    XEvent event0 = trace.getImmutableEvents().get(act.getImmutableEventIndexes().get(0));
//                    String confirmedVal =
//                            getConformedEventAttrValue(act, firstKey, secondKey, primaryValues, secondaryValues);
//                    if (confirmedVal != null) return true;
//                }
//                break;
        }

        return false;
    }

    private static String getConformedEventAttrValue(ActivityInstance event, String firstKey, String secondKey,
                                                     Set<String> primaryValues, Set<String> secondaryValues) {

        if (!event.getAttributes().containsKey(firstKey)) return null;

        String val = event.getAttributes().get(firstKey).toString();

        if (!primaryValues.contains(val)) return null;

        return conformEventAttributeKeyValue(event, secondKey, secondaryValues);
    }

    private static String conformCaseAttributeKeyValue(PTrace trace, String attributeKey, Set<String> values) {
        UnifiedMap<String, String> attrMap = trace.getAttributes();
        if (!attrMap.containsKey(attributeKey)) return null;
        return values.contains(attrMap.get(attributeKey)) ? attrMap.get(attributeKey) : null;
    }

    private static String conformEventAttributeKeyValue(ActivityInstance activityInstance,
                                                        String attributeKey, Set<String> values) {

        if (!activityInstance.getAttributes().containsKey(attributeKey)) return null;

        String val = activityInstance.getAttributes().get(attributeKey).toString();

        return values.contains(val) ? val : null;
    }

    private static boolean confirmEventToCaseAttrValues(PTrace trace, String firstKey, String secondKey,
                                                        Set<String> primaryValues, Set<String> secondaryValues,
                                                        Inclusion inclusion) {
        UnifiedMap<String, String> caseAttrMap = trace.getAttributes();

        List<ActivityInstance> activityList = trace.getActivityInstances();

        switch (inclusion) {
            case ALL_VALUES:
                UnifiedSet<String> matchedVals = new UnifiedSet<>();

                for (ActivityInstance act : activityList) {
//                    XEvent event0 = trace.getImmutableEvents().get(act.getImmutableEventIndexes().get(0));
                    String attrVal = getEventAttributeValue(act, firstKey);
                    if (attrVal != null && primaryValues.contains(attrVal)) {
                        String attrVal2 = caseAttrMap.get(secondKey);
                        if (attrVal2 != null) {
                            if (secondaryValues.contains(attrVal2)) {
                                matchedVals.add(attrVal2);
                            }
                        }
                    }
                }

                return matchedVals.size() == secondaryValues.size();
            case ANY_VALUE:
                for (ActivityInstance act : activityList) {
//                    XEvent event0 = trace.getImmutableEvents().get(act.getImmutableEventIndexes().get(0));
                    String attrVal = getEventAttributeValue(act, firstKey);
                    if (attrVal != null) {
                        if (primaryValues.contains(attrVal)) {
                            String attrVal2 = caseAttrMap.get(secondKey);
                            if (attrVal2 != null) {
                                if (secondaryValues.contains(attrVal2)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                break;
        }

        return false;
    }

    private static String getEventAttributeValue(ActivityInstance activityInstance, String key) {
        if (!activityInstance.getAttributes().containsKey(key)) return null;

        return activityInstance.getAttributes().containsKey(key) ? activityInstance.getAttributes().get(key).toString() : null;
    }
}
