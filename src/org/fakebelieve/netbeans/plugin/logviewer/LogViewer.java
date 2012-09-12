/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/*
 * Netbeans Logging Levels - add option "-J-Dorg.netbeans.level=FINEST" into netbeans.conf file.
 */

public class LogViewer implements Runnable {

    private static final Logger log = Logger.getLogger(LogViewer.class.getName());
    private static ProcessManager procMgr = new ProcessManager();

    private boolean shouldStop = false;
    private ContextLogSupport logSupport;
    private InputStream logStream = null;
    private BufferedReader logReader = null;
    private InputOutput io;
    private String logConfig;
    private String ioName;
    private int refreshInterval = 10;
    private int maxLines = -1;
    private int lookbackLines = 2000;
    private int bufferLines = 2000;
    private int linesRead;
    private Ring ring;
    private final RequestProcessor.Task task = RequestProcessor.getDefault().create(this);
    private Process process = null;

    /**
     * Connects a given process to the output window. Returns immediately, but threads are started that
     * copy streams of the process to/from the output window.
     *
     * @param process process whose streams to connect to the output window
     * @param ioName name of the output window tab to use
     */
    public LogViewer(String logConfig, final String ioName) {
        this.logConfig = logConfig;
        this.ioName = ioName;
        logSupport = new ContextLogSupport("/tmp", null);
    }

    public void setLookbackLines(int lookbackLines) {
        this.lookbackLines = lookbackLines;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    private void init() {
        ring = new Ring(lookbackLines);

        // Read the log file without
        // displaying everything
        try {
            String line;
            while (logReader.ready() && (line = logReader.readLine()) != null) {
                ring.add(line);
            } // end of while ((line = ins.readLine()) != null)
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed reading log file.", e);
        } // end of try-catch

        // Now show the last OLD_LINES
        linesRead = ring.output();
        ring.setMaxCount(bufferLines);
        log.log(Level.FINE, "Done reading file.");
    }

    @Override
    public void run() {
        log.log(Level.FINE, "{0} - isClosed() = {1}", new Object[]{ioName, io.isClosed()});
        if (!shouldStop && !io.isClosed()) {
            try {
                if (maxLines > 0 && linesRead >= maxLines) {
                    io.getOut().reset();
                    linesRead = ring.output();
                } // end of if (lines >= MAX_LINES)

                String line;
                while (logReader.ready() && (line = logReader.readLine()) != null) {
                    if ((line = ring.add(line)) != null) {
                        //io.getOut().println(line);
                        processLine(line);
                        linesRead++;
                    } // end of if ((line = ring.add(line)) != null)
                }

            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed reading log file and printing to output.", e);
            }
            task.schedule(refreshInterval * 1000);
        } else {
            ///System.out.println("end of infinite loop for log viewer\n\n\n\n");
            stopUpdatingLogViewer();
        }
    }

    /* display the log viewer dialog
     *
     **/
    public void showLogViewer() throws IOException {
        shouldStop = false;

        io = IOProvider.getDefault().getIO(ioName, true);
        io.getOut().reset();
        io.select();

        try {
            if (!logConfig.startsWith("!")) {
                File logFile = new File(logConfig);
                logStream = new FileInputStream(logFile);
                logReader = new BufferedReader(new InputStreamReader(logStream));
                io.getOut().println("*** -> " + logConfig);
                io.getOut().println("***");
                io.getOut().println();
                log.log(Level.FINE, "Started reader.");
            } else {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("/bin/sh", "-c", logConfig.substring(1).trim());
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                procMgr.add(process);
                logStream = process.getInputStream();
                logReader = new BufferedReader(new InputStreamReader(logStream));
                io.getOut().println("*** -> " + logConfig.substring(1).trim());
                io.getOut().println("***");
                io.getOut().println();
                log.log(Level.FINE,"Started process.");
            }
        } catch (IOException ex) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream pout = new PrintStream(out);

            if (ex instanceof FileNotFoundException) {
                pout.println("File \"" + logConfig + "\" Not Found.");
            } else {
                ex.printStackTrace(pout);
                pout.flush();
            }
            
            logStream = new ByteArrayInputStream(out.toByteArray());
            logReader = new BufferedReader(new InputStreamReader(logStream));
            log.log(Level.FINE,"Showing error.");
        }

        init();
        task.schedule(0);
    }

    /* stop to update  the log viewer dialog
     *
     **/
    public void stopUpdatingLogViewer() {
        log.log(Level.FINE, "Stopping Log Viewer.");
        try {
            logReader.close();
            logStream.close();
            if (process != null) {
                process.destroy();
                procMgr.remove(process);
                process = null;
            }
            io.closeInputOutput();
            io.setOutputVisible(false);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to close log file streams.", e);
        }
    }

    private void processLine(String line) {
        ContextLogSupport.LineInfo lineInfo = logSupport.analyzeLine(line);
        if (lineInfo.isError()) {
            if (lineInfo.isAccessible()) {
                try {
                    io.getErr().println(line, logSupport.getLink(lineInfo.message(), lineInfo.path(), lineInfo.line()));
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else {
                io.getErr().println(line);
            }
        } else {
//            if (line.contains("java.lang.LinkageError: JAXB 2.0 API")) { // NOI18N
//                File file = InstalledFileLocator.getDefault().locate("modules/ext/jaxws21/api/jaxws-api.jar", null, false); // NOI18N
//                File endoresedDir = tomcatManager.getTomcatProperties().getJavaEndorsedDir();
//                if (file != null) {
//                    writer.println(NbBundle.getMessage(LogViewer.class, "MSG_WSSERVLET11", file.getParent(), endoresedDir));
//                } else {
//                    writer.println(NbBundle.getMessage(LogViewer.class, "MSG_WSSERVLET11_NOJAR", endoresedDir));
//                }
//            }
            io.getOut().println(line);
        }
    }

    private class Ring {

        private int maxCount;
        private int count;
        private LinkedList<String> anchor;

        public Ring(int max) {
            maxCount = max;
            count = 0;
            anchor = new LinkedList<String>();
        }

        public String add(String line) {
            if (line == null || line.equals("")) { // NOI18N
                return null;
            } // end of if (line == null || line.equals(""))

            while (count >= maxCount) {
                anchor.removeFirst();
                count--;
            } // end of while (count >= maxCount)

            anchor.addLast(line);
            count++;

            return line;
        }

        public void setMaxCount(int newMax) {
            maxCount = newMax;
        }

        public int output() {
            int i = 0;
            for (String s : anchor) {
                processLine(s);
                i++;
            }

            return i;
        }

        public void reset() {
            anchor = new LinkedList<String>();
        }
    }
}