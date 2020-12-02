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
import org.apromore.plugin.portal.loganimation2.model.Frame;
import org.apromore.plugin.portal.loganimation2.model.FrameRecorder;
import org.apromore.plugin.portal.loganimation2.model.Movie;
import org.apromore.service.loganimation2.replay.AnimationLog;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


public class FrameTest extends TestDataSetup {
    protected Movie createAnimationMovie_OneTraceAndCompleteEvents_Graph() throws Exception {
        AnimationLog animationLog = this.animate_OneTraceAndCompleteEvents_Graph();
        AnimationContext animationContext = new AnimationContext(animationLog, 60, 600);
        AnimationIndex animationIndex = new AnimationIndex(animationLog, animationContext);
        return FrameRecorder.record(animationLog, animationIndex, animationContext);
    }
    
    protected Movie createAnimationMovie_TwoTraceAndCompleteEvents_Graph() throws Exception {
        AnimationLog animationLog = this.animate_TwoTraceAndCompleteEvents_Graph();
        AnimationContext animationContext = new AnimationContext(animationLog, 60, 600);
        AnimationIndex animationIndex = new AnimationIndex(animationLog, animationContext);
        return FrameRecorder.record(animationLog, animationIndex, animationContext);
    }
    
    @Test
    public void test_FrameData_OneTraceLog() throws Exception {
        Movie animationMovie = createAnimationMovie_OneTraceAndCompleteEvents_Graph();
        
        Frame frame0 = animationMovie.get(0);
        Assert.assertEquals(0, frame0.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame0.getCaseIndexes());
        Assert.assertArrayEquals(new int[] {13}, frame0.getElementIndexes());
        Assert.assertArrayEquals(new int[] {0}, frame0.getTokenIndexes());
        Assert.assertArrayEquals(new int[] {}, frame0.getTokenIndexesByElement(0));
        Assert.assertArrayEquals(new int[] {0}, frame0.getTokenIndexesByElement(13));
        
        Frame frame299 = animationMovie.get(299);
        Assert.assertEquals(299, frame299.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame299.getCaseIndexes());
        Assert.assertArrayEquals(new int[] {13}, frame299.getElementIndexes());
        Assert.assertArrayEquals(new int[] {0}, frame299.getTokenIndexes());
        Assert.assertArrayEquals(new int[] {}, frame299.getTokenIndexesByElement(0));
        Assert.assertArrayEquals(new int[] {0}, frame299.getTokenIndexesByElement(13));
       
        Frame frame35999 = animationMovie.get(35999);
        Assert.assertEquals(35999, frame35999.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame35999.getCaseIndexes());
        Assert.assertArrayEquals(new int[] {11}, frame35999.getElementIndexes());
        Assert.assertArrayEquals(new int[] {3}, frame35999.getTokenIndexes());
        Assert.assertArrayEquals(new int[] {}, frame35999.getTokenIndexesByElement(0));
        Assert.assertArrayEquals(new int[] {3}, frame35999.getTokenIndexesByElement(11));
    }
    
    @Test
    public void test_FrameJSON_OneTraceLog() throws Exception {
        Movie animationMovie = createAnimationMovie_OneTraceAndCompleteEvents_Graph();
        
        JSONObject frame0 = animationMovie.get(0).getJSON();
        JSONObject frame0Expect = this.readFrame_OneTraceAndCompleteEvents(0);
        Assert.assertEquals(true, frame0Expect.similar(frame0));
        
        JSONObject frame299 = animationMovie.get(299).getJSON();
        JSONObject frame299Expect = this.readFrame_OneTraceAndCompleteEvents(299);
        Assert.assertEquals(true, frame299Expect.similar(frame299));
        
        JSONObject frame35990 = animationMovie.get(35990).getJSON();
        JSONObject frame35990Expect = this.readFrame_OneTraceAndCompleteEvents(35990);
        Assert.assertEquals(true, frame35990Expect.similar(frame35990));
        
        JSONObject frame35999 = animationMovie.get(35999).getJSON();
        JSONObject frame35999Expect = this.readFrame_OneTraceAndCompleteEvents(35999);
        Assert.assertEquals(true, frame35999Expect.similar(frame35999));
    }
}
