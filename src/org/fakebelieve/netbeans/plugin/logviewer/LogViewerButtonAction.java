/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
displayName = "#CTL_LogViewerButtonAction")
@ActionReference(path = "Toolbars/Debug", position = 1050)
@Messages("CTL_LogViewerButtonAction=Log Viewer")
public class LogViewerButtonAction implements ActionListener {

    protected static final Logger log = Logger.getLogger(LogViewer.class.getName());
    protected Preferences preferences = NbPreferences.forModule(LogViewer.class);
    protected int nameSize = 30;

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
            viewLog(logConfig, Integer.parseInt(myPanel.getLookbackConfig()), Integer.parseInt(myPanel.getRefreshConfig()));
        }
    }

    public void viewLog(String logConfig, int lookback, int refresh) {
        LogViewer viewer = null;
        if (FileLogViewer.handleConfig(logConfig)) {
            viewer = new FileLogViewer(logConfig);
        } else if (ProcessLogViewer.handleConfig(logConfig)) {
            viewer = new ProcessLogViewer(logConfig);
        } else if (SshLogViewer.handleConfig(logConfig)) {
            viewer = new SshLogViewer(logConfig);
        }

        if (viewer != null) {
            viewer.setLookbackLines(lookback);
            viewer.setRefreshInterval(refresh);

            try {
                viewer.showLogViewer();
            } catch (java.io.IOException e) {
                log.log(Level.SEVERE, "Showing log action failed.", e);
            }
        }
    }
}
