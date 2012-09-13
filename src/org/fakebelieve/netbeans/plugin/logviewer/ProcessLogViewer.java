/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

/*
 * Netbeans Logging Levels - add option "-J-Dorg.netbeans.level=FINEST" into netbeans.conf file.
 */
public class ProcessLogViewer extends LogViewer {

    private static ProcessManager procMgr = new ProcessManager();
    private Process process = null;

    /**
     * Connects a given process to the output window. Returns immediately, but threads are started that
     * copy streams of the process to/from the output window.
     *
     * @param process process whose streams to connect to the output window
     * @param ioName name of the output window tab to use
     */
    public ProcessLogViewer(String logConfig, final String ioName) {
        super(logConfig.substring(1), ioName);
    }

    public static boolean handleConfig(String logConfig) {
        return logConfig.startsWith("!");
    }

    @Override
    public boolean checkShouldStop() {
        return shouldStop;
    }

    @Override
    public void configViewer() throws IOException {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/sh", "-c", logConfig);
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
        procMgr.add(process);
        logStream = process.getInputStream();
        logReader = new BufferedReader(new InputStreamReader(logStream));
        lineReader = new LineReader(logReader);

        io.getOut().println("*** -> " + logConfig);
        io.getOut().println("***");
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