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

package org.apromore.processsimulation.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessSimulationInfo {

    @XmlAttribute
    private String id;
    @XmlAttribute
    private long processInstances;
    @XmlAttribute
    private Currency currency;
    @XmlAttribute
    private String startDateTime;

    @XmlElement(name = "qbp:errors")
    private Errors errors;

    @XmlElement(name = "qbp:arrivalRateDistribution")
    private Distribution arrivalRateDistribution;

    @XmlElementWrapper(name = "qbp:elements")
    @XmlElement(name = "qbp:element")
    private List<Element> tasks;

    @XmlElementWrapper(name = "qbp:timetables")
    @XmlElement(name = "qbp:timetable")
    private List<Timetable> timetables;

    @XmlElementWrapper(name = "qbp:resources")
    @XmlElement(name = "qbp:resource")
    private List<Resource> resources;

}
