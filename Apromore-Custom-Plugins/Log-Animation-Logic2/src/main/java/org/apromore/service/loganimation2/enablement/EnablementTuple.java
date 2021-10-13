package org.apromore.service.loganimation2.enablement;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class EnablementTuple {
    private final String elementId;
    private final boolean isSkip;
    private final long startTimestamp;
    private final long endTimestamp;

    public static EnablementTuple valueOf(@NonNull String elementId, boolean isSkip,
                                          @NonNull String startTimestamp, @NonNull String endTimestamp) {
        try {
            return new EnablementTuple(elementId, isSkip, Long.valueOf(startTimestamp), Long.valueOf(endTimestamp));
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Could not create EnablementTuple from the parameters");
        }
    }

    private EnablementTuple(String elementId, boolean isSkip, long startTimestamp, long endTimestamp) {
        this.elementId = elementId;
        this.isSkip = isSkip;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }
}
