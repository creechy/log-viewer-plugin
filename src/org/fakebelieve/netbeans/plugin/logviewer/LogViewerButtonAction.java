/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

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
    protected int nameSize = 30;


    @Override
    public void actionPerformed(ActionEvent e) {
        LogViewerPanel myPanel = new LogViewerPanel();

        List<History> logHistory = LogViewerOptions.loadHistory();
        myPanel.setLogHistory(logHistory);

        DialogDescriptor dd = new DialogDescriptor(myPanel, "Log Viewer Chooser");

        // let's display the dialog now...
        if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.OK_OPTION) {
            // user clicked yes, do something here, for example:
            System.out.println(myPanel.getLogConfig());
            String logConfig = myPanel.getLogConfig();
            History history = new History(myPanel.getLogConfig(), Integer.parseInt(myPanel.getRefreshConfig()), Integer.parseInt(myPanel.getLookbackConfig()));
            LogViewerOptions.updateHistory(logHistory, history);
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
