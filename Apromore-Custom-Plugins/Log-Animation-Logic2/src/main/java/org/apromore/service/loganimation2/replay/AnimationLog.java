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

package org.apromore.service.loganimation2.replay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apromore.service.loganimation2.utils.TimeUtilities;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import de.hpi.bpmn2_0.model.connector.SequenceFlow;

public class AnimationLog {
    private XLog xlog = null;
    private Map<XTrace, ReplayTrace> traceMap = new HashMap();
    private String name = "";
    private String fileName = "";
    private DateTime startDate = null;
    private DateTime endDate = null;
    private static final Logger LOGGER = Logger.getLogger(AnimationLog.class.getCanonicalName());
    private CaseMapping caseMapping; // mapping caseID to case index for space efficiency
    
    public AnimationLog(XLog xlog) {
        this.xlog = xlog;
        this.caseMapping = new CaseMapping(xlog);
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFileName() {
        return this.fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public int getCaseIndexFromId(String caseId) {
    	return this.caseMapping.getIndex(caseId);
    }
    
    public DateTime getStartDate() {
        if (startDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(2050, 1, 1);
            DateTime logStartDate = new DateTime(cal.getTime());
            
            for (ReplayTrace trace : this.getTraces()) {
                if (logStartDate.isAfter(trace.getStartDate())) {
                    logStartDate = trace.getStartDate();
                }
            }
            startDate = logStartDate;
        }
        return startDate;
    }
    
    public DateTime getEndDate() {
        if (endDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(1920, 1, 1);
            DateTime logEndDate = new DateTime(cal.getTime());
            
            for (ReplayTrace trace : this.getTraces()) {
                if (logEndDate.isBefore(trace.getEndDate())) {
                    logEndDate = trace.getEndDate();
                }
            }
            endDate = logEndDate;
        }
        return endDate;
    }
    
    public void add(XTrace trace, ReplayTrace replayTrace) {
        traceMap.put(trace, replayTrace);
    }
    
    public Collection<ReplayTrace> getTraces() {
        return this.traceMap.values();
    }
    
    public List<ReplayTrace> getTracesWithOriginalOrder() {
    	List<ReplayTrace> orderedTraces = new ArrayList<ReplayTrace>();
    	for (XTrace xtrace : this.xlog) {
    		if (traceMap.containsKey(xtrace)) {
    			orderedTraces.add(traceMap.get(xtrace));
    		}
    	}
    	return orderedTraces;
    }

    public double getTraceFitness(double minBoundMoveCostOnModel) {
        return 0;
    }

    public double getMinBoundMoveOnModel() {
        return 0;
    }

    public int getReliableTraceCount() {
        return 0;
    }

    public String getUnReliableTraceIDs() {
        return "";
    }

    /*
    * Return a map of intervals by sequenceIds for this log
    * Note that a trace might contain many overlapping intervals created from sequence flows    
    * key: sequence Id
    * value: list of intervals, each reprenseting a token transfer via the sequence flow (Id is the key)
    * The intervals of one sequenceId are sorted by start date, something look like below.
    * |--------------|
    *      |---------| 
    *      |---------|
    *        |------------------|
    *            |----------|
    *                  |-----------------------|
    */    
    public Map<String, SortedSet<Interval>> getIntervalMap() {
        Map<String, SortedSet<Interval>> sequenceByIds = new HashMap();
        
        SortedSet<Interval> transfers;
                
        for (ReplayTrace trace : traceMap.values()) {
            for (SequenceFlow seqFlow : trace.getSequenceFlows()) {
                if (!sequenceByIds.containsKey(seqFlow.getId())) {
                    transfers = new TreeSet<>(
                        new Comparator<Interval>() {
                            @Override
                            public int compare(Interval o1, Interval o2) {
                                if (o1.getStart().isBefore(o2.getStart())) {
                                    return -1;
                                } else {
                                    return +1;
                                }
                            }
                        });
                    sequenceByIds.put(seqFlow.getId(), transfers);
                }
//                LOGGER.info("Node1:" + seqFlow.getSourceRef().getName() + " id:" + seqFlow.getSourceRef().getId() +
//                            "Node2:" + seqFlow.getTargetRef().getName() + " id:" + seqFlow.getTargetRef().getId());
                sequenceByIds.get(seqFlow.getId()).add(TimeUtilities.getInterval(seqFlow));
            }
        }
        return sequenceByIds;
    }

    public void clear() {
        this.xlog = null;
        for (ReplayTrace trace : this.traceMap.values()) {
            trace.clear();
        }
        traceMap.clear();
    }

}
