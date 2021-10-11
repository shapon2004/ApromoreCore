package org.apromore.service.loganimation2.impl;

import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.replay.AnimationParams;
import org.apromore.service.loganimation2.utils.LogUtility;
import org.deckfour.xes.model.XTrace;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Prepare parameters for the animation
 */
public class ParamsReader {
    public static AnimationParams createAnimationParams(List<LogAnimationService2.Log> logs) throws AnimationException {
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
    private static double computeArtificialTransitionDuration(List<LogAnimationService2.Log> logs, int artificialTransDurRatio) {
        double UPPER_BOUND = 1.0/artificialTransDurRatio;
        double LOWER_BOUND =  1.0/(2*artificialTransDurRatio);

        // Scan the log the compute the average transition duration and log duration
        // Artificial transition duration is set according to the log
        double totalAvgTransitionDur = 0;
        long minLogTimestamp = Long.MAX_VALUE, maxLogTimestamp = 0;
        int traceCount = 0;
        for (LogAnimationService2.Log log : logs) {
            for (XTrace trace : log.xlog) {
                minLogTimestamp = Math.min(minLogTimestamp, !trace.isEmpty() ? LogUtility.getTimestamp(trace.get(0)).getTime() : Long.MAX_VALUE);
                maxLogTimestamp = Math.max(maxLogTimestamp, !trace.isEmpty() ? LogUtility.getTimestamp(trace.get(trace.size()-1)).getTime() : 0);
                if (trace.size() >= 2) {
                    long traceDuration = LogUtility.getTimestamp(trace.get(trace.size()-1)).getTime() - LogUtility.getTimestamp(trace.get(0)).getTime();
                    totalAvgTransitionDur += traceDuration/(trace.size()-1);
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
