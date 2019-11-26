package org.apromore.xlog.singletonlog;

import org.apromore.xes.extension.XExtension;
import org.apromore.xes.id.XID;
import org.apromore.xes.id.XIDFactory;
import org.apromore.xes.model.XTrace;
import org.apromore.xes.util.XAttributeUtils;

import java.util.Set;

public class XEventSingletonImpl implements org.apromore.xes.model.XEvent {
    private XID id;
    private org.apromore.xes.model.XAttributeMap attributes;

    public XEventSingletonImpl() {
        this(XIDFactory.instance().createId(), new XAttributeMapSingletonImpl());
    }

    public XEventSingletonImpl(XID id) {
        this(id, new XAttributeMapSingletonImpl());
    }

    public XEventSingletonImpl(org.apromore.xes.model.XAttributeMap attributes) {
        this(XIDFactory.instance().createId(), attributes);
    }

    public XEventSingletonImpl(XID id, org.apromore.xes.model.XAttributeMap attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public org.apromore.xes.model.XAttributeMap getAttributes() {
        return this.attributes;
    }

    public void setAttributes(org.apromore.xes.model.XAttributeMap attributes) {
        this.attributes = attributes;
    }

    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    public Set<XExtension> getExtensions() {
        return XAttributeUtils.extractExtensions(this.attributes);
    }

    public Object clone() {
        XEventSingletonImpl clone;
        try {
            clone = (XEventSingletonImpl)super.clone();
        } catch (CloneNotSupportedException var3) {
            var3.printStackTrace();
            return null;
        }

        clone.id = XIDFactory.instance().createId();
        clone.attributes = (org.apromore.xes.model.XAttributeMap)this.attributes.clone();
        return clone;
    }

    public boolean equals(Object o) {
        return o instanceof XEventSingletonImpl ? ((XEventSingletonImpl)o).id.equals(this.id) : false;
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public XID getID() {
        return this.id;
    }

    public void setID(XID id) {
        this.id = id;
    }

    public void accept(org.apromore.xes.model.XVisitor visitor, XTrace trace) {
        visitor.visitEventPre(this, trace);
        for(org.apromore.xes.model.XAttribute attribute : attributes.values()) {
            attribute.accept(visitor, this);
        }

        visitor.visitEventPost(this, trace);
    }
}
