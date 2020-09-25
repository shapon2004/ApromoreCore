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
package org.apromore.plugin.portal.bimp;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPathExpressionException;

public abstract class BIMPRequestBodyBuilder {

    protected final String definitionsPath = "/definitions";
    protected final String processPath = "/definitions/process";
    protected final String extensionElementsPath = "/definitions/process/extensionElements";
    protected final String processSimulationInfoPath = "/definitions/process/extensionElements/processSimulationInfo";

    public abstract String createBIMPRequestBody(Document document) throws XPathExpressionException, JAXBException;
}
