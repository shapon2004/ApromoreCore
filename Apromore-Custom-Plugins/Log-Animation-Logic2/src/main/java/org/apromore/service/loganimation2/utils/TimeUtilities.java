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

package org.apromore.service.loganimation2.utils;

import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TimeUtilities {
    public static SortedSet<DateTime> sortDates(Set<DateTime> dateSet) {
        
        SortedSet<DateTime> timeline = new TreeSet<>(
                                new Comparator<DateTime>() {
                                    @Override
                                    public int compare(DateTime o1, DateTime o2) {
                                        return o1.compareTo(o2);
                                    }
                                }); 
        
        for (DateTime dateE : dateSet) {
            timeline.add(dateE);
        }
        
        return timeline;
    }

    
}
