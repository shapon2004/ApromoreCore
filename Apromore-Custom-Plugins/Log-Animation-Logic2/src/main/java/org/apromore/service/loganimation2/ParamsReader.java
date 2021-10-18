/**
 * #%L
 * This file is part of "Apromore Enterprise Edition".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.service.loganimation2;

import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.logman.attribute.log.AttributeTrace;
import org.apromore.service.loganimation2.data.AnimationParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Prepare parameters for the animation
 */
public class ParamsReader {
    public static AnimationParams createAnimationParams(List<AttributeLog> logs) throws AnimationException {
        try {
            InputStream is = ParamsReader.class.getClassLoader().getResourceAsStream("properties.xml");
            Properties props = new Properties();
            props.loadFromXML(is);

            AnimationParams params = new AnimationParams();
            params.setTimelineSlots(Integer.valueOf(props.getProperty("TimelineSlots")).intValue());
            params.setTotalEngineSeconds(Integer.valueOf(props.getProperty("TotalEngineSeconds")).intValue());
            params.setTotalEngineSeconds(600); //Override this setting for testing

            int artificialTransitionRatio = Integer.valueOf(props.getProperty("ArtificialTransitionDurationRatio")).intValue();
            int artificalTransitionDur = (int)computeArtificialTransitionDuration(logs, artificialTransitionRatio);
            params.setStartEventToFirstEventDuration(artificalTransitionDur);
            params.setLastEventToEndEventDuration(artificalTransitionDur);

            return params;
        } catch (IOException e) {
            throw new AnimationException("An internal error occurred when reading animation properties file. Please check system logs");
        }
    }

    /**
     * Compute an artificial transition duration which is used for the transition
     * from the Start Event to the next node and from a node to the End Event.
     * @param logs: logs used in animation
     * @param artificialTransDurRatio: the parameter for the artificial duration compared to the total timeline duration, e.g. 20 means 1/20
     * @return: artificial transition duration in seconds
     */
    private static double computeArtificialTransitionDuration(List<AttributeLog> logs, int artificialTransDurRatio) {
        double UPPER_BOUND = 1.0/artificialTransDurRatio;
        double LOWER_BOUND =  1.0/(2*artificialTransDurRatio);

        // Scan the log the compute the average transition duration and log duration
        // Artificial transition duration is set according to the log
        double totalAvgTransitionDur = 0;
        long minLogTimestamp = Long.MAX_VALUE, maxLogTimestamp = 0;
        int traceCount = 0;
        for (AttributeLog log : logs) {
            for (AttributeTrace trace : log.getTraces()) {
                minLogTimestamp = Math.min(minLogTimestamp, !trace.isEmpty() ? trace.getStartTimeAtIndex(0) : Long.MAX_VALUE);
                maxLogTimestamp = Math.max(maxLogTimestamp, !trace.isEmpty() ? trace.getEndTimeAtIndex(trace.getValueTrace().size()-1) : 0);
                if (trace.getValueTrace().size() >= 2) {
                    long traceDuration = trace.getEndTimeAtIndex(trace.getValueTrace().size()-1) - trace.getStartTimeAtIndex(0);
                    totalAvgTransitionDur += traceDuration/(trace.getValueTrace().size()-1);
                    traceCount++;
                }
            }
        }

        double avgTransitionDur = (traceCount != 0) ? totalAvgTransitionDur/traceCount : 0;
        long logDuration = (minLogTimestamp >= maxLogTimestamp) ? 0 : (maxLogTimestamp - minLogTimestamp);
        double timelineDur = logDuration + 2*avgTransitionDur;
        double avgTransitionRatio = (timelineDur > 0) ? avgTransitionDur/timelineDur : 0;

        // Adjust the artificial transition duration so that it's not too big or small.
        double artificialTransitionDur = avgTransitionDur/1000;
        if (avgTransitionRatio >= UPPER_BOUND) {
            artificialTransitionDur = timelineDur*UPPER_BOUND/1000;
        }
        else if (avgTransitionRatio <= LOWER_BOUND) {
            artificialTransitionDur = timelineDur*LOWER_BOUND/1000;
        }

        if (artificialTransitionDur == 0) artificialTransitionDur = 10;

        return artificialTransitionDur;
    }
}
