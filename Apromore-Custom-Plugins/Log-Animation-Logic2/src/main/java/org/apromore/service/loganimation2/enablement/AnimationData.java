package org.apromore.service.loganimation2.enablement;

import lombok.Getter;
import lombok.NonNull;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * <b>AnimationData</b> bundles multiple {@link EnablementLog} and the setup data.
 */
@Getter
public class AnimationData {
    @NonNull
    private final List<EnablementLog> enablementLogs;

    @NonNull
    private final JSONObject setupData;

    public AnimationData(@NonNull List<EnablementLog> enablementLogs, @NonNull AnimationParams animationParams) {
        this.enablementLogs = enablementLogs;
        this.setupData = createSetupData(enablementLogs, animationParams);
    }

    /**
     * Create JSON setup data for the animation
     * @param animationLogs: list of AnimationLog objects
     * @param animationParams: replay parameters
     * @return JSONObject containing setup data
     */
    private JSONObject createSetupData(List<EnablementLog> animationLogs, AnimationParams animationParams) {
        AnimationJSONBuilder2 jsonBuilder = new AnimationJSONBuilder2(animationLogs, animationParams);
        JSONObject setupData = jsonBuilder.parseLogCollection();
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }
}
