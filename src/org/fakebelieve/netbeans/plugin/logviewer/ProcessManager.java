/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fakebelieve.netbeans.plugin.logviewer;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple class to manage started processes and kill them when the IDE exits.
 * 
 * @author mock
 */
public class ProcessManager extends Thread {

    Set<Process> processes = new HashSet<Process>();

    public ProcessManager() {
        Runtime.getRuntime().addShutdownHook(this);
    }

    public void add(Process process) {
        processes.add(process);
    }

    public void remove(Process process) {
        processes.remove(process);
    }

    @Override
    public void run() {
        for (Process process : processes) {
            process.destroy();
        }
    }
}
