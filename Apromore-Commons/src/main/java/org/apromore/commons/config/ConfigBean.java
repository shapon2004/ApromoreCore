/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2012 - 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
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

package org.apromore.commons.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties
public class ConfigBean {
    private String logsDir = "../Event-Logs-Repository";
    private String numOfEvent;
    private String numOfTrace;
//    Fallback Storage path
    private String storagePath = "FILE::../Event-Logs-Repository";
    
    private static final long serialVersionUID = 117L;
    private static final String COMMUNITY_TAG = "community";

    private String  siteEditor;
    private String  siteExternalHost;
    private int     siteExternalPort;
    private String  siteFilestore;
    private String  siteManager;
    private String  sitePortal;
    private String  majorVersionNumber;
    private String  minorVersionNumber;
    private String  versionEdition;
    private String  versionBuildDate;

    // LDAP
    private String  ldapProviderURL;
    private String  ldapUserContext;
    private String  ldapUsernameAttribute;
    private String  ldapEmailAttribute;
    private String  ldapFirstNameAttribute;
    private String  ldapLastNameAttribute;

    // Switches to enable features
    private boolean  enablePublish;
    private boolean  enableTC;
    private boolean  enablePP;
    private boolean  enableUserReg;
    private boolean  enableFullUserReg;
    private boolean  enableSubscription;

    // Switch for custom calendar
    private boolean  enableCalendar;

    // Maximum upload size
    private long     maxUploadSize;
    
    private boolean useKeycloakSso;
    
    private String securityMsHost;
    private String securityMsPort;
    private String securityMsHttpLogoutUrl;
    private String securityMsHttpsLogoutUrl;

    // Email for issue reporting
    private String  contactEmail;



	public boolean isCommunity() {
        return versionEdition.toLowerCase().contains(COMMUNITY_TAG);
    }
    
}
