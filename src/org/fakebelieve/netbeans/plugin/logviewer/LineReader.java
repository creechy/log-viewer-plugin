/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Simple non-blocking line reader.
 * 
 * @author mock
 */
public class LineReader {

    private final BufferedReader reader;
    private char[] buffer = new char[2048];
    private int bufferLen = 0;
    private int bufferPtr = 0;
    private boolean checkForNewline = false;
    private StringBuilder lineBuffer = new StringBuilder();

    public LineReader(BufferedReader reader) {
        this.reader = reader;
    }

    public String readLine() throws IOException {
        for (;;) {
            if (bufferPtr >= bufferLen) {
                if (!reader.ready()) {
                    return null;
                }
                bufferLen = reader.read(buffer);
                bufferPtr = 0;
            }

            char chr = buffer[bufferPtr++];

            if (checkForNewline) {
                // We've already hit a \r, check to see if a \n is following it
                // and consume it, otherwise reuse the character next time.
                // and return the current line.

                checkForNewline = false;

                if (chr != '\n') {
                    bufferPtr--;
                }
                String completeLine = lineBuffer.toString();
                lineBuffer = new StringBuilder();
                return completeLine;
            }
            if (chr == '\n') {
                // We've hit a \n, return the current line.

                String completeLine = lineBuffer.toString();
                lineBuffer = new StringBuilder();
                return completeLine;
            }
            if (chr == '\r') {
                // We've hit a \r, it may be followed by a \n, lets set a flag to
                // check that next iteration. Doing it in the next iteration will
                // ensure that its either in the buffer or that we return until more
                // is in the buffer to check.

                checkForNewline = true;
            }

            // No end-of-line chars, so just add it to the current line buffer.
            lineBuffer.append(chr);
        }
    }
}
