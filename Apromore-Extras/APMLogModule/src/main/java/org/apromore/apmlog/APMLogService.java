package org.apromore.apmlog;

import org.apromore.xes.model.XLog;

public interface APMLogService {

    public APMLog findAPMLogForXLog(XLog xLog);
}