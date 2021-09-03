/**
 * #%L
 * This file is part of "Apromore Enterprise Edition".
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
package org.apromore.alignmentautomaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ReplayResult {

  @NonNull
  private List<String> nodeInstances = new ArrayList<>();

  @NonNull
  private List<StepType> stepTypes = new ArrayList<>();

  @NonNull
  private SortedSet<Integer> traceIndex = new TreeSet<>();

  private boolean reliable = false;

  @NonNull
  private Map<String, Double> info = new HashMap<>();

  @NonNull
  private List<Map<String, Double>> singleInfoLst = new ArrayList<>();

  public ReplayResult(List<String> nodeInstances, List<StepType> stepTypes, int traceIndex, boolean reliable) {
    this(nodeInstances, stepTypes, new TreeSet<>(), reliable, new HashMap<>(), new ArrayList<>());
    addNewCase(traceIndex);
  }

  public ReplayResult(List<String> nodeInstances, List<StepType> stepTypes, SortedSet<Integer> traceIndex,
      boolean reliable, Map<String, Double> info, List<Map<String, Double>> singleInfoLst) {
    this.nodeInstances = nodeInstances;
    this.stepTypes = stepTypes;
    this.traceIndex = traceIndex;
    this.reliable = reliable;
    this.info = info;
    this.singleInfoLst = singleInfoLst;
  }

  public void addNewCase(int traceIndex) {
    this.traceIndex.add(traceIndex);
  }

  public void addSingleInfo(Map<String, Double> singleInfo) {
    this.singleInfoLst.add(singleInfo);
  }
}
