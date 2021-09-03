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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apromore.alignmentautomaton.AlignmentResult;
import org.apromore.alignmentautomaton.api.FileStoreResponse;
import org.apromore.alignmentautomaton.api.RESTEndpointsConfig;
import org.apromore.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.apromore.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XesXmlSerializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST client for the trace alignment micro-service
 */
public class AlignmentClient {

  public static final String BPMN_SUFFIX = ".bpmn";

  private static final String XES_SUFFIX = ".xes";

  private final RestTemplate restTemplate;

  private final String apiURI;

  public AlignmentClient(RestTemplate restTemplate, String apiURI) {
    this.apiURI = apiURI;
    this.restTemplate = restTemplate;
  }

  public AlignmentResult computeAlignment(BPMNDiagram bpmn, XLog xLog) {
    String modelName = UUID.randomUUID() + ".bpmn";
    String logName = UUID.randomUUID() + ".xes";
    return computeAlignment(bpmn, xLog, modelName, logName);
  }

  public AlignmentResult computeAlignment(BPMNDiagram bpmn, XLog xLog, String modelName, String logName) {

    if (!modelName.endsWith(BPMN_SUFFIX)) {
      throw new IllegalArgumentException("Model name must end with " + BPMN_SUFFIX);
    }

    if (!logName.endsWith(XES_SUFFIX)) {
      throw new IllegalArgumentException("Log name must end with " + XES_SUFFIX);
    }

    try {
      URI bpURI = UriComponentsBuilder.fromHttpUrl(apiURI + RESTEndpointsConfig.BPMN_UPLOAD_PATH)
          .queryParam("fileName", modelName).build().encode().toUri();
      URI lURI = UriComponentsBuilder.fromHttpUrl(apiURI + RESTEndpointsConfig.XES_UPLOAD_PATH)
          .queryParam("fileName", logName).build().encode().toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_XML);

      HttpEntity<String> logRequest = new HttpEntity<>(serLog(xLog), headers);
      HttpEntity<String> bpmnRequest = new HttpEntity<>(serBPMN(bpmn), headers);

      restTemplate.postForEntity(bpURI, bpmnRequest, FileStoreResponse.class).getBody();
      restTemplate.postForObject(lURI, logRequest, FileStoreResponse.class);

      URI reqURI = UriComponentsBuilder.fromHttpUrl(apiURI + RESTEndpointsConfig.ALIGNMENT_PATH)
          .queryParam("xesFileName", logName).queryParam("modelFileName", modelName).build().encode().toUri();

      return restTemplate.postForObject(reqURI, "", AlignmentResult.class);
    } catch (IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private String serLog(XLog xLog) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    new XesXmlSerializer().serialize(xLog, os);
    return os.toString(StandardCharsets.UTF_8);
  }

  private String serBPMN(BPMNDiagram diagram) throws IOException {
    Path tempFile = Files.createTempFile("aprom", ".bpmn");
    new BpmnExportPlugin().export(diagram, tempFile.toFile());
    return Files.readString(tempFile);
  }
}
