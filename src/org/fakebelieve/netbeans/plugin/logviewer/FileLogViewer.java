/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

/*
 * Netbeans Logging Levels - add option "-J-Dorg.netbeans.level=FINEST" into netbeans.conf file.
 */
public class FileLogViewer extends LogViewer {

    /**
     * Connects a given process to the output window. Returns immediately, but threads are started that
     * copy streams of the process to/from the output window.
     *
     * @param process process whose streams to connect to the output window
     * @param ioName name of the output window tab to use
     */
    public FileLogViewer(String logConfig, final String ioName) {
        super(logConfig, ioName);
    }

    public static boolean handleConfig(String logConfig) {
        File file = new File(logConfig);
        return file.exists();
    }

    @Override
    public void configViewer() throws IOException {

            File logFile = new File(logConfig);
            logStream = new FileInputStream(logFile);
            logReader = new BufferedReader(new InputStreamReader(logStream));
            lineReader = new LineReader(logReader);

            io.getOut().println("*** -> " + logConfig);
            io.getOut().println("***");
            io.getOut().println();
            log.log(Level.FINE, "Started reader.");
    }

}