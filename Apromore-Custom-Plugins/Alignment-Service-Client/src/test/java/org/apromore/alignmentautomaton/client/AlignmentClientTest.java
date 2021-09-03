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
package org.apromore.alignmentautomaton.client;

import java.io.File;
import java.io.FileInputStream;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import com.google.common.io.Resources;
import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.alignmentautomaton.api.RESTEndpointsConfig;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnImportPlugin;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class AlignmentClientTest {

  public static final String XES = "simple.xes";

  public static final String BPMN = "simple.bpmn";

  @BeforeEach
  public void setUp() throws Exception {
  }

  @AfterEach
  public void tearDown() throws Exception {
  }

  @Test
  public void computeAlignment() throws Exception {

    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    String apiURL = "http://fake-host";
    AlignmentClient alignmentClient = new AlignmentClient(restTemplate, apiURL);

    mockServer.expect(requestTo(apiURL + RESTEndpointsConfig.BPMN_UPLOAD_PATH + "?fileName=model1.bpmn"))
        .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}", APPLICATION_JSON));
    mockServer.expect(requestTo(apiURL + RESTEndpointsConfig.XES_UPLOAD_PATH + "?fileName=log1.xes"))
        .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}", APPLICATION_JSON));
    mockServer.expect(
        requestTo(apiURL + RESTEndpointsConfig.ALIGNMENT_PATH + "?xesFileName=log1.xes&modelFileName=model1.bpmn"))
        .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}", APPLICATION_JSON));

    File xes = new File(Resources.getResource("fixtures/" + XES).getFile());
    File modelFile = new File(Resources.getResource("fixtures/" + BPMN).getFile());

    XLog xLog = importEventLog(xes);
    BPMNDiagram bpmn = new BpmnImportPlugin().importFromStreamToDiagram(new FileInputStream(modelFile), BPMN);
    AlignmentResult alignmentResult = alignmentClient.computeAlignment(bpmn, xLog, "model1.bpmn", "log1.xes");
  }

  private static XLog importEventLog(File file) throws Exception {
    XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
    if (!parser.canParse(file)) {
      parser = new XesXmlGZIPParser();
      if (!parser.canParse(file)) {
        throw new IllegalArgumentException("Unparsable log file: " + file.getAbsolutePath());
      }
    }

    return parser.parse(file).get(0);
  }

  /**
   * This test will run a trace alignment in a real server. The API URL needs to be
   * set accordingly
   */
  @Test
  @Disabled
  public void computeAlignmentRealServer() throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    AlignmentClient alignmentClient = new AlignmentClient(restTemplate, "http://localhost:1112");

    File xes = new File(Resources.getResource("fixtures/" + XES).getFile());
    File modelFile = new File(Resources.getResource("fixtures/" + BPMN).getFile());

    XLog xLog = importEventLog(xes);
    BPMNDiagram bpmn = new BpmnImportPlugin().importFromStreamToDiagram(new FileInputStream(modelFile), BPMN);
    AlignmentResult alignmentResult = alignmentClient.computeAlignment(bpmn, xLog);
  }
}