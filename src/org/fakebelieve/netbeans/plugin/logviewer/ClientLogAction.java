/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

/**
 *
 * @author mock
 */
import java.io.File;
import java.util.logging.Level;

public class ClientLogAction extends LogViewerAction {

    public ClientLogAction() {
        putValue("userDirLogFile", "/var/log/messages.log");
    }

    @Override
    public void performAction() {
        String logFilename = (String) getValue("logFile");

        if (logFilename == null) {
            // FIXME This may not be used this way anymore.
            String userDir = System.getProperty("netbeans.user");
            if (userDir == null) {
                return;
            }
            // FIXME the same as above
            logFilename = userDir + getValue("userDirLogFile");
        }

        log.log(Level.FINE, "Viewing client log file: {0}", logFilename);
        File f = new File(logFilename);
        viewLog(f);
    }
}