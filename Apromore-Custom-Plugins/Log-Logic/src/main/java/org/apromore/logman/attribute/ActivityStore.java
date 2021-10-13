package org.apromore.logman.attribute;

import org.apromore.logman.ALog;
import org.apromore.logman.ATrace;
import org.apromore.logman.Constants;
import org.apromore.logman.utils.LogUtils;
import org.deckfour.xes.model.XEvent;

public class ActivityStore extends AttributeStore {
    public ActivityStore(ALog log) {
        super();
        for (ATrace trace: log.getTraces()) {
            for (XEvent event : trace.getEvents()) {
                registerXAttributes(event.getAttributes(), AttributeLevel.EVENT);
            }
        }
    }

    protected boolean isValidAttributeKey(String key) {
        return key.equals(Constants.ATT_KEY_CONCEPT_NAME);
    }
}
