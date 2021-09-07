package org.apromore.service.loganimation.replay;

import de.hpi.bpmn2_0.model.Definitions;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

@Getter
public class AnimationData {
    @NonNull
    private final List<AnimationLog> animationLogs;

    @NonNull
    private final JSONObject setupData;

    public AnimationData(List<AnimationLog> animationLogs, JSONObject setupData) {
        this.animationLogs = Collections.unmodifiableList(animationLogs);
        this.setupData = setupData;
    }
}
