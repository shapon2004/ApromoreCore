package org.apromore.service.loganimation2.replay;

import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import de.hpi.bpmn2_0.transformation.BPMN2DiagramConverter;
import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.alignmentautomaton.ReplayResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.BpmnDefinitions;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.impl.AnimationException;
import org.apromore.service.loganimation2.impl.DiagramMappingException;
import org.apromore.service.loganimation2.impl.ElementIDMapper;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.apromore.service.loganimation2.utils.LogUtility;
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
 * <bold>LogAnimation</bold> is responsible for creating log animation data based on log-model alignment data.
 * The purpose is creating {@link AnimationData} from the alignment data. For each trace and its alignment with the model,
 * a concrete path on the model must be calculated with node IDs and sequence flow IDs, this path is used for running the
 * animation tokens along. In addition, timing must be calculated for each node on the path. This timing
 * must be based on the original timestamps of events in each trace to be accurate. For non-task nodes (e.g. gateways)
 * or unmatched task nodes, timing must be calculated to match the gateway semantics. For example, a token coming out
 * of an AND-split must spawn several tokens at the same timestamp, and these tokens coming to an AND-merge must
 * arrive at the same timestamp.
 *
 * @author Bruce Nguyen
 */
public class LogAnimation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimation.class);

    private final BPMNDiagram bpmnDiagramWithGateways;

    private final Definitions oldBpmnDiagram;
    private final Definitions oldBpmnDiagramWithGateways;

    private final BPMNDiagramHelper diagramHelper;
    private final AnimationParams params;
    private final boolean isGraph;

    public LogAnimation(@NotNull BPMNDiagram bpmnDiagram, @NotNull AnimationParams animationParams, boolean isGraph) throws AnimationException {
        this.oldBpmnDiagram = convertToOldBPMNDiagram(bpmnDiagram, !isGraph);

        // Log alignment only works on BPMN diagram with gateways
        this.bpmnDiagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(bpmnDiagram) : bpmnDiagram;
        this.oldBpmnDiagramWithGateways = isGraph ? convertToOldBPMNDiagram(bpmnDiagramWithGateways, false) : oldBpmnDiagram;

        diagramHelper = new BPMNDiagramHelper(oldBpmnDiagramWithGateways);
        this.params = animationParams;
        this.isGraph = isGraph;
    }

    public AnimationData createAnimation(@NotNull @NotEmpty List<LogAnimationService2.Log> logs) throws AnimationException {
        List<AnimationLog> animationLogs = logs.stream()
                .map(this::align)
                .collect(Collectors.toList());
        AnimationData animationData = new AnimationData(oldBpmnDiagramWithGateways, animationLogs,
                                                        createSetupData(animationLogs, params));
        if (isGraph) transformToNoGateways(animationData);
        return animationData;
    }

    private AnimationLog align(@NotNull LogAnimationService2.Log log) throws AnimationException {
        AnimationLog animationLog = new AnimationLog(log.xlog);
        animationLog.setFileName(log.fileName);
        animationLog.setName(log.xlog.getAttributes().get("concept:name").toString());

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
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }

    /**
     * Create a replay trace from a TraceAlignment result. A ReplayTrace represents the path on the model aligned
     * with the events in the trace. The timing at each node is also calculated.
     * @param log: the XLog
     * @param traceIndex: index of the trace in the log
     * @param traceAlignment: the alignment result of the trace
     * @return replay trace
     */
    private ReplayTrace createReplayTrace(@NotNull XLog log, int traceIndex, @NotNull TraceAlignment traceAlignment) {
        XTrace trace = log.get(traceIndex);
        ReplayTrace replayTrace = new ReplayTrace(log.get(traceIndex), params);

        //Set up starting node
        TraceNode traceNode = new TraceNode(diagramHelper.getStartEvent());
        traceNode.setStart((new DateTime(LogUtility.getTimestamp(trace.get(0)))).minusSeconds(
                params.getStartEventToFirstEventDuration()));
        replayTrace.setStart(traceNode);
        replayTrace.addToReplayedList(diagramHelper.getStartEvent());
        TraceNode previousNode = traceNode;

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

        //Set up ending node
        if (diagramHelper.getTargets(previousNode.getModelNode()).contains(diagramHelper.getEndEvent())) {
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

    /**
     * Create JSON setup data for the animation
     * @param animationLogs: list of AnimationLog objects
     * @param animationParams: replay parameters
     * @return JSONObject containing setup data
     */
    private JSONObject createSetupData(List<AnimationLog> animationLogs, AnimationParams animationParams) {
        AnimationJSONBuilder2 jsonBuilder = new AnimationJSONBuilder2(animationLogs, animationParams);
        JSONObject setupData = jsonBuilder.parseLogCollection();
        setupData.put("success", true);  // Ext2JS's file upload requires this flag
        return setupData;
    }

    private Definitions convertToOldBPMNDiagram(BPMNDiagram bpmnDiagram, boolean check) throws AnimationException {
        try {
            Definitions oldBpmnDiagram = BPMN2DiagramConverter.parseBPMN(getBPMN(bpmnDiagram), getClass().getClassLoader());
            BPMNDiagramHelper oldBpmnHelper = new BPMNDiagramHelper(oldBpmnDiagram);
            if (check && !oldBpmnHelper.isValidModel()) {
                throw new AnimationException("The BPMN diagram is not valid for animation.");
            }
            return oldBpmnDiagram;
        }
        catch (JAXBException ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in parsing BPMN XML. Please check system logs");
        }
    }

    //Convert from BPMNDiagram to string
    private String getBPMN(BPMNDiagram diagram) {
        BpmnDefinitions.BpmnDefinitionsBuilder definitionsBuilder = new BpmnDefinitions.BpmnDefinitionsBuilder(diagram);
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
        bpmnText = bpmnText.replaceAll("\n", "");
        return bpmnText;
    }

    /**
     * The alignment is done based on BPMN diagrams with gateways. This transformation is to transform the animation
     * data back to BPMN diagrams with no gateways (i.e. graph)
     * @param animationData AnimationData
     * @throws AnimationException when transformation has an issue
     */
    private void transformToNoGateways(AnimationData animationData) throws AnimationException {
        try {
            ElementIDMapper diagramMapping = new ElementIDMapper(oldBpmnDiagram);
            for (AnimationLog animationLog : animationData.getAnimationLogs()) {
                transformToNoGateways(animationLog, diagramMapping);
            }
        }
        catch (DiagramMappingException ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in transforming animation data to graph format. Please check system logs.");
        }
    }

    /**
     * Transform each AnimationLog to using graph-based BPMN Diagram.
     * For each ReplayTrace in the AnimationLog, this transformation perform:
     *  - Remove all gateway nodes in the replay trace
     *  - Update the node IDs and sequence flow IDs to be the ones from the input diagram mapping
     * @param animationLog: Animation Log
     * @param diagramMapping: mapping from node names to node IDs, this mapping is created from the graph-based BPMN diagram
     * @throws DiagramMappingException when a node ID is not found for a node name (something wrong with the replay trace or mapping)
     */
    private void transformToNoGateways(AnimationLog animationLog, ElementIDMapper diagramMapping) throws DiagramMappingException {
        for (ReplayTrace trace : animationLog.getTraces()) {
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
