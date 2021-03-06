/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;

/*
 * Netbeans Logging Levels - add option "-J-Dorg.netbeans.level=FINEST" into netbeans.conf file.
 */
public class ProcessLogViewer extends LogViewer {

    private static ProcessManager procMgr = new ProcessManager();
    private Process process = null;

    public ProcessLogViewer() {
        super();
    }
    
    /**
     * Connects a given process to the output window. Returns immediately, but threads are started that
     * copy streams of the process to/from the output window.
     *
     * @param process process whose streams to connect to the output window
     * @param ioName name of the output window tab to use
     */
    public ProcessLogViewer(String logConfig) {
        if (logConfig.length() > maxIoName) {
            init(logConfig, logConfig.substring(0, maxIoName / 2) + "..." + logConfig.substring(logConfig.length() - maxIoName / 2));
        } else {
            init(logConfig, logConfig);
        }
    }

    public static boolean handleConfig(String logConfig) {
        return logConfig.startsWith("!");
    }

    @Override
    public boolean checkShouldStop() {
        // Try to get the process exit value. If we get one,
        // the process has ended, so we should stop.

        // Otherwise we'll get an exception which means the
        // process is still running.

        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException ex) {
            // IGNORE
        }
        
        return super.checkShouldStop();
    }

    @Override
    public void configViewer() throws IOException {

        List<String> command = new ArrayList<String>();
        StrTokenizer st = new StrTokenizer(logConfig, StrMatcher.charSetMatcher(" \t"), StrMatcher.quoteMatcher());
        while (st.hasNext()) {
            command.add(st.nextToken());
        }

        startCommand(command);
    }

    public void startCommand(List<String> command) throws IOException {
        log.log(Level.FINE, command.toString());
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
        procMgr.add(process);
        logStream = process.getInputStream();
        logReader = new BufferedReader(new InputStreamReader(logStream));
        lineReader = new LineReader(logReader);

        io.getOut().println("* -> " + logConfig);
        io.getOut().println();
        log.log(Level.FINE, "Started process.");
    }

    /* stop to update  the log viewer dialog
     *
     **/
    @Override
    public void stopUpdatingLogViewer() {
        process.destroy();
        procMgr.remove(process);
        process = null;
        super.stopUpdatingLogViewer();
    }
}