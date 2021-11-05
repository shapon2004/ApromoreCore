package org.apromore.service.loganimation2.enablement;

import org.apromore.alignmentautomaton.EnablementResult;
import org.apromore.alignmentautomaton.client.AlignmentClient;
import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.AnimationException;
import org.apromore.service.loganimation2.impl.LogAnimationServiceImpl2;
import org.apromore.service.loganimation2.utils.BPMNDiagramHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AttributeLogAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimationServiceImpl2.class);
    private static final AlignmentClient alignmentServiceClient = new AlignmentClient("");

    public static AttributeLogEnablement align(@NotNull AttributeLog log, BPMNDiagram diagram, boolean isGraph) throws AnimationException {
        BPMNDiagram diagramWithGateways = isGraph ? BPMNDiagramHelper.createBPMNDiagramWithGateways(diagram) : diagram;
        try {
            Map<String, List<EnablementResult>> enablements = alignmentServiceClient.computeEnablement(diagramWithGateways,
                    "",
                    ApromLogAdapter.adaptLog(log),
                    ApromLogAdapter.adaptCaseVariants(log),
                    Collections.emptyMap());
            if (isGraph) enablements = TaskEnablementConverter.convertLogEnablement(enablements, diagram);
            return new AttributeLogEnablement(log, enablements);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new AnimationException("An internal error occurred in calling to alignment service. Please check system logs");
        }
    }
}
