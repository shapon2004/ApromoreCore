/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.apromore.service.loganimation.impl;

// Java 2 Standard Edition
import java.util.List;

        import org.apromore.plugin.DefaultParameterAwarePlugin;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation.LogAnimationService2;
import org.apromore.service.loganimation.replay.*;
//import org.apromore.processmining.plugins.signaturediscovery.encoding.EncodeTraces;
//import org.apromore.processmining.plugins.signaturediscovery.encoding.EncodingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("logAnimationService2")
@Qualifier("logAnimationService2")
public class LogAnimationServiceImpl2 extends DefaultParameterAwarePlugin implements LogAnimationService2 {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimationServiceImpl2.class);

    @Override
    public AnimationData createAnimation(BPMNDiagram bpmnDiagram, List<Log> logs) throws AnimationException {
        LogProcessor.cleanLogs(logs);
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateAnimation(bpmnDiagram, logs, animationParams, false);
    }

    @Override
    public AnimationData createAnimationForGraph(BPMNDiagram bpmnDiagramNoGateways, List<Log> logs)
            throws AnimationException {
        LogProcessor.cleanLogs(logs);
        AnimationParams animationParams = ParamsReader.createAnimationParams(logs);
        return doCreateAnimation(bpmnDiagramNoGateways, logs, animationParams, true);
    }

    private AnimationData doCreateAnimation(BPMNDiagram bpmnDiagram, List<Log> logs, AnimationParams params, boolean isGraph) throws AnimationException {
        return (new LogAnimation(bpmnDiagram, params, isGraph)).createAnimation(logs);
    }

}
