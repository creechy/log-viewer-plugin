/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.GlobalPathRegistry;

/**
 * Support class for context log line analyzation and for creating links in
 * the output window.
 */
class ContextLogSupport extends LogSupport {

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

    public LogSupport.LineInfo analyzeLine(String logLine) {
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
        return new LogSupport.LineInfo(path, line, message, error, accessible);
    }
}
