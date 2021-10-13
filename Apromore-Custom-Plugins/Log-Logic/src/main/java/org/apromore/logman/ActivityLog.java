package org.apromore.logman;

import org.apromore.logman.attribute.ActivityStore;
import org.apromore.logman.attribute.AttributeStore;
import org.deckfour.xes.model.XLog;

public class ActivityLog extends ALog {
    public ActivityLog(XLog log) {
        super(log);
    }

    protected void createAttributeStore() {
        originalAttributeStore = new ActivityStore(this);
    }
}
