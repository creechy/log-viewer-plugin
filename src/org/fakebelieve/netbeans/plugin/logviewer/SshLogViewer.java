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
        if (logConfig.length() > maxIoName) {
            int pathStart = logConfig.indexOf("/", 6);
            String nameStart = logConfig.substring(0, pathStart + 1);
            String logPath = logConfig.substring(logConfig.length() - (maxIoName - pathStart));
            init(logConfig, nameStart + "..." + logPath);
        } else {
            init(logConfig, logConfig);
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

        String userHost = logConfig.substring(6, pathStart);
        int userHostSeparator = userHost.indexOf("@");
        String user = (userHostSeparator >= 0) ? userHost.substring(0, userHostSeparator) : System.getProperty("user.name");
        String host = (userHostSeparator >= 0) ? userHost.substring(userHostSeparator + 1) : userHost;

        String logPath = logConfig.substring(pathStart);

        String sshCommand = LogViewerOptions.getSshCommand();
        String remoteCmd = LogViewerOptions.getRemoteCommand();

        List<String> command = new ArrayList<String>();
        StrTokenizer st = new StrTokenizer(sshCommand, StrMatcher.charSetMatcher(" \t"), StrMatcher.quoteMatcher());
        while (st.hasNext()) {
            String arg = st.nextToken();
            arg = arg.replace("%h", host);
            arg = arg.replace("%r", user);
            arg = arg.replace("%d", userHost);
            command.add(arg);
        }
        st = new StrTokenizer(remoteCmd, StrMatcher.charSetMatcher(" \t"), StrMatcher.quoteMatcher());
        while (st.hasNext()) {
            String arg = st.nextToken();
            arg = arg.replace("%n", String.valueOf(lookbackLines));
            arg = arg.replace("%f", logPath);
            command.add(arg);
        }

        startCommand(command);
    }
}
