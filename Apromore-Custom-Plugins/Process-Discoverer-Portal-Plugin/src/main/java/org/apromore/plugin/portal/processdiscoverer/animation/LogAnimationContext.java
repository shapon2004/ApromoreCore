/*-
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
package org.apromore.plugin.portal.processdiscoverer.animation;

import org.apromore.service.loganimation.replay.AnimationLog;

public class LogAnimationContext {
    private long logStartTimestamp;
    private long logEndTimestamp;
    private double logToRecordingTimeRatio = 1; //a second on the animation timeline is converted to actual seconds
    private double logTimeFrameInterval; // frame interval in terms of log time (milliseconds)
    
    public LogAnimationContext(AnimationLog log) {
        this.logStartTimestamp = log.getStartDate().getMillis();
        this.logEndTimestamp = log.getEndDate().getMillis();
    }
    
    public LogAnimationContext(AnimationLog log, int recordingFrameRate, int recordingDuration) {
        this(log);
    }
    
    public long getLogStartTimestamp() {
        return this.logStartTimestamp;
    }
    
    public long getLogEndTimestamp() {
        return this.logEndTimestamp;
    }
    
    public double getTimelineRatio() {
        return this.logToRecordingTimeRatio;
    }
    
}
