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
        // TODO implement action body
        // Create instance of your panel, extends JPanel...
        LogViewerPanel myPanel = new LogViewerPanel();

        // Create a custom NotifyDescriptor, specify the panel instance as a parameter + other params
//        NotifyDescriptor nd = new NotifyDescriptor(
//                myPanel, // instance of your panel
//                "Log Viewer", // title of the dialog
//                NotifyDescriptor.YES_NO_OPTION, // it is Yes/No dialog ...
//                NotifyDescriptor.QUESTION_MESSAGE, // ... of a question type => a question mark icon
//                null, // we have specified YES_NO_OPTION => can be null, options specified by L&F,
//                // otherwise specify options as:
//                //     new Object[] { NotifyDescriptor.YES_OPTION, ... etc. },
//                NotifyDescriptor.YES_OPTION // default option is "Yes"
//                );

        DialogDescriptor nd = new DialogDescriptor(myPanel, "Log Viewer");

        // let's display the dialog now...
        if (DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.OK_OPTION) {
            // user clicked yes, do something here, for example:
            System.out.println(myPanel.getLogFile());
            String logPath = myPanel.getLogFile();

            int rindex = logPath.lastIndexOf("/");
            if (rindex == -1) {
                rindex = logPath.lastIndexOf("\\");
            }
            String logFile = (rindex == -1) ? logPath : logPath.substring(rindex + 1);

            Map map = new HashMap();
            map.put("viewerAction", new ClientLogAction());
            map.put("userDirLogFile", "/var/log/messages.log");
            map.put("logFile", logPath);
            map.put("displayName", "Log - " + logFile);
            map.put("name", "Log - " + logFile);

            LogViewerAction logAction = LogViewerAction.getLogViewerAction(map);
            logAction.performAction();
        }
    }

}
