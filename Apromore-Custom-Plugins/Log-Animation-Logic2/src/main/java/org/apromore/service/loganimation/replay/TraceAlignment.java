package org.apromore.service.loganimation.replay;

import lombok.NonNull;
import org.apromore.alignmentautomaton.StepType;

import java.util.List;

public class TraceAlignment {
    private List<StepType> steps;
    private List<String> nodeIDs;

    public TraceAlignment(@NonNull List<StepType> steps, @NonNull List<String> nodeIDs) {
        this.steps = steps;
        this.nodeIDs = nodeIDs;
    }

    private boolean isValidStepIndex(int stepIndex) {
        return (stepIndex >= 0 && stepIndex < steps.size());
    }

    public int getNumberOfSteps() {
        return steps.size();
    }

    public boolean isActivityMatched(int stepIndex) {
        if (!isValidStepIndex(stepIndex)) return false;
        return steps.get(stepIndex) == StepType.LMGOOD || steps.get(stepIndex) == StepType.MREAL;
    }

    public boolean isEventMatched(int stepIndex) {
        if (!isValidStepIndex(stepIndex)) return false;
        return steps.get(stepIndex) == StepType.LMGOOD || steps.get(stepIndex) == StepType.L;
    }

    public String getNodeId(int stepIndex) {
        return isActivityMatched(stepIndex) ? nodeIDs.get(stepIndex) : "";
    }

    public int getEventIndex(int stepIndex) {
        return isEventMatched(stepIndex) ? stepIndex : -1;
    }
}
