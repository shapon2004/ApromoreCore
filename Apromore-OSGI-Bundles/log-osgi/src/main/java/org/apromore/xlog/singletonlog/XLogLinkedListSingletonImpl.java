package org.apromore.xlog.singletonlog;

import org.apromore.xes.classification.XEventClassifier;
import org.apromore.xes.extension.XExtension;
import org.apromore.xes.info.XLogInfo;
import org.apromore.xes.model.XTrace;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class XLogLinkedListSingletonImpl extends LinkedList<org.apromore.xes.model.XTrace> implements org.apromore.xes.model.XLog {
    private static final long serialVersionUID = -9192919845877466525L;
    private org.apromore.xes.model.XAttributeMap attributes;
    private Set<XExtension> extensions;
    private List<XEventClassifier> classifiers;
    private List<org.apromore.xes.model.XAttribute> globalTraceAttributes;
    private List<org.apromore.xes.model.XAttribute> globalEventAttributes;
    private XEventClassifier cachedClassifier;
    private XLogInfo cachedInfo;

    public XLogLinkedListSingletonImpl(org.apromore.xes.model.XAttributeMap attributeMap) {
        this.attributes = attributeMap;
        this.extensions = new UnifiedSet<>();
        this.classifiers = new ArrayList();
        this.globalTraceAttributes = new ArrayList();
        this.globalEventAttributes = new ArrayList();
        this.cachedClassifier = null;
        this.cachedInfo = null;
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
        return this.extensions;
    }

    public Object clone() {
        XLogLinkedListSingletonImpl clone = (XLogLinkedListSingletonImpl)super.clone();
        clone.attributes = (org.apromore.xes.model.XAttributeMap)this.attributes.clone();
        clone.extensions = new UnifiedSet(this.extensions);
        clone.classifiers = new ArrayList(this.classifiers);
        clone.globalTraceAttributes = new ArrayList(this.globalTraceAttributes);
        clone.globalEventAttributes = new ArrayList(this.globalEventAttributes);
        clone.cachedClassifier = null;
        clone.cachedInfo = null;
        clone.clear();

        for(org.apromore.xes.model.XTrace trace : this) {
            clone.add((org.apromore.xes.model.XTrace)trace.clone());
        }

        return clone;
    }

    public List<XEventClassifier> getClassifiers() {
        return this.classifiers;
    }

    public List<org.apromore.xes.model.XAttribute> getGlobalEventAttributes() {
        return this.globalEventAttributes;
    }

    public List<org.apromore.xes.model.XAttribute> getGlobalTraceAttributes() {
        return this.globalTraceAttributes;
    }

    public boolean accept(org.apromore.xes.model.XVisitor visitor) {
        if (!visitor.precondition()) {
            return false;
        } else {
            visitor.init(this);
            visitor.visitLogPre(this);

            for(XExtension extension : extensions) {
                extension.accept(visitor, this);
            }

            for(XEventClassifier classifier : classifiers) {
                classifier.accept(visitor, this);
            }

            for(org.apromore.xes.model.XAttribute attribute : attributes.values()) {
                attribute.accept(visitor, this);
            }

            for(XTrace trace : this) {
                trace.accept(visitor, this);
            }

            visitor.visitLogPost(this);
            return true;
        }
    }

    public XLogInfo getInfo(XEventClassifier classifier) {
        return classifier.equals(this.cachedClassifier) ? this.cachedInfo : null;
    }

    public void setInfo(XEventClassifier classifier, XLogInfo info) {
        this.cachedClassifier = classifier;
        this.cachedInfo = info;
    }
}
