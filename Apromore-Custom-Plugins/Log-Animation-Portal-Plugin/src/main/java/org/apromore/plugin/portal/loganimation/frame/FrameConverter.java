package org.apromore.plugin.portal.loganimation.frame;

public class FrameConverter {
    public static long getTimestampFromFrameIndex(long frameIndex, long startTimestamp, int frameGap) {
        return startTimestamp + frameIndex*frameGap;
    }
}
