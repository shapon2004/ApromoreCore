/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2019 - 2021 Apromore Pty Ltd. All Rights Reserved.
 * %%
 * NOTICE:  All information contained herein is, and remains the
 * property of Apromore Pty Ltd and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to Apromore Pty Ltd and its suppliers and may
 * be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission
 * is obtained from Apromore Pty Ltd.
 * #L%
 */
package org.apromore.plugin.portal.processdiscoverer.animation;

import java.util.Arrays;

import org.apromore.plugin.portal.loganimation2.model.Frame;
import org.apromore.plugin.portal.loganimation2.model.Movie;
import org.json.JSONArray;

/**
 * <b>Stats</b> is used to contain or generate statistics for an animation movie {@link Movie}.
 * 
 * @author Bruce Nguyen
 *
 */
public class Stats {
    public static JSONArray computeCaseCountsJSON(Movie movie) {
        JSONArray json = new JSONArray();
        for (Frame frame : movie) {
            int totalCaseCount = Arrays.stream(frame.getLogIndexes()).map(i -> frame.getCaseIndexes(i).length).sum();
            json.put(totalCaseCount);
        }
        return json;
    }
}
