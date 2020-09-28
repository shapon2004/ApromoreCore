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
package org.apromore.plugin.portal.bimp.client;


import org.apromore.plugin.portal.bimp.model.SimulationRequest;
import org.apromore.plugin.portal.bimp.model.SimulationResponse;
import org.zkoss.zul.Filedownload;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BIMPClientImpl implements BIMPClient {

    @Override
    public SimulationResponse postSimulation(SimulationRequest simulationRequestBody) throws IOException {
        URL url = new URL("https://api-test.qbp-simulator.com/rest/v1/Simulation");
        SimulationResponse response = new SimulationResponse();

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/xml");
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Authorization", "Bearer");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = simulationRequestBody.getBody().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                stringBuilder.append(responseLine.trim());
            }

            response.setCode(con.getResponseCode());
            response.setBody(stringBuilder.toString());
        }

        return response;
    }

    @Override
    public InputStream getSimulationMXMLLogs(String simulationId) throws IOException {
        URL url = new URL(String.format("https://api-test.qbp-simulator.com/rest/v1/Simulation/%s/MXML", simulationId));

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/xml");
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Authorization", "Bearer");
        con.setDoOutput(true);

        return con.getInputStream();
    }
}
