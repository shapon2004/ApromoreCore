package org.apromore.portal.plugin;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apromore.portal.dialogController.BaseController;

/**
 * This class represents a running plugin.
 * 
 * @author Bruce Nguyen
 *
 */
public class PluginExecution {
    private BaseController baseController;
    
    public PluginExecution(BaseController pluginController) {
        this.baseController = pluginController;
    }
    
    public BaseController getPluginController() {
        return this.baseController;
    }
    
    public void processRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String result = baseController.processRequest(request.getParameter("dataRequest"));
        OutputStream os;
        try {
            os = response.getOutputStream();
            byte[] data = result.getBytes();
            os.write(data);
            response.flushBuffer();
        } catch (IOException e) {
            response.getWriter().write(e.getMessage());
        }
    }
}
