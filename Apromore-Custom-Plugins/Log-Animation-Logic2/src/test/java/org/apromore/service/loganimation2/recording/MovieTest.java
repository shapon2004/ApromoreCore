/*-
 * #%L
 * This file is part of "Apromore Core".
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
package org.apromore.service.loganimation2.recording;

import java.util.List;

import org.apromore.service.loganimation2.recording.*;
import org.apromore.service.loganimation2.data.AnimationData;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

public class MovieTest extends TestDataSetup {
    
    @Test
    public void test_getChunkJSON_OneLog() throws Exception {
        AnimationData result = this.animate_OneTraceAndCompleteEvents_BPMNDiagram();
        AnimationContext animationContext = new AnimationContext(result.getStartTimestamp(), result.getEndTimestamp(), 60, 600);
        AnimationIndex animationIndex = new AnimationIndex(result.getEnablementLogs().get(0), result, animationContext);
        Movie movie = FrameRecorder.record(List.of(animationIndex), animationContext);
        
        Assert.assertEquals(36000, movie.size());
        
        JSONArray firstChunk = movie.getChunkJSON(0, 300); // first chunk
        JSONArray firstChunkExpect = this.readChunk_OneTraceAndCompleteEvents(0);
        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
        
        JSONArray lastChunk = movie.getChunkJSON(35817, 300); // last chunk
        JSONArray lastChunkExpect = this.readChunk_OneTraceAndCompleteEvents(35817);
        Assert.assertEquals(true, lastChunk.similar(lastChunkExpect));
    }
    
    @Test
    public void test_getChunkJSON_TwoLogs() throws Exception {
        AnimationData result = this.animate_TwoLogs_With_BPMNDiagram();
        AnimationContext animationContext = new AnimationContext(result.getStartTimestamp(), result.getEndTimestamp(), 60, 600);
        Movie movie = FrameRecorder.record(createAnimationIndexes(result, animationContext), animationContext);
        
        Assert.assertEquals(36000, movie.size());
        
        JSONArray firstChunk = movie.getChunkJSON(0, 300);
        JSONArray firstChunkExpect = this.readChunk_TwoLogs(0);
        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
        
        JSONArray lastChunk = movie.getChunkJSON(35744, 300);
        JSONArray lastChunkExpect = this.readChunk_TwoLogs(0);
        Assert.assertEquals(true, firstChunk.similar(firstChunkExpect));
    }
}
