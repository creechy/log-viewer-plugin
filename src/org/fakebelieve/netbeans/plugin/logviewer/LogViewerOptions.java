/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.util.Locale;
import org.openide.util.NbPreferences;

/**
 *
 * @author mock
 */
public class LogViewerOptions {

    public static final String DEFAULT_UNIX_SSH_COMMAND = "/usr/bin/ssh -l %r %h";
    public static final String DEFAULT_WINDOWS_SSH_COMMAND = "\"C:\\Program Files\\PuTTY\\plink.exe\" -l %r %h";
    public static final String DEFAULT_REMOTE_COMMAND = "tail -n %n -f %f";

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
    public static String getSshCommand() {
        if (isWindows()) {
         return NbPreferences.forModule(LogViewerOptionsPanel.class).get("sshCommand", DEFAULT_WINDOWS_SSH_COMMAND);
        } else {
         return NbPreferences.forModule(LogViewerOptionsPanel.class).get("sshCommand", DEFAULT_UNIX_SSH_COMMAND);
        }
    }
    
    public static void setSshCommand(String command) {
        NbPreferences.forModule(LogViewerOptionsPanel.class).put("sshCommand", command);        
    }

    public static String getRemoteCommand() {
        return NbPreferences.forModule(LogViewerOptionsPanel.class).get("remoteCommand", DEFAULT_REMOTE_COMMAND);
    }

    public static void setRemoteCommand(String command) {
        NbPreferences.forModule(LogViewerOptionsPanel.class).put("remoteCommand", command);
    }
}
