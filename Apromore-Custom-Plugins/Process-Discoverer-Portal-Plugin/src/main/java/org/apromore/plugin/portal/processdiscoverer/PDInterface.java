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
package org.apromore.plugin.portal.processdiscoverer;

import java.util.List;

import org.apromore.apmlog.APMLog;
import org.apromore.apmlog.filter.PLog;
import org.apromore.apmlog.filter.rules.LogFilterRule;
import org.apromore.logman.attribute.AbstractAttribute;
import org.apromore.logman.attribute.log.AttributeInfo;
import org.apromore.plugin.portal.processdiscoverer.data.CaseDetails;
import org.apromore.plugin.portal.processdiscoverer.data.ConfigData;
import org.apromore.plugin.portal.processdiscoverer.data.ContextData;
import org.apromore.plugin.portal.processdiscoverer.data.GraphSettings;
import org.apromore.plugin.portal.processdiscoverer.data.LogStatistics;
import org.apromore.plugin.portal.processdiscoverer.data.ViewSettings;
import org.deckfour.xes.model.XLog;

public interface PDInterface {
    // READ DATA
    ContextData getPDContext();
    ConfigData getConfig();
    XLog getActualXLog();
    
    /*
    AttributeLogSummary getOriginalLogSummary();
    AttributeLogSummary getCurrentLogSummary();
    
    String getFilteredMinDuration();
    String getFilteredMedianDuration();
    String getFilteredMeanDuration();
    String getFilteredMaxDuration();
    String getFilteredStartTime();
    String getFilteredEndTime();
    */
    
    CaseDetails getCaseDetails();
    List<AttributeInfo> getAttributeInfoList();
    List<AbstractAttribute> getAvailableAttributes();
    LogStatistics getOriginalLogStatistics();
    LogStatistics getCurrentLogStatistics();
    
    // FILTER DATA
    List<LogFilterRule> getFilterCriteria();
    boolean filter_RemoveTracesAnyValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RetainTracesAnyValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RemoveTracesAllValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RetainTracesAllValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RemoveEventsAnyValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RetainEventsAnyValueOfEventAttribute(String value, String attKey) throws Exception;
    boolean filter_RemoveTracesAnyValueOfDirectFollowRelation(String value, String attKey) throws Exception;
    boolean filter_RetainTracesAnyValueOfDirectFollowRelation(String value, String attKey) throws Exception;
    PLog getFilteredPLog();
    void clearFilter();
    void setCurrentFilterCriteria(List<LogFilterRule> criteria);
    void updateFilterLogs(PLog pLog, APMLog apmLog);
    
    // UPDATE STATE
    void setPerspective(String newPerspectiveAttribute);
    void setGraphSettings(GraphSettings absSettings);
    void setViewSettings(ViewSettings viewSettings);
    
    
    
}
