/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
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
displayName = "#CTL_LogViewerAction")
@ActionReference(path = "Toolbars/Debug", position = 1050)
@Messages("CTL_LogViewerAction=Log Viewer")
public class LogViewerButtonAction implements ActionListener {

    protected static final Logger log = Logger.getLogger(LogViewer.class.getName());

    @Override
    public void actionPerformed(ActionEvent e) {
        LogViewerPanel myPanel = new LogViewerPanel();

        DialogDescriptor dd = new DialogDescriptor(myPanel, "Log Viewer");

        // let's display the dialog now...
        if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.OK_OPTION) {
            // user clicked yes, do something here, for example:
            System.out.println(myPanel.getLogFile());
            String logPath = myPanel.getLogFile();
            String logName;

            if (logPath.startsWith("!")) {
                logName = logPath.substring(1).trim();
            } else {
                int rindex = logPath.lastIndexOf("/");
                if (rindex == -1) {
                    rindex = logPath.lastIndexOf("\\");
                }
                logName = (rindex == -1) ? logPath : logPath.substring(rindex + 1);
            }

            // REMIND: This seems a little backwards, should probably rework it.
            Map map = new HashMap();
            map.put("viewerAction", new ClientLogAction());
            map.put("userDirLogFile", "/var/log/messages.log");
            map.put("logFile", logPath);
            map.put("displayName", "Log - " + logName);
            map.put("name", "Log - " + logName);

            LogViewerAction logAction = LogViewerAction.getLogViewerAction(map);
            logAction.performAction();
        }
    }
}
