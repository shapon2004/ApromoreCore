package org.apromore.service.loganimation.impl;

import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.utils.LogUtility;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Prepare logs for animation
 */
public class LogProcessor {
    public static void cleanLogs(List<LogAnimationService2.Log> logs) {
        for (LogAnimationService2.Log log : logs) {
            for (XTrace trace : log.xlog) {
                cleanTrace(trace);
            }
        }
    }

    /**
     * Clean the trace:
     *  - Remove events with lifecycle different from "complete" value
     *  - Make sure the only "complete" event logs have the same start and end timestamp as the original one
     */
    private static void cleanTrace(XTrace trace) {
        if (trace == null || trace.isEmpty()) return;

        Date startTimestamp = LogUtility.getTimestamp(trace.get(0));
        Date endTimestamp = LogUtility.getTimestamp(trace.get(trace.size()-1));

        // Remove events with lifecycle different from "complete"
        Iterator<XEvent> iterator = trace.iterator();
        while (iterator.hasNext()) {
            XEvent event = iterator.next();
            if (!LogUtility.getLifecycleTransition(event).toLowerCase().equals("complete")) {
                iterator.remove();
            }
        }

        // After events have been removed, adjust the timestamp of the first/last events to ensure the clean log has a
        // matched start/end date with the original one.
        if (!trace.isEmpty()) {
            LogUtility.setTimestamp(trace.get(0), startTimestamp);
            if (trace.size() > 1) LogUtility.setTimestamp(trace.get(trace.size()-1), endTimestamp);
        }
    }
}
