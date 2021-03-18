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
import org.apromore.plugin.portal.processdiscoverer.animation.Stats;
import org.apromore.plugin.portal.processdiscoverer.vis.MissingLayoutException;
import org.apromore.processdiscoverer.Abstraction;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.jgraph.ProMJGraph;
import org.apromore.processmining.plugins.bpmn.BpmnDefinitions;
import org.apromore.service.loganimation.AnimationResult;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.replay.AnimationLog;
import org.deckfour.xes.model.XLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;

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
    
    private List<LogAnimationService2.Log> createLogs(XLog eventlog) {
        List<LogAnimationService2.Log> logs = new ArrayList<>();
        Iterator<String> colors = Arrays.asList("#0088FF", "#FF8800", "#88FF00").iterator();
        LogAnimationService2.Log log = new LogAnimationService2.Log();
        log.fileName = parent.getContextData().getLogName();
        log.xlog     = eventlog;
        log.color    = colors.hasNext() ? colors.next() : "red";
        logs.add(log);
        return logs;
    }
    
    public AnimationResult createAlignment() throws Exception {
        Abstraction abs = parent.getOutputData().getAbstraction();
        if (abs.getLayout() == null) {
            throw new MissingLayoutException("Missing layout of the process map for animating");
        }
        
        LogAnimationService2 logAnimationService = parent.getLogAnimationService();
        AnimationResult result = null;
        if (parent.isBPMNView()) {
            result = logAnimationService.createAnimation(
                                        getBPMN(abs.getValidBPMNDiagram(), abs.getLayout().getGraphLayout()),
                                        createLogs(parent.getLogData().getLog().getActualXLog()));
            
        }
        else {
            result = logAnimationService.createAnimationWithNoGateways(
                                        getBPMN(abs.getValidBPMNDiagram(), null), 
                                        getBPMN(abs.getDiagram(), abs.getLayout().getGraphLayout()), 
                                        createLogs(parent.getLogData().getLog().getActualXLog())); 
        }
        
        if (result == null) {
            throw new Exception("No animation result was created");
        }
        return result;
    }
    
    public void createAnimation() throws Exception {
        // Align log and model
        AnimationResult alignmentResult = createAlignment();
        
        // Prepare animation data
        AnimationContext animateContext = new AnimationContext(alignmentResult.getAnimationLogs());
        ModelMapping modelMapping = new ModelMapping(parent.getOutputData().getAbstraction().getValidBPMNDiagram());
        
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
        
        // Prepare initial setup data for the animation
        JSONObject setupData = alignmentResult.getSetupJSON();
        setupData.put("recordingFrameRate", animateContext.getRecordingFrameRate());
        setupData.put("recordingDuration", animateContext.getRecordingDuration());
        setupData.put("logStartFrameIndexes", logStartFrameIndexes);
        setupData.put("logEndFrameIndexes", logEndFrameIndexes);
        setupData.put("elementIndexIDMap", modelMapping.getElementJSON());
        setupData.put("caseCountsByFrames", Stats.computeCaseCountsJSON(animationMovie));
        
        // Show animation view
        String javascript = "Ap.pd.switchToAnimation(" + 
                            "'" + parent.getPluginExecutionId() + "'," + 
                            "'" + setupData.toString() + "'" +
                            ")";
        Clients.evalJavaScript(javascript);
    }
    
    private String getBPMN(BPMNDiagram diagram, ProMJGraph layoutInfo) {
        BpmnDefinitions.BpmnDefinitionsBuilder definitionsBuilder = null;
        if (layoutInfo != null) {
            definitionsBuilder = new BpmnDefinitions.BpmnDefinitionsBuilder(diagram, layoutInfo);
        }
        else {
            definitionsBuilder = new BpmnDefinitions.BpmnDefinitionsBuilder(diagram);
        }
        BpmnDefinitions definitions = new BpmnDefinitions("definitions", definitionsBuilder);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n " +
                "xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n " +
                "xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n " +
                "targetNamespace=\"http://www.omg.org/bpmn20\"\n " +
                "xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\">");
        sb.append(definitions.exportElements());
        sb.append("</definitions>");
        String bpmnText = sb.toString();
        bpmnText.replaceAll("\n", "");
        return bpmnText;
    }
    
}
