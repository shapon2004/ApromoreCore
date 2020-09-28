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

import org.apromore.plugin.portal.DefaultPortalPlugin;
import org.apromore.plugin.portal.PortalContext;
import org.apromore.portal.common.UserSessionManager;
import org.apromore.portal.dialogController.MainController;
import org.apromore.portal.model.ExportFormatResultType;
import org.apromore.portal.model.ProcessSummaryType;
import org.apromore.portal.model.SummaryType;
import org.apromore.portal.model.VersionSummaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import java.io.InputStream;
import java.util.*;

public class BIMPPortalPlugin extends DefaultPortalPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BIMPPortalPlugin.class);

    private final String label = "Generate BIMP simulated log";
    private final String groupLabel = "Analyze";


    // PortalPlugin overrides

    @Override
    public String getLabel(Locale locale) {
        return label;
    }

    @Override
    public String getGroupLabel(Locale locale) {
        return groupLabel;
    }

    @Override
    public String getIconPath() {
        return "bimp/icon.svg";
    }

    @Override
    public void execute(PortalContext portalContext) {
        try {
            MainController mainController = (MainController) portalContext.getMainController();
            Map<SummaryType, List<VersionSummaryType>> selectedProcessVersions = mainController.getSelectedElementsAndVersions();
            if (selectedProcessVersions.size() != 1) {
                throw new Exception("You must select exactly one process");
            }
            SummaryType summaryType = selectedProcessVersions.keySet().iterator().next();
            if (!(summaryType instanceof ProcessSummaryType)) {
                throw new Exception("Only processes may be simulated");
            }
            ProcessSummaryType model = (ProcessSummaryType) summaryType;
            VersionSummaryType version = selectedProcessVersions.get(model).get(0);
            ExportFormatResultType exportResult = mainController.getService().exportFormat(
                    model.getId(), model.getName(), version.getName(),
                    version.getVersionNumber(), model.getOriginalNativeType(), UserSessionManager.getCurrentUser().getUsername()
            );
            InputStream in = exportResult.getNative().getInputStream();
            Scanner scanner = new Scanner(in).useDelimiter("\\A");

            // Open the dialog, passing the BPMN markup as an argument
            Map arg = new HashMap<>();
            arg.put("bpmn", scanner.next());
            Window window = (Window) portalContext.getUI().createComponent(getClass().getClassLoader(), "bimp/bimp.zul", null, arg);
            window.doModal();

        } catch (Exception e) {
            LOGGER.error("Unable to generate BIMP simulation log", e);
            Messagebox.show(e.getMessage(), "Attention", Messagebox.OK, Messagebox.ERROR);
        }
    }
}
