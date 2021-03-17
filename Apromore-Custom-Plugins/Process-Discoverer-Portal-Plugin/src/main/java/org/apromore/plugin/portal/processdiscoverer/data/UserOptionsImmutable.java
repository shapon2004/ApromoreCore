package org.apromore.plugin.portal.processdiscoverer.data;

import org.apromore.logman.attribute.graph.MeasureAggregation;
import org.apromore.logman.attribute.graph.MeasureRelation;
import org.apromore.logman.attribute.graph.MeasureType;

public final class UserOptionsImmutable extends UserOptionsData {
    private UserOptionsImmutable(UserOptionsData userOptions) {
        this.arcs_value = userOptions.arcs_value;
        this.bpmnMode = userOptions.bpmnMode;
        this.duration_max = userOptions.duration_max;
        this.duration_mean = userOptions.duration_mean;
        this.duration_median = userOptions.duration_median;
        this.duration_min = userOptions.duration_min;
        this.duration_total = userOptions.duration_total;
    }
    
    public static UserOptionsImmutable copyOf(UserOptionsData userOptions) {
        return new UserOptionsImmutable(userOptions);
    }
    
    @Override
    public void setMainAttributeKey(String key) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setNodeFilterValue(double newValue) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setArcFilterValue(double newValue) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setParallelismFilterValue(double newValue) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFixedType(MeasureType newType) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFixedAggregation(MeasureAggregation fixed) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFixedRelation(MeasureRelation newRelation) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setPrimaryType(MeasureType newType) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setPrimaryAggregation(MeasureAggregation newAggregate) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setPrimaryRelation(MeasureRelation newRelation) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setSecondaryType(MeasureType type) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setSecondaryAggregation(MeasureAggregation newAggregate) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setSecondaryRelation(MeasureRelation newRelation) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setBPMNMode(boolean bpmnMode) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setInvertedNodesMode(boolean inverted_nodes) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setInvertedArcsMode(boolean inverted_arcs) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setSelectedLayout(int layout) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setRetainZoomPan(boolean retainZoomPan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setIncludeSecondary(boolean includeSecondary) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setLayoutHierarchy(boolean layoutOn) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setLayoutDagre(boolean layoutOn) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setUseDynamic(boolean value) {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public void setFrequencyTotal(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public void setFrequencyCase(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFrequencyMin(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFrequencyMax(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFrequencyMean(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFrequencyMedian(boolean frequency) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setDurationCumulative(boolean duration) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setDurationMin(boolean duration) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setDurationMax(boolean duration) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setDurationMean(boolean duration) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setDurationMedian(boolean duration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrimaryAggregation() {
        throw new UnsupportedOperationException();
    }
}
