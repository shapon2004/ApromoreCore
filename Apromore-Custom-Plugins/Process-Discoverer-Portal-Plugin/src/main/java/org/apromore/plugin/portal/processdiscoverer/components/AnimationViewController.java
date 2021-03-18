package org.apromore.plugin.portal.processdiscoverer.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apromore.plugin.portal.processdiscoverer.PDController;
import org.apromore.plugin.portal.processdiscoverer.animation.AnimationContext;
import org.apromore.plugin.portal.processdiscoverer.animation.AnimationIndex;
import org.apromore.plugin.portal.processdiscoverer.animation.FrameRecorder;
import org.apromore.plugin.portal.processdiscoverer.animation.ModelMapping;
import org.apromore.plugin.portal.processdiscoverer.animation.Movie;
import org.apromore.portal.plugincontrol.PluginExecution;
import org.apromore.portal.plugincontrol.PluginExecutionManager;
import org.apromore.service.loganimation.AnimationResult;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.replay.AnimationLog;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;

/**
 * This class manages the animation visualization 
 * 
 * @author Bruce Nguyen
 *
 */
public class AnimationViewController extends VisualController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationViewController.class);
    
    public AnimationViewController(PDController parent) {
        super(parent);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void initializeControls(Object data) throws Exception {
        //
    }
    
    @Override
    public void initializeEventListeners(Object data) throws Exception {
        
    }
    
    @Override
    public void updateUI(Object data) throws Exception {
        //
    }
    
    private List<LogAnimationService2.Log> createLogs(XLog eventlog, String logName) {
        List<LogAnimationService2.Log> logs = new ArrayList<>();
        Iterator<String> colors = Arrays.asList("#0088FF", "#FF8800", "#88FF00").iterator();
        LogAnimationService2.Log log = new LogAnimationService2.Log();
        log.fileName = logName;
        log.xlog     = eventlog;
        log.color    = colors.hasNext() ? colors.next() : "red";
        logs.add(log);
        return logs;
    }
    
    public AnimationResult createAlignment() {
        if (logAnimationService != null) {  // logAnimationService is null if invoked from the editor toobar
            AnimationResult result = bpmnNoGateways.isEmpty() ? logAnimationService.createAnimation(bpmnWithGateways, logs) :
                                logAnimationService.createAnimationWithNoGateways(bpmnWithGateways, bpmnNoGateways, logs);
            if (result == null) {
                throw new Exception("No animation result was created");
            }
            return result;
        }
        else {
            throw new Exception("Log Animation Service is not available");
        }
    }
    
    public void createAnimation() {
        AnimationResult alignmentResult = createAlignment();
        AnimationContext animateContext = new AnimationContext(alignmentResult.getAnimationLogs());
        ModelMapping modelMapping = new ModelMapping(parent.get);
        
        JSONObject setupData = alignmentResult.getSetupJSON();
        setupData.put("recordingFrameRate", animateContext.getRecordingFrameRate());
        setupData.put("recordingDuration", animateContext.getRecordingDuration());
        setupData.put("logStartFrameIndexes", logStartFrameIndexes);
        setupData.put("logEndFrameIndexes", logEndFrameIndexes);
        setupData.put("elementIndexIDMap", modelMapping.getElementJSON());
        setupData.put("caseCountsByFrames", Stats.computeCaseCountsJSON(animationMovie));
        
        
        
        long timer = System.currentTimeMillis();
        List<AnimationIndex> animationIndexes = new ArrayList<>();
        JSONArray logStartFrameIndexes = new JSONArray(); 
        JSONArray logEndFrameIndexes = new JSONArray();
        for (AnimationLog log : alignmentResult.getAnimationLogs()) {
            animationIndexes.add(new AnimationIndex(log, modelMapping, animateContext));
            logStartFrameIndexes.put(animateContext.getFrameIndexFromLogTimestamp(log.getStartDate().getMillis()));
            logEndFrameIndexes.put(animateContext.getFrameIndexFromLogTimestamp(log.getEndDate().getMillis()));
        }
        LOGGER.debug("Create animation index: " + (System.currentTimeMillis() - timer)/1000 + " seconds.");
        
        LOGGER.debug("Start recording frames");
        timer = System.currentTimeMillis();
        Movie animationMovie = FrameRecorder.record(alignmentResult.getAnimationLogs(), animationIndexes, animateContext);
        LOGGER.debug("Finished recording frames: " + (System.currentTimeMillis() - timer)/1000 + " seconds.");
        

        
        // Create page parameters, these parameters will be injected into the ZUL file by ZK
        String pluginExecutionId = PluginExecutionManager.registerPluginExecution(new PluginExecution(this), Sessions.getCurrent());
    }
    
}
