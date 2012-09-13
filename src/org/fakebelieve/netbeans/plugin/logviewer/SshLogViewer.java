/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author mock
 */
public class SshLogViewer extends ProcessLogViewer {

    // ssh://user@host/...

    public SshLogViewer(String logConfig, String ioName) {
        super(logConfig, ioName);
    }

    public static boolean handleConfig(String logConfig) {
        return logConfig.startsWith("ssh://");
    }

    @Override
    public void configViewer() throws IOException {
        int pathStart = logConfig.indexOf("/", 6);
        if (pathStart < 0) {
            throw new FileNotFoundException("Cannot Parse \"" + logConfig + "\"");
        }

        String userHost = logConfig.substring(5, pathStart);
        String logPath = logConfig.substring(pathStart);

        logConfig = "ssh " + userHost + " tail -f \"" + logPath + "\"";

        super.configViewer();
    }
}
