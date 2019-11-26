/*
 * Copyright © 2009-2018 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.processmining.stagemining.utils;

import org.apromore.xes.extension.std.XConceptExtension;
import org.apromore.xes.extension.std.XLifecycleExtension;
import org.apromore.xes.extension.std.XOrganizationalExtension;
import org.apromore.xes.extension.std.XTimeExtension;
import org.apromore.xes.factory.XFactory;
import org.apromore.xes.factory.XFactoryRegistry;
import org.apromore.xes.model.XTrace;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogUtilites {

	public static String getConceptName(org.apromore.xes.model.XAttributable attrib) {
		String name = XConceptExtension.instance().extractName(attrib);
		return (name != null ? name : "<no name>");
	}

	public static void setConceptName(org.apromore.xes.model.XAttributable attrib, String name) {
		XConceptExtension.instance().assignName(attrib, name);
	}

	public static String getLifecycleTransition(org.apromore.xes.model.XEvent event) {
		String name = XLifecycleExtension.instance().extractTransition(event);
		return (name != null ? name : "<no transition>");
	}

	public static void setLifecycleTransition(org.apromore.xes.model.XEvent event, String transition) {
		XLifecycleExtension.instance().assignTransition(event, transition);
	}

	public static void setTimestamp(org.apromore.xes.model.XEvent event, Date timestamp) {
		XTimeExtension.instance().assignTimestamp(event, timestamp);
	}

	public static DateTime getTimestamp(org.apromore.xes.model.XEvent event) {
		Date date = XTimeExtension.instance().extractTimestamp(event);
		return new DateTime(date);
	}
	
	

	public static String getOrganizationalResource(org.apromore.xes.model.XEvent event) {
		String name = XOrganizationalExtension.instance().extractResource(event);
		return (name != null ? name : "<no resource>");
	}

	public static String getValue(org.apromore.xes.model.XAttribute attr) {
		if (attr instanceof org.apromore.xes.model.XAttributeBoolean) {
			Boolean b = ((org.apromore.xes.model.XAttributeBoolean) attr).getValue();
			return b.toString();
		} else if (attr instanceof org.apromore.xes.model.XAttributeContinuous) {
			Double d = ((org.apromore.xes.model.XAttributeContinuous) attr).getValue();
			return d.toString();
		} else if (attr instanceof org.apromore.xes.model.XAttributeDiscrete) {
			Long l = ((org.apromore.xes.model.XAttributeDiscrete) attr).getValue();
			return l.toString();
		} else if (attr instanceof org.apromore.xes.model.XAttributeLiteral) {
			String s = ((org.apromore.xes.model.XAttributeLiteral) attr).getValue();
			return s;
		} else if (attr instanceof org.apromore.xes.model.XAttributeTimestamp) {
			Date d = ((org.apromore.xes.model.XAttributeTimestamp) attr).getValue();
			return d.toString();
		}
		return "";
	}
	
	/**
	 * Remember: this log has been pre-processed by 
	 * adding one starting event and one ending event
	 * Compute the number of distinct events by ending events
	 * @return: map from event name to number of distinct event before it in a trace
	 * key: ending event name, value: number of distinct event before the key in a trace
	 * Note: if one event is never an ending event in a trace, it will NOT
	 * be in the returning result.
	 */
	public static Map<String,Double> computeDistinctEventsStatToEnd(org.apromore.xes.model.XLog log) {
		Map<String,Double> result = new HashMap<String,Double>();
		Map<String,Integer> countOccurs = new HashMap<String,Integer>();
		Set<String> visited = new HashSet<String>();
		for (org.apromore.xes.model.XTrace trace : log) {
			//Count the number of distinct event in a trace
			visited.clear();
			for (org.apromore.xes.model.XEvent evt : trace) {
				visited.add(LogUtilites.getConceptName(evt).toLowerCase());
			}
			
			//Cumulatively add the number of distinct events in different traces
			org.apromore.xes.model.XEvent last = trace.get(trace.size()-2); //exclude the added ending event
			String lastName = LogUtilites.getConceptName(last).toLowerCase();
			if (!countOccurs.containsKey(lastName)) {
				countOccurs.put(lastName, 1);
			}
			else {
				countOccurs.put(lastName, countOccurs.get(lastName) + 1);
			}
			
			if (!result.containsKey(lastName)) {
				result.put(lastName, 1.0*visited.size()-2); //exclude the added starting and ending event
			}
			else {
				result.put(lastName, visited.size() - 2 + result.get(lastName));
			}
		}
		
		//Compute the average number of distinct events
		for (String eventName : result.keySet()) {
			result.put(eventName, 1.0*result.get(eventName)/countOccurs.get(eventName));
		}
		
		System.out.println("Avg. Distinct Events per Trace (end_event->number): " + result.toString());
		
		return result;
	}
	
	public static Map<String,Double> computeDistinctEventsStatFromStart(org.apromore.xes.model.XLog log) {
		Map<String,Double> result = new HashMap<String,Double>();
		Map<String,Integer> countOccurs = new HashMap<String,Integer>();
		Set<String> visited = new HashSet<String>();
		for (org.apromore.xes.model.XTrace trace : log) {
			//Count the number of distinct event in a trace
			visited.clear();
			for (org.apromore.xes.model.XEvent evt : trace) {
				visited.add(LogUtilites.getConceptName(evt).toLowerCase());
			}
			
			//Cumulatively add the number of distinct events in different traces
			org.apromore.xes.model.XEvent first = trace.get(1); //exclude the added start event
			String firstName = LogUtilites.getConceptName(first).toLowerCase();
			if (!countOccurs.containsKey(firstName)) {
				countOccurs.put(firstName, 1);
			}
			else {
				countOccurs.put(firstName, countOccurs.get(firstName) + 1);
			}
			
			if (!result.containsKey(firstName)) {
				result.put(firstName, 1.0*visited.size()-2); //exclude the added starting event
			}
			else {
				result.put(firstName, visited.size() - 2 + result.get(firstName));
			}
		}
		
		//Compute the average number of distinct events
		for (String eventName : result.keySet()) {
			result.put(eventName, 1.0*result.get(eventName)/countOccurs.get(eventName));
		}
		
		System.out.println("Distinct Events per Trace (start_event->number): " + result.toString());
		
		return result;
	}
	
	/**
	 * Select the event that is the ending event of a trace and 
	 * has the maximum number of distict events before it in a trace
	 * @param log
	 * @return: event name
	 */
	public static String computeMainstreamEndingEventName(org.apromore.xes.model.XLog log, boolean fromStart) {
		Map<String,Double> mapDistinctEvent = null;
		if (fromStart) {
			mapDistinctEvent = LogUtilites.computeDistinctEventsStatFromStart(log);
		}
		else {
			mapDistinctEvent = LogUtilites.computeDistinctEventsStatToEnd(log);
		}
		double maxCount = 0;
		String maxName = "";
		for (String eventName : mapDistinctEvent.keySet()) {
			if (mapDistinctEvent.get(eventName) > maxCount) {
				maxCount = mapDistinctEvent.get(eventName);
				maxName = eventName;
			}
		}
		return maxName;
	}
	
	public static int getDistinctEventClassCount(org.apromore.xes.model.XLog log) {
		Set<String> eventSet = new HashSet<String>();
		for (org.apromore.xes.model.XTrace trace : log) {
			for (org.apromore.xes.model.XEvent e : trace) {
				eventSet.add(LogUtilites.getConceptName(e).toLowerCase());
			}
		}
		return eventSet.size();
	}
	
	/**
	 * Add start and end events to all traces of an input log
	 * @param log
	 * @throws ParseException
	 */
	public static void addStartEndEvents(org.apromore.xes.model.XLog log) throws ParseException {
		// Do not add again if the log has been added start and end events before.
		for (org.apromore.xes.model.XTrace trace : log) {
			String firstEvent = LogUtilites.getConceptName(trace.get(0)).toLowerCase();
			String lastEvent = LogUtilites.getConceptName(trace.get(trace.size()-1)).toLowerCase();
			if (firstEvent.equals("start") && lastEvent.equals("end")) {
				return;
			}
    	} 
		
		//--------------------------------
		// Create start/end event
		//--------------------------------
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); 
//		XFactory factory = new XFactoryNaiveImpl();
//		XFactory factory = new XFactoryExternalStore.InMemoryStoreImpl();
		try {
			XFactory factory = XFactoryRegistry.instance().currentDefault().getClass().getConstructor().newInstance();
//			XFactory factory = XFactoryRegistry.instance().currentDefault();

			org.apromore.xes.model.XEvent startEvent = factory.createEvent();
			org.apromore.xes.model.XAttributeMap startEventMap = factory.createAttributeMap();
			startEventMap.put("concept:name", factory.createAttributeLiteral("concept:name", "start", null));
			startEventMap.put("lifecycle:transition", factory.createAttributeLiteral("lifecycle:transition", "complete", null));
			startEventMap.put("time:timestamp", factory.createAttributeTimestamp("time:timestamp", df.parse("01/01/1970"), null));
			startEvent.setAttributes(startEventMap);

			org.apromore.xes.model.XEvent endEvent = factory.createEvent();
			org.apromore.xes.model.XAttributeMap endEventMap = factory.createAttributeMap();
			endEventMap.put("concept:name", factory.createAttributeLiteral("concept:name", "end", null));
			endEventMap.put("lifecycle:transition", factory.createAttributeLiteral("lifecycle:transition", "complete", null));
			endEventMap.put("time:timestamp", factory.createAttributeTimestamp("time:timestamp", df.parse("01/01/2020"), null));
			endEvent.setAttributes(endEventMap);

			for (XTrace trace : log) {
				org.apromore.xes.model.XEvent preEvt = null;
				trace.add(0, startEvent);
				trace.add(endEvent);
			}
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(
					"Exception while creating XES factory", e);
		}
	}

}