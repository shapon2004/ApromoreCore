/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2015 - 2017 Queensland University of Technology.
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

package org.apromore.service.loganimation2.enablement;

import lombok.NonNull;
import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.logman.attribute.log.AttributeLog;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>LogEnablement</b> contains enablement data obtained for one log.
 * Enablement includes alignment of each trace in AttributeLog with a BPMN diagram augmented with enablement
 * timestamp for each diagram element. Each enablement for one case is a set of {@link EnablementTuple}.
 */
public class LogEnablement {
    private final CaseIdIndexing caseIdIndexing; // mapping caseID to case index for space efficiency
    private final Map<String, List<EnablementResult>> enablements;
    private long startTimestamp = Long.MAX_VALUE;
    private long endTimestamp = Long.MIN_VALUE;
    private final String logName;

    public LogEnablement(@NonNull AttributeLog attLog, @NonNull Map<String, List<EnablementResult>> enablements) {
        this.caseIdIndexing = new CaseIdIndexing(attLog);
        this.enablements = enablements;
        attLog.getTraces().forEach(trace -> {
           if (trace.getStartTimeAtIndex(0) < startTimestamp) {
               startTimestamp = trace.getStartTimeAtIndex(0);
           }
           if (trace.getStartTimeAtIndex(trace.getValueTrace().size()-1) > endTimestamp) {
               endTimestamp = trace.getEndTimeAtIndex(trace.getValueTrace().size() - 1);
           }
        });
        this.logName = attLog.getName();
    }

    public String getName() {
        return this.logName;
    }

    public int size() {
        return enablements.size();
    }

    public Set<String> getCaseIDs() {
        return enablements.keySet();
    }

    /**
     * Return a list of {@link EnablementTuple} for a case ID. EnablementTuple is used here
     * instead of {@link EnablementResult} to stop deeper dependency {@link EnablementResult} of the AlignmentClient library
     * as it is under a different cycle of changes.
     * @param caseId
     * @return
     */
    public List<EnablementTuple> getEnablementsByCaseId(String caseId) {
        return enablements.getOrDefault(caseId, Collections.emptyList())
                .stream()
                .map(e -> new EnablementTuple(e.getElementId(),
                            e.isSkipped(), e.isSequenceFlow(),
                            e.getEnablementTimestamp() == null ? 0 : e.getEnablementTimestamp(),
                            e.getEndTimestamp() == null ? 0 : e.getEndTimestamp()))
                .collect(Collectors.toList());
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public long getEndTimestamp() {
        return this.endTimestamp;
    }

    public int getCaseIndexFromId(String caseId) {
        return caseIdIndexing.getIndex(caseId);
    }
}
