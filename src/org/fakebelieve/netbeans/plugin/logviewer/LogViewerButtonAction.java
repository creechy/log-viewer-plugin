/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

@ActionID(
    category = "Debug",
id = "org.fakebelieve.netbeans.plugin.logviewer.LogViewerAction")
@ActionRegistration(
    iconBase = "org/fakebelieve/netbeans/plugin/logviewer/wilbur.png",
displayName = "#CTL_LogViewerAction")
@ActionReference(path = "Toolbars/Debug", position = 1050)
@Messages("CTL_LogViewerAction=Log Viewer")
public class LogViewerButtonAction implements ActionListener {

    protected static final Logger log = Logger.getLogger(LogViewer.class.getName());
    protected Preferences preferences = NbPreferences.forModule(LogViewer.class);

    protected List<String> loadHistory() {
        List<String> logHistory = new ArrayList<String>();
        for (int idx = 0; idx < 15; idx++) {
            String text = preferences.get("history-" + idx, null);
            if (text != null && !text.isEmpty()) {
                logHistory.add(text);
            }
        }
        return logHistory;
    }

    protected void updateHistory(List<String> logHistory, String logConfig) {
        //
        // Save prefs in MRU order that way, crapshit ones will drop off the
        // list eventually.
        //
        for (int idx = 0; idx < logHistory.size(); idx++) {
            if (logHistory.get(idx).equals(logConfig)) {
                logHistory.remove(idx);
                break;
            }
        }
        logHistory.add(0, logConfig);

        for (int idx = 0; idx < 15; idx++) {
            if (idx < logHistory.size()) {
                preferences.put("history-" + idx, logHistory.get(idx));
            } else {
                preferences.remove("history-" + idx);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LogViewerPanel myPanel = new LogViewerPanel();

        List<String> logHistory = loadHistory();
        myPanel.setLogHistory(logHistory.toArray(new String[0]));

        DialogDescriptor dd = new DialogDescriptor(myPanel, "Log Viewer Chooser");

        // let's display the dialog now...
        if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.OK_OPTION) {
            // user clicked yes, do something here, for example:
            System.out.println(myPanel.getLogConfig());
            String logConfig = myPanel.getLogConfig();
            updateHistory(logHistory, logConfig);
            
            String logName = (logConfig.startsWith("!")) ? logConfig.substring(1).trim() : logConfig.trim();
            if (logName.length() > 40) {
                logName = logName.substring(0, 20) + "..." + logName.substring(logName.length() - 20);
            }

            // REMIND: This seems a little backwards, should probably rework it.
            Map map = new HashMap();
            map.put("viewerAction", new LogViewerAction());
            map.put("displayName", logName + " (log)");
            map.put("name", logName + " (log)");
            map.put("lookback", myPanel.getLookbackConfig());
            map.put("refresh", myPanel.getRefreshConfig());

            LogViewerAction logAction = LogViewerAction.getLogViewerAction(map);
            logAction.viewLog(logConfig);
        }
    }
}
