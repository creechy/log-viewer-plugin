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
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class LogViewer implements Runnable {

    private static final Logger log = Logger.getLogger(LogViewer.class.getName());
    boolean shouldStop = false;
    private ContextLogSupport logSupport;
    FileInputStream filestream = null;
    BufferedReader ins;
    InputOutput io;
    File fileName;
    String ioName;
    int lines;
    Ring ring;
    private final RequestProcessor.Task task = RequestProcessor.getDefault().create(this);

    /**
     * Connects a given process to the output window. Returns immediately, but threads are started that
     * copy streams of the process to/from the output window.
     *
     * @param process process whose streams to connect to the output window
     * @param ioName name of the output window tab to use
     */
    public LogViewer(final File fileName, final String ioName) {
        this.fileName = fileName;
        this.ioName = ioName;
        logSupport = new ContextLogSupport("/tmp", null);
    }

    private void init() {
        final int LINES = 2000;
        final int OLD_LINES = 2000;
        ring = new Ring(OLD_LINES);
        String line;

        // Read the log file without
        // displaying everything
        try {
            while ((line = ins.readLine()) != null) {
                ring.add(line);
            } // end of while ((line = ins.readLine()) != null)
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed reading log file.", e);
        } // end of try-catch

        // Now show the last OLD_LINES
        lines = ring.output();
        ring.setMaxCount(LINES);
    }

    @Override
    public void run() {
        final int MAX_LINES = 10000;
        String line;

        shouldStop = io.isClosed();

        if (!shouldStop) {
            try {
                if (lines >= MAX_LINES) {
                    io.getOut().reset();
                    lines = ring.output();
                } // end of if (lines >= MAX_LINES)

                while ((line = ins.readLine()) != null) {
                    if ((line = ring.add(line)) != null) {
                        //io.getOut().println(line);
                        processLine(line);
                        lines++;
                    } // end of if ((line = ring.add(line)) != null)
                }

            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed reading log file and printing to output.", e);
            }
            task.schedule(10000);
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
        io = IOProvider.getDefault().getIO(ioName, false);
        io.getOut().reset();
        io.select();
        filestream = new FileInputStream(fileName);
        ins = new BufferedReader(new InputStreamReader(filestream));

        init();
        task.schedule(0);
    }

    /* stop to update  the log viewer dialog
     *
     **/
    public void stopUpdatingLogViewer() {
        try {
            ins.close();
            filestream.close();
            io.closeInputOutput();
            io.setOutputVisible(false);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to close log file streams.", e);
        }
    }

    private void processLine(String line) {
        ContextLogSupport.LineInfo lineInfo = logSupport.analyzeLine(line);
        //System.err.println(line);
        //System.err.println("   " + lineInfo);
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
                //io.getOut().println(s);
                processLine(s);
                i++;
            }

            return i;
        }

        public void reset() {
            anchor = new LinkedList<String>();
        }
    }

    /**
     * Support class for context log line analyzation and for creating links in
     * the output window.
     */
    static class ContextLogSupport extends LogSupport {

        private final String CATALINA_WORK_DIR;
        private String context = null;
        private String prevMessage = null;
        private static final String STANDARD_CONTEXT = "StandardContext["; // NOI18N
        private static final int STANDARD_CONTEXT_LENGTH = STANDARD_CONTEXT.length();
        private GlobalPathRegistry globalPathReg = GlobalPathRegistry.getDefault();

        public ContextLogSupport(String catalinaWork, String webAppContext) {
            CATALINA_WORK_DIR = catalinaWork;
            context = webAppContext;
        }

        public LineInfo analyzeLine(String logLine) {
            String path = null;
            int line = -1;
            String message = null;
            boolean error = false;
            boolean accessible = false;

            logLine = logLine.trim();
            int lineLenght = logLine.length();

            // look for unix file links (e.g. /foo/bar.java:51: 'error msg')
            if (logLine.startsWith("/")) {
                error = true;
                int colonIdx = logLine.indexOf(':');
                if (colonIdx > -1) {
                    path = logLine.substring(0, colonIdx);
                    accessible = true;
                    if (lineLenght > colonIdx) {
                        int nextColonIdx = logLine.indexOf(':', colonIdx + 1);
                        if (nextColonIdx > -1) {
                            String lineNum = logLine.substring(colonIdx + 1, nextColonIdx);
                            try {
                                line = Integer.valueOf(lineNum).intValue();
                            } catch (NumberFormatException nfe) {
                                // ignore it
                                Logger.getLogger(LogViewer.class.getName()).log(Level.INFO, null, nfe);
                            }
                            if (lineLenght > nextColonIdx) {
                                message = logLine.substring(nextColonIdx + 1, lineLenght);
                            }
                        }
                    }
                }
            } // look for windows file links (e.g. c:\foo\bar.java:51: 'error msg')
            else if (lineLenght > 3 && Character.isLetter(logLine.charAt(0))
                    && (logLine.charAt(1) == ':') && (logLine.charAt(2) == '\\')) {
                error = true;
                int secondColonIdx = logLine.indexOf(':', 2);
                if (secondColonIdx > -1) {
                    path = logLine.substring(0, secondColonIdx);
                    accessible = true;
                    if (lineLenght > secondColonIdx) {
                        int thirdColonIdx = logLine.indexOf(':', secondColonIdx + 1);
                        if (thirdColonIdx > -1) {
                            String lineNum = logLine.substring(secondColonIdx + 1, thirdColonIdx);
                            try {
                                line = Integer.valueOf(lineNum).intValue();
                            } catch (NumberFormatException nfe) { // ignore it
                                Logger.getLogger(LogViewer.class.getName()).log(Level.INFO, null, nfe);
                            }
                            if (lineLenght > thirdColonIdx) {
                                message = logLine.substring(thirdColonIdx + 1, lineLenght);
                            }
                        }
                    }
                }
            } // look for stacktrace links (e.g. at java.lang.Thread.run(Thread.java:595)
            //                                 at t.HyperlinkTest$1.run(HyperlinkTest.java:24))
            else if (logLine.startsWith("at ") && lineLenght > 3) {
                error = true;
                int parenthIdx = logLine.indexOf('(');
                if (parenthIdx > -1) {
                    String classWithMethod = logLine.substring(3, parenthIdx);
                    int lastDotIdx = classWithMethod.lastIndexOf('.');
                    if (lastDotIdx > -1) {
                        int lastParenthIdx = logLine.lastIndexOf(')');
                        int lastColonIdx = logLine.lastIndexOf(':');
                        if (lastParenthIdx > -1 && lastColonIdx > -1) {
                            String lineNum = logLine.substring(lastColonIdx + 1, lastParenthIdx);
                            try {
                                line = Integer.valueOf(lineNum).intValue();
                            } catch (NumberFormatException nfe) { // ignore it
                                Logger.getLogger(LogViewer.class.getName()).log(Level.INFO, null, nfe);
                            }
                            message = prevMessage;
                        }
                        int firstDolarIdx = classWithMethod.indexOf('$'); // > -1 for inner classes
                        String className = classWithMethod.substring(0, firstDolarIdx > -1 ? firstDolarIdx : lastDotIdx);
                        path = className.replace('.', '/') + ".java"; // NOI18N
                        accessible = globalPathReg.findResource(path) != null;
                        if (className.startsWith("org.apache.jsp.") && context != null) { // NOI18N
                            if (context != null) {
                                String contextPath = context.equals("/")
                                        ? "/_" // hande ROOT context
                                        : context;
                                path = CATALINA_WORK_DIR + contextPath + "/" + path;
                                accessible = new File(path).exists();
                            }
                        }
                    }
                }
            } // every other message treat as normal info message
            else {
                prevMessage = logLine;
                // try to get context, if stored
                int stdContextIdx = logLine.indexOf(STANDARD_CONTEXT);
                int lBracketIdx = -1;
                if (stdContextIdx > -1) {
                    lBracketIdx = stdContextIdx + STANDARD_CONTEXT_LENGTH;
                }
                int rBracketIdx = logLine.indexOf(']');
                if (lBracketIdx > -1 && rBracketIdx > -1 && rBracketIdx > lBracketIdx) {
                    context = logLine.substring(lBracketIdx, rBracketIdx);
                }
            }
            return new LineInfo(path, line, message, error, accessible);
        }
    }
}