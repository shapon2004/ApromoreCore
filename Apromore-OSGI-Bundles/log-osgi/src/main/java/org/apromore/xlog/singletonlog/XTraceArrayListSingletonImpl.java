package org.apromore.xlog.singletonlog;

import org.apromore.xes.extension.XExtension;
import org.apromore.xes.model.XTrace;
import org.apromore.xes.util.XAttributeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class XTraceArrayListSingletonImpl extends ArrayList<org.apromore.xes.model.XEvent> implements XTrace {
    private static final long serialVersionUID = 843122019760036963L;
    private org.apromore.xes.model.XAttributeMap attributes;

    public XTraceArrayListSingletonImpl(org.apromore.xes.model.XAttributeMap attributeMap) {
        this.attributes = attributeMap;
    }

    public org.apromore.xes.model.XAttributeMap getAttributes() {
        return this.attributes;
    }

    public Set<XExtension> getExtensions() {
        return XAttributeUtils.extractExtensions(this.attributes);
    }

    public void setAttributes(org.apromore.xes.model.XAttributeMap attributes) {
        this.attributes = attributes;
    }

    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    public Object clone() {
        XTraceArrayListSingletonImpl clone = (XTraceArrayListSingletonImpl)super.clone();
        clone.attributes = (org.apromore.xes.model.XAttributeMap)this.attributes.clone();
        clone.clear();

        for(org.apromore.xes.model.XEvent event : this) {
            clone.add((org.apromore.xes.model.XEvent)event.clone());
        }

        return clone;
    }

    public synchronized int insertOrdered(org.apromore.xes.model.XEvent event) {
        if (this.size() == 0) {
            this.add(event);
            return 0;
        } else {
            org.apromore.xes.model.XAttribute insTsAttr = event.getAttributes().get("time:timestamp");
            if (insTsAttr == null) {
                this.add(event);
                return this.size() - 1;
            } else {
                Date insTs = ((org.apromore.xes.model.XAttributeTimestamp)insTsAttr).getValue();

                for(int i = this.size() - 1; i >= 0; --i) {
                    org.apromore.xes.model.XAttribute refTsAttr = this.get(i).getAttributes().get("time:timestamp");
                    if (refTsAttr == null) {
                        this.add(event);
                        return this.size() - 1;
                    }

                    Date refTs = ((org.apromore.xes.model.XAttributeTimestamp)refTsAttr).getValue();
                    if (!insTs.before(refTs)) {
                        this.add(i + 1, event);
                        return i + 1;
                    }
                }

                this.add(0, event);
                return 0;
            }
        }
    }

    public void accept(org.apromore.xes.model.XVisitor visitor, org.apromore.xes.model.XLog log) {
        visitor.visitTracePre(this, log);

        for(org.apromore.xes.model.XAttribute attribute : attributes.values()) {
            attribute.accept(visitor, this);
        }

        for(org.apromore.xes.model.XEvent event : this) {
            event.accept(visitor, this);
        }

        visitor.visitTracePost(this, log);
    }
}
