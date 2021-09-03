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

public enum StepType {
  LMGOOD("Sync move"),
  LMNOGOOD("False sync move"),
  L("Log move"),
  MINVI("Invisible step"),
  MREAL("Model move"),
  LMREPLACED("Replaced step"),
  LMSWAPPED("Swapped step");

  private final String descr;

  StepType(String descr) {
    this.descr = descr;
  }

  public String toString() {
    return this.descr;
  }

}
