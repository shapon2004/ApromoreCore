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

package org.apromore.service.loganimation2.impl;

import org.apromore.logman.attribute.log.AttributeLog;
import org.apromore.plugin.DefaultParameterAwarePlugin;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.service.loganimation2.LogAnimationService2;
import org.apromore.service.loganimation2.ParamsReader;
import org.apromore.service.loganimation2.enablement.AttributeLogAligner;
import org.apromore.service.loganimation2.enablement.CompositeLogEnablement;
import org.apromore.service.loganimation2.recording.Frame;
import org.apromore.service.loganimation2.recording.Movie;
import org.apromore.service.loganimation2.recording.TokenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("logAnimationService2")
@Qualifier("logAnimationService2")
public class LogAnimationServiceImpl2 extends DefaultParameterAwarePlugin implements LogAnimationService2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnimationServiceImpl2.class);
    private AttributeLogAligner aligner;

    public LogAnimationServiceImpl2() {
        aligner = new AttributeLogAligner();
    }

    /**
     * Create animation movie from a {@link BPMNDiagram} and a list of {@link AttributeLog}s. Each movie is a sequence of
     * {@link Frame}s.
     * <p>
     * The data transformation is: the diagram and logs are aligned to have list of EnablementResult, one for each log,
     * which is bundled as a {@link CompositeLogEnablement}, which is then converted to a list of {@link TokenMap},
     * one for each log, which is then used to create frames in the movie.
     * </p>
     * @param diagram the diagram
     * @param logs the list of logs
     * @return movie
     */
    @Override
    public Movie createAnimationMovie(BPMNDiagram diagram, List<AttributeLog> logs) {
        return new Movie(aligner.createEnablement(diagram, logs), ParamsReader.createAnimationParams(logs));
    }

    /**
     * Similar to {@link #createAnimationMovie(BPMNDiagram, List)} but the diagram is a graph.
     * @param diagram a graph diagram (without gateways)
     * @param logs the list of logs
     * @return movie
     */
    @Override
    public Movie createAnimationMovieForGraph(BPMNDiagram diagram, List<AttributeLog> logs) {
        return new Movie(aligner.createEnablementForGraph(diagram, logs), ParamsReader.createAnimationParams(logs));
    }
}
