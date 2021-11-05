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
package org.apromore.service.loganimation2.enablement;

import org.apromore.logman.attribute.log.AttributeLog;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

/**
 * Mapping from case IDs to indexes and vice versa.
 * @author Bruce Nguyen
 *
 */
public class CaseIdIndexing {
	private final HashBiMap<String, Integer> mapping = new HashBiMap<>();

	public CaseIdIndexing(AttributeLog attLog) {
		attLog.getTraces().forEachWithIndex((trace, index) -> mapping.put(trace.getTraceId(), index));
	}
	
	public int getIndex(String caseId) {
		return mapping.getIfAbsent(caseId, () -> -1);
	}
	
	public String getId(int index) {
		return mapping.inverse().getIfAbsent(index, () -> "");
	}
	
	public int size() {
		return mapping.size();
	}
}
