/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.openide.util.NbPreferences;

/**
 *
 * @author mock
 */
public class SshLogViewer extends ProcessLogViewer {

    // ssh://user@host/...
    public SshLogViewer(String logConfig) {
        super(logConfig);
        if (logConfig.length() > maxIoName) {
            int pathStart = logConfig.indexOf("/", 6);
            String nameStart = logConfig.substring(0, pathStart + 1);
            String logPath = logConfig.substring(logConfig.length() - (maxIoName - pathStart));
            ioName = nameStart + "..." + logPath;
        }
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

        String sshCommand = NbPreferences.forModule(LogViewerOptionsPanel.class).get("sshCommand", "/usr/bin/ssh");


        List<String> command = new ArrayList<String>();
        StrTokenizer st = new StrTokenizer(sshCommand, StrMatcher.charSetMatcher(" \t"), StrMatcher.quoteMatcher());
        while (st.hasNext()) {
            command.add(st.nextToken());
        }
        command.add(userHost);
        command.add("tail");
        if (lookbackLines > 0) {
            command.add("-n");
            command.add(String.valueOf(lookbackLines));
        }
        command.add("-f");
        command.add(logPath);

        startCommand(command);
    }
}
