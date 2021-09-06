package org.apromore.service.loganimation.replay;

import de.hpi.bpmn2_0.model.Definitions;
import de.hpi.bpmn2_0.model.FlowNode;
import de.hpi.bpmn2_0.model.connector.SequenceFlow;
import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.alignmentautomaton.ReplayResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.utils.BPMNDiagramHelper;
import org.apromore.service.loganimation.utils.LogUtility;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.joda.time.DateTime;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LogAlignment {
    private BPMNDiagram bpmnDiagram;
    private Definitions oldBpmnDiagram;
    private BPMNDiagramHelper diagramHelper;
    private ReplayParams params;

    public LogAlignment(BPMNDiagram bpmnDiagram, Definitions oldBpmnDiagram, ReplayParams replayParams) {
        this.bpmnDiagram = bpmnDiagram;
        this.oldBpmnDiagram = oldBpmnDiagram;
        diagramHelper = new BPMNDiagramHelper(oldBpmnDiagram);
        this.params = replayParams;
    }

    public List<AnimationLog> align(List<LogAnimationService2.Log> logs) {
        return logs.stream()
                    .map(log -> align(log))
                    .collect(Collectors.toList());
    }

    private AnimationLog align(LogAnimationService2.Log log) {
        AnimationLog animationLog = new AnimationLog(log.xlog);
        animationLog.setFileName(log.fileName);
        animationLog.setName(log.xlog.getAttributes().get("concept:name").toString());
        animationLog.setDiagram(oldBpmnDiagram);

        AlignmentClient alignmentClient = new AlignmentClient(new RestTemplate(), "http://54.217.90.168");
        AlignmentResult alignRes = alignmentClient.computeAlignment(bpmnDiagram, log.xlog);
        for (ReplayResult rep : alignRes.getAlignmentResults()) {
            Set<Integer> alignTraces = rep.getTraceIndex();
            TraceAlignment traceAlignment = new TraceAlignment(rep.getStepTypes(), rep.getNodeInstances());
            for (int i : alignTraces) {
                animationLog.add(log.xlog.get(i), createReplayTrace(log.xlog, i, traceAlignment));
            }
        }
        return animationLog;
    }

    private ReplayTrace createReplayTrace(XLog log, int traceIndex, TraceAlignment traceAlignment) {
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
            //---------------------------------------
            //Add normal node to the replay trace
            //---------------------------------------
            modelNode = diagramHelper.getNode(traceAlignment.getNodeId(i));
            traceNode = new TraceNode(modelNode);
            SequenceFlow flow = diagramHelper.getSequenceFlow(previousNode.getName(), traceNode.getName());
            replayTrace.add((FlowNode)flow.getSourceRef(), traceNode);

            //Set timestamp
            if (traceNode.isActivity()) {
                if (traceAlignment.isActivityMatched(i)) {
                    traceNode.setActivityMatched(true);
                    traceNode.setStart(new DateTime(LogUtility.getTimestamp(trace.get(traceAlignment.getEventIndex(i)))));
                } else {
                    traceNode.setActivitySkipped(true);
                }
            }

            replayTrace.addToReplayedList(modelNode);
        }

        //----------------------------------------------
        // Set timing for end event if it is connected to the last node in the replay trace
        // modelNode: point to the last node of the replay trace
        //----------------------------------------------
        if (diagramHelper.getTargets(modelNode).contains(diagramHelper.getEndEvent())) {
            traceNode = new TraceNode(diagramHelper.getEndEvent());
            traceNode.setStart((new DateTime(LogUtility.getTimestamp(trace.get(trace.size()-1)))).plusSeconds(
                    params.getLastEventToEndEventDuration()));
            replayTrace.add(modelNode, traceNode);
            replayTrace.addToReplayedList(diagramHelper.getEndEvent());
        }

        if (!replayTrace.isEmpty()) replayTrace.calcTiming();

        return replayTrace;
    }
}
