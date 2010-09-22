/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console.jline;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.BundleContext;

public class ConsoleFactory {

    private BundleContext bundleContext;
    private CommandProcessor commandProcessor;
    private TerminalFactory terminalFactory;
    private Console console;
    private boolean start;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public synchronized void registerCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        this.commandProcessor = commandProcessor;
        start();
    }

    public synchronized void unregisterCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        this.commandProcessor = null;
        stop();
    }

    public void setTerminalFactory(TerminalFactory terminalFactory) {
        this.terminalFactory = terminalFactory;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    protected void start() throws Exception {
        if (start) {
            InputStream in = unwrap(System.in);
            PrintStream out = unwrap(System.out);
            PrintStream err = unwrap(System.err);
            Runnable callback = new Runnable() {
                public void run() {
                    try {
                        bundleContext.getBundle(0).stop();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            };
            final Terminal terminal = terminalFactory.getTerminal();
            this.console = new Console(commandProcessor,
                                       in,
                                       wrap(out),
                                       wrap(err),
                                       terminal,
                                       callback);
            CommandSession session = console.getSession();
            session.put("USER", "karaf");
            session.put("APPLICATION", System.getProperty("karaf.name", "root"));
            session.put("#LINES", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getTerminalHeight());
                }
            });
            session.put("#COLUMNS", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getTerminalWidth());
                }
            });
            session.put(".jline.terminal", terminal);
            new Thread(console, "Karaf Shell Console Thread").start();
        }
    }

    protected void stop() throws Exception {
        if (console != null) {
            console.close();
        }
    }

    private static PrintStream wrap(PrintStream stream) {
        OutputStream o = AnsiConsole.wrapOutputStream(stream);
        if (o instanceof PrintStream) {
            return ((PrintStream) o);
        } else {
            return new PrintStream(o);
        }
    }

    private static <T> T unwrap(T stream) {
        try {
            Method mth = stream.getClass().getMethod("getRoot");
            return (T) mth.invoke(stream);
        } catch (Throwable t) {
            return stream;
        }
    }
}
