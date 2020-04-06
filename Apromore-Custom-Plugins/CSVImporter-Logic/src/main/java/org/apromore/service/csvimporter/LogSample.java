/*
 * This file is part of "Apromore".
 *
 * Copyright (C) 2019 - 2020 The University of Melbourne.
 * Copyright (C) 2019 - 2020 The University of Tartu.
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.apromore.service.csvimporter;

import org.zkoss.zul.Window;

import java.util.List;
import java.util.Map;

public interface LogSample {

    String getCaseIdLabel();
    String getActivityLabel();
    String getTimestampLabel();
    String getStartTimestampLabel();
    String getOtherTimestampLabel();
    String getResourceLabel();

    boolean isParsable(int colPos);
    boolean isParsableWithFormat(int colPos, String format);

    List<String> getHeader();
    List<List<String>> getLines();
    Map<String, Integer> getMainAttributes();
    String getTimestampFormat();
    void setTimestampFormat(String s);
    String getStartTsFormat();
    void setStartTsFormat(String s);
    List<Integer> getIgnoredPos();
    Map<Integer, String> getOtherTimeStampsPos();
    List<Integer> getCaseAttributesPos();
    void setIgnoreAll(Window window);
    void setOtherAll(Window window);
    void setOtherTimestamps();



}
