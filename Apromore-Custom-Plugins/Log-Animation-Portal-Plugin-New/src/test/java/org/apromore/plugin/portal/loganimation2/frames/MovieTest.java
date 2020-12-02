/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
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
package org.apromore.plugin.portal.loganimation2.frames;

import org.apromore.plugin.portal.loganimation2.model.AnimationContext;
import org.apromore.plugin.portal.loganimation2.model.AnimationIndex;
import org.apromore.plugin.portal.loganimation2.model.FrameRecorder;
import org.apromore.plugin.portal.loganimation2.model.Movie;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.json.JSONArray;
import org.junit.Test;

import junit.framework.Assert;

public class MovieTest extends TestDataSetup {
    
    @Test
    public void test_getChunkJSON() throws Exception {
        AnimationLog animationLog = this.animate_OneTraceAndCompleteEvents_Graph();
        AnimationContext animationContext = new AnimationContext(animationLog, 60, 600);
        AnimationIndex animationIndex = new AnimationIndex(animationLog, animationContext);
        Movie movie = FrameRecorder.record(animationLog, animationIndex, animationContext);
        
        Assert.assertEquals(36000, movie.size());
        
        JSONArray chunk0 = movie.getChunkJSON(0, 300); // first chunk
        JSONArray chunk0Expect = this.readChunk_OneTraceAndCompleteEvents(0);
        Assert.assertEquals(true, chunk0.similar(chunk0Expect));
        
        JSONArray chunk300 = movie.getChunkJSON(300, 300); // second chunk
        JSONArray chunk300Expect = this.readChunk_OneTraceAndCompleteEvents(300);
        Assert.assertEquals(true, chunk300.similar(chunk300Expect));
    }
}
