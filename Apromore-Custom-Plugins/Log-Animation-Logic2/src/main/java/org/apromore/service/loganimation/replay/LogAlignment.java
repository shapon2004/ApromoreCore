package org.apromore.service.loganimation.replay;

import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.transformation.BPMN2DiagramConverter;
import lombok.NonNull;
import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.alignmentautomaton.ReplayResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.jgraph.ProMJGraph;
import org.apromore.processmining.plugins.bpmn.BpmnDefinitions;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.impl.AnimationException;
import org.apromore.service.loganimation.impl.DiagramMappingException;
import org.apromore.service.loganimation.impl.ElementIDMapper;
import org.apromore.service.loganimation.json.AnimationJSONBuilder2;
import org.apromore.service.loganimation.utils.BPMNDiagramHelper;
import org.apromore.service.loganimation.utils.LogUtility;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the main class for creating log animation data based on alignment log and model.
 * The result is {@link AnimationData}.
 *
 * @author Bruce Nguyen
 */
public class LogAlignment {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAlignment.class);

    private final BPMNDiagram bpmnDiagram;
    private final BPMNDiagram bpmnDiagramWithGateways;

    private final Definitions oldBpmnDiagram;
    private final Definitions oldBpmnDiagramWithGateways;

    private final BPMNDiagramHelper diagramHelper;
    private final ReplayParams params;
    private final boolean isGraph;

    public LogAlignment(@NotNull BPMNDiagram bpmnDiagram, @NotNull ReplayParams replayParams, boolean isGraph) throws AnimationException {
        this.bpmnDiagram = bpmnDiagram;
        this.oldBpmnDiagram = convertToOldBPMNDiagram(bpmnDiagram);

        // Log alignment only works on BPMN diagram with gateways
        this.bpmnDiagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(bpmnDiagram) : bpmnDiagram;
        this.oldBpmnDiagramWithGateways = isGraph ? convertToOldBPMNDiagram(bpmnDiagramWithGateways) : oldBpmnDiagram;

        diagramHelper = new BPMNDiagramHelper(oldBpmnDiagramWithGateways);
        this.params = replayParams;
        this.isGraph = isGraph;
    }

    public AnimationData align(@NotNull @NotEmpty List<LogAnimationService2.Log> logs) throws AnimationException {
        List<AnimationLog> animationLogs = logs.stream()
                .map(log -> align(log))
                .collect(Collectors.toList());
        AnimationData animationData = new AnimationData(animationLogs, createSetupData(animationLogs, params));
        if (isGraph) transformToNoGateways(animationData);
        return animationData;
    }

    private AnimationLog align(@NotNull LogAnimationService2.Log log) throws AnimationException {
        AnimationLog animationLog = new AnimationLog(log.xlog);
        animationLog.setFileName(log.fileName);
        animationLog.setName(log.xlog.getAttributes().get("concept:name").toString());
        animationLog.setDiagram(oldBpmnDiagramWithGateways);

        try {
            AlignmentClient alignmentClient = new AlignmentClient(new RestTemplate(), "http://54.217.90.168");
            AlignmentResult alignRes = alignmentClient.computeAlignment(bpmnDiagramWithGateways, log.xlog);
            for (ReplayResult rep : alignRes.getAlignmentResults()) {
                Set<Integer> alignTraces = rep.getTraceIndex();
                TraceAlignment traceAlignment = new TraceAlignment(rep.getStepTypes(), rep.getNodeInstances());
                for (int i : alignTraces) {
                    animationLog.add(log.xlog.get(i), createReplayTrace(log.xlog, i, traceAlignment));
                }
            }
            return animationLog;
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred during calling to aligment service. Please check system logs");
        }
    }

    private ReplayTrace createReplayTrace(@NotNull XLog log, int traceIndex, TraceAlignment traceAlignment) {
        XTrace trace = log.get(traceIndex);
        ReplayTrace replayTrace = new ReplayTrace(log.get(traceIndex), params);

        //Set up starting node
        TraceNode traceNode = new TraceNode(diagramHelper.getStartEvent());
        traceNode.setStart((new DateTime(LogUtility.getTimestamp(trace.get(0)))).minusSeconds(
                params.getStartEventToFirstEventDuration()));
        replayTrace.setStart(traceNode);
        replayTrace.addToReplayedList(diagramHelper.getStartEvent());
        TraceNode previousNode = traceNode;

        FlowNode modelNode = null;
        for (int i=0; i<traceAlignment.getNumberOfSteps(); i++) {
            if (traceAlignment.isMoveOnModel(i)) continue;

            // Add node
            traceNode = new TraceNode(diagramHelper.getNode(traceAlignment.getNodeId(i)));
            replayTrace.add(previousNode, traceNode);

            //Set timestamp
            if (traceNode.isActivity()) {
                if (traceAlignment.isMoveOnModel(i)) {
                    traceNode.setActivityMatched(true);
                    traceNode.setStart(new DateTime(LogUtility.getTimestamp(trace.get(traceAlignment.getEventIndex(i)))));
                } else {
                    traceNode.setActivitySkipped(true);
                }
            }

            replayTrace.addToReplayedList(traceNode.getModelNode());
            previousNode = traceNode;
        }

        //----------------------------------------------
        // Set timing for end event if it is connected to the last node in the replay trace
        // modelNode: point to the last node of the replay trace
        //----------------------------------------------
        if (diagramHelper.getTargets(modelNode).contains(diagramHelper.getEndEvent())) {
            traceNode = new TraceNode(diagramHelper.getEndEvent());
            traceNode.setStart((new DateTime(LogUtility.getTimestamp(trace.get(trace.size()-1)))).plusSeconds(
                    params.getLastEventToEndEventDuration()));
            replayTrace.add(previousNode, traceNode);
            replayTrace.addToReplayedList(diagramHelper.getEndEvent());
        }

        // Calculate timing for non-timing nodes
        if (!replayTrace.isEmpty()) {
            replayTrace.calcTiming();
        }

        return replayTrace;
    }

    private JSONObject createSetupData(List<AnimationLog> animationLogs, ReplayParams replayParams) {
        AnimationJSONBuilder2 jsonBuilder = new AnimationJSONBuilder2(animationLogs, replayParams);
        JSONObject setupData = jsonBuilder.parseLogCollection();
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }

    private Definitions convertToOldBPMNDiagram(BPMNDiagram bpmnDiagram) throws AnimationException {
        try {
            Definitions oldBpmnDiagram = BPMN2DiagramConverter.parseBPMN(getBPMN(bpmnDiagram, null), getClass().getClassLoader());
            BPMNDiagramHelper oldBpmnHelper = new BPMNDiagramHelper(oldBpmnDiagram);
            if (!oldBpmnHelper.isValidModel()) {
                throw new AnimationException("The BPMN diagram is not valid for animation.");
            }
            return oldBpmnDiagram;
        }
        catch (JAXBException ex) {
            throw new AnimationException("An internal error occurred during parsing BPMN XML. Please check system logs");
        }
    }

    //Convert from BPMNDiagram to string
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

    private void transformToNoGateways(AnimationData animationData) throws AnimationException {
        try {
            ElementIDMapper diagramMapping = new ElementIDMapper(oldBpmnDiagram);
            for (AnimationLog animationLog : animationData.getAnimationLogs()) {
                transformToNonGateways(animationLog, diagramMapping);
                animationLog.setDiagram(oldBpmnDiagram);
            }
        }
        catch (DiagramMappingException ex) {
            LOGGER.error(ex.getMessage());
            throw new AnimationException("An internal error occurred during animation result transformation. Please check system logs.");
        }
    }

    // The input animation log will be modified after the call to this method
    private void transformToNonGateways(AnimationLog diagramAnimationLog, ElementIDMapper diagramMapping) throws DiagramMappingException {
        for (ReplayTrace trace : diagramAnimationLog.getTraces()) {
            trace.convertToNonGateways();
            for (FlowNode node : trace.getNodes()) {
                String newId = diagramMapping.getId(node);
                if (newId.equals(ElementIDMapper.UNFOUND)) {
                    throw new DiagramMappingException("Couldn't find id for the node with name = " + node.getName());
                }
                node.setId(newId);
            }
            for (SequenceFlow flow : trace.getSequenceFlows()) {
                String newId = diagramMapping.getId(flow);
                if (newId.equals(ElementIDMapper.UNFOUND)) {
                    throw new DiagramMappingException("Couldn't find id for the sequence flow with name = " + flow.getName());
                }
                flow.setId(newId);
            }
        }
    }
}
