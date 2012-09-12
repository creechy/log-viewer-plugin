/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.HelpCtx;

public class LogViewerAction {

    protected static final Logger log = Logger.getLogger(LogViewer.class.getName());
    protected Map values = new HashMap();

    public static LogViewerAction getLogViewerAction(Map map) {
        Object o = map.get("viewerAction");
        if (!(o instanceof LogViewerAction)) {
            log.log(Level.SEVERE, "Received an invalid viewerAction type: {0}", o.getClass().getName());
            throw new IllegalArgumentException("Invalid viewerAction attribute. Has to be a 'newvalue' of type LogViewerAction.");
        }

        LogViewerAction logViewer = (LogViewerAction) o;
        log.log(Level.FINE, "Creating new action of type: {0}", logViewer.getClass().getName());

        for (Object entry : map.entrySet()) {
            Object key = ((Map.Entry) entry).getKey();

            if ("instanceCreate".equals(key) || "viewerAction".equals(key)) {
                continue;
            }

            // we have a valid property, add it
            Object value = ((Map.Entry) entry).getValue();
            logViewer.putValue((String) key, value);

            // log it
            if (value == null) {
                value = "null";
            }
            log.log(Level.FINE, "Adding LogViewerAction attribute ''{0}''=''{1}'' ({2}).",
                    new Object[]{(String) key, value.toString(), logViewer.getClass().getName()});
        }

        return logViewer;
    }

    public void viewLog(String f) {
        LogViewer p = new LogViewer(f, getName());
        p.setLookbackLines(Integer.parseInt((String) getValue("lookback")));
        p.setRefreshInterval(Integer.parseInt((String) getValue("refresh")));
        
        try {
            p.showLogViewer();
        } catch (java.io.IOException e) {
            log.log(Level.SEVERE, "Showing log action failed.", e);
        }
    }

    public void performAction() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        return (String) getValue("displayName");
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

        public String iconResource() {
        return "org/fakebelieve/netbeans/plugin/logviewer/wilbur.png"; // NOI18N
    }

    public Object getValue(String key) {
        return values.get(key);
   }

    public void putValue(String key, Object value) {
        values.put(key, value);
    }
}
