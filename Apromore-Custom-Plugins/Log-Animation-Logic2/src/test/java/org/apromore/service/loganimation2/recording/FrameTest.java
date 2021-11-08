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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class FrameTest extends MovieTest {
    @Test
    public void test_FrameData_OneTraceLog() throws Exception {
        Movie animationMovie = this.createMovie_d1_1trace();
        
        Frame frame0 = animationMovie.get(0);
        Assert.assertEquals(0, frame0.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame0.getCaseIndexes(0));
        Assert.assertArrayEquals(new int[] {13}, frame0.getElementIndexes(0));
        Assert.assertArrayEquals(new int[] {0}, frame0.getTokens(0));
        Assert.assertArrayEquals(new int[] {}, frame0.getTokensByElement(0,0));
        Assert.assertArrayEquals(new int[] {0}, frame0.getTokensByElement(0, 13));

        Frame frame299 = animationMovie.get(299);
        Assert.assertEquals(299, frame299.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame299.getCaseIndexes(0));
        Assert.assertArrayEquals(new int[] {13}, frame299.getElementIndexes(0));
        Assert.assertArrayEquals(new int[] {0}, frame299.getTokens(0));
        Assert.assertArrayEquals(new int[] {}, frame299.getTokensByElement(0,0));
        Assert.assertArrayEquals(new int[] {0}, frame299.getTokensByElement(0,13));

        Frame frame35999 = animationMovie.get(35999);
        Assert.assertEquals(35999, frame35999.getIndex());
        Assert.assertArrayEquals(new int[] {0}, frame35999.getCaseIndexes(0));
        Assert.assertArrayEquals(new int[] {11}, frame35999.getElementIndexes(0));
        Assert.assertArrayEquals(new int[] {3}, frame35999.getTokens(0));
        Assert.assertArrayEquals(new int[] {}, frame35999.getTokensByElement(0,0));
        Assert.assertArrayEquals(new int[] {3}, frame35999.getTokensByElement(0,11));
    }
    
    @Test
    public void test_FrameJSON_OneTraceLog() throws Exception {
        Movie animationMovie = this.createMovie_d1_1trace();
        
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
    
    @Test
    public void test_FrameData_TwoLogs() throws Exception {
        Movie animationMovie = this.createMovie_d1_1trace();
        
        // This frame only has one token for the 2nd log
        Frame firstFrame = animationMovie.get(0);
        Assert.assertEquals(0, firstFrame.getIndex());
        Assert.assertEquals(true, firstFrame.getTokens(0).length > 0);
        Assert.assertEquals(true, firstFrame.getTokens(1).length == 0); // no token for the 2nd log in this frame
        
        // This frame has one token for both logs
        Frame frameTwoTokens = animationMovie.get(19036);
        Assert.assertEquals(19036, frameTwoTokens.getIndex());
        Assert.assertEquals(true, frameTwoTokens.getTokens(0).length > 0);
        Assert.assertEquals(true, frameTwoTokens.getTokens(1).length > 0);
       
        // This frame only has one token for the 1st log
        Frame lastFrame = animationMovie.get(35999);
        Assert.assertEquals(35999, lastFrame.getIndex());
        Assert.assertEquals(true, lastFrame.getTokens(1).length > 0);
        Assert.assertEquals(true, lastFrame.getTokens(0).length == 0); // no token for the 1st log in this frame
    }
    
    @Test
    public void test_FrameJSON_TwoLogs() throws Exception {
        Movie animationMovie = this.createMovie_d1_1trace();
        
        JSONObject testFrame = animationMovie.get(19036).getJSON();
        JSONObject expectedFrame = this.readFrame_TwoLogs(19036);
        Assert.assertEquals(true, expectedFrame.similar(testFrame));
    }
}
