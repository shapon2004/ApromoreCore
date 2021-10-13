package org.apromore.service.loganimation2.enablement;

import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagramMapping;
import org.apromore.service.loganimation2.impl.AnimationException;
import org.apromore.service.loganimation2.json.AnimationJSONBuilder2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
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
    private final BPMNDiagram diagram;
    private final BPMNDiagram diagramWithGateways;
    private final AnimationParams params;
    private final boolean isGraph;

    public LogAnimation(@NotNull BPMNDiagram diagram, @NotNull AnimationParams animationParams, boolean isGraph) throws AnimationException {
        this.diagram = diagram;
        this.diagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram) : diagram;
        this.params = animationParams;
        this.isGraph = isGraph;
    }

    public AnimationData createAnimation(@NotNull @NotEmpty List<AttributeLog> logs) throws AnimationException {
        List<EnablementLog> enablmentLogs = logs.stream()
                .map(this::align)
                .collect(Collectors.toList());
        AnimationData animationData = new AnimationData(enablmentLogs, params);
        return animationData;
    }

    private EnablementLog align(@NotNull AttributeLog log) throws AnimationException {
        try {
            AlignmentClient alignmentClient = new AlignmentClient(new RestTemplate(), "http://54.217.90.168");
            Map<String, List<EnablementResult>> enablements = alignmentClient.computeEnablement(diagramWithGateways,
                    "",
                    ApromLogAdapter.adaptLog(log),
                    ApromLogAdapter.adaptCaseVariants(log),
                    Collections.<String, Long>emptyMap());
            if (isGraph) enablements = convertEnablements(enablements);
            return new EnablementLog(log, enablements);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }

    private Map<String, List<EnablementResult>> convertEnablements(Map<String, List<EnablementResult>> enablements) {
        Map<String, String> taskNodeMapping = getTaskNodeIDMapping(diagram, diagramWithGateways);
        BPMNDiagramMapping diagramMapping = new BPMNDiagramMapping(diagram);
        Map<String, List<EnablementResult>> newEnablements = new HashMap<>();
        for (Map.Entry<String, List<EnablementResult>> caseEnablement : enablements.entrySet()) {
            String caseID = caseEnablement.getKey();
            List<EnablementResult> newCaseEnablement = Collections.emptyList();
            List<EnablementResult> taskEnablements = caseEnablement.getValue()
                                                            .stream()
                                                            .filter(e -> taskNodeMapping.keySet().contains(e.getElementId()))
                                                            .collect(Collectors.toList());
            if (taskEnablements.size() == 1) newCaseEnablement.addAll(taskEnablements);
            for (int i=1; i<taskEnablements.size(); i++) {
                newCaseEnablement.add(taskEnablements.get(i - 1));
                String sourceId = taskNodeMapping.get(taskEnablements.get(i - 1).getElementId());
                String targetId = taskNodeMapping.get(taskEnablements.get(i).getElementId());
                String edgeId = diagramMapping.getEdgeId(sourceId, targetId);
                newCaseEnablement.add(new EnablementResult(edgeId,
                        taskEnablements.get(i - 1).getEnablementTimestamp(),
                        taskEnablements.get(i - 1).getEnablementTimestamp(),
                        true, false));
                if (i == (taskEnablements.size()-1)) newCaseEnablement.add(taskEnablements.get(i));
            }
            newEnablements.put(caseID, newCaseEnablement);
        }
        return newEnablements;
    }

    /**
     * Get task node ID mapping from otherDiagram  to diagram
     * @param diagram
     * @param otherDiagram
     * @return mapping
     */
    private Map<String,String> getTaskNodeIDMapping(BPMNDiagram diagram, BPMNDiagram otherDiagram) {
        BPMNDiagramMapping diagramMapping = new BPMNDiagramMapping(diagram);
        BPMNDiagramMapping otherDiagramMapping = new BPMNDiagramMapping(otherDiagram);
        Map<String,String> mapping = new HashMap<>();
        otherDiagramMapping.getNodeLabels().forEach(label -> {
            String id = diagramMapping.getNodeIdFromLabel(label);
            String otherId = otherDiagramMapping.getNodeIdFromLabel(label);
            if (!id.isEmpty() && !otherId.isEmpty()) mapping.put(otherId, id);
        });
        return mapping;
    }
}
