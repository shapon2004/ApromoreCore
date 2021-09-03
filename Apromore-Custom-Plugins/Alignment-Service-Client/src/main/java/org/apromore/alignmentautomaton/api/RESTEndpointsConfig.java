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
package org.apromore.alignmentautomaton.api;

public final class RESTEndpointsConfig {

  public static final String BASE = "/api";

  public static final String INPUT_FILE_UPLOAD_PATH = BASE + "/files";

  public static final String XES_UPLOAD_PATH = INPUT_FILE_UPLOAD_PATH + "-xes";

  public static final String BPMN_UPLOAD_PATH = INPUT_FILE_UPLOAD_PATH + "-bpmn";

  public static final String ALIGNMENT_PATH = BASE + "/alignment";
}
