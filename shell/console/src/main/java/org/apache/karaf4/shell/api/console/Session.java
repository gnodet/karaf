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
package org.apache.karaf4.shell.api.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public interface Session extends Runnable {

    //
    // Session properties
    //
    // Property names starting with "karaf." are reserved for karaf
    //

    String SCOPE = "SCOPE";
    String SUBSHELL = "SUBSHELL";

    String PRINT_STACK_TRACES = "karaf.printStackTraces";
    String LAST_EXCEPTION = "karaf.lastException";
    String IGNORE_INTERRUPTS = "karaf.ignoreInterrupts";
    String COMPLETION_MODE = "karaf.completionMode";

    String COMPLETION_MODE_GLOBAL = "global";
    String COMPLETION_MODE_SUBSHELL = "subshell";
    String COMPLETION_MODE_FIRST = "first";

    /**
     * Execute a program in this session.
     *
     * @param commandline
     * @return the result of the execution
     */
    Object execute(CharSequence commandline) throws Exception;

    /**
     * Get the value of a variable.
     *
     * @param name
     * @return
     */
    Object get(String name);

    /**
     * Set the value of a variable.
     *
     * @param name  Name of the variable.
     * @param value Value of the variable
     */
    void put(String name, Object value);

    /**
     * Return the input stream that is the first of the pipeline. This stream is
     * sometimes necessary to communicate directly to the end user. For example,
     * a "less" or "more" command needs direct input from the keyboard to
     * control the paging.
     *
     * @return InputStream used closest to the user or null if input is from a
     *         file.
     */
    InputStream getKeyboard();

    /**
     * Return the PrintStream for the console. This must always be the stream
     * "closest" to the user. This stream can be used to post messages that
     * bypass the piping. If the output is piped to a file, then the object
     * returned must be null.
     *
     * @return
     */
    PrintStream getConsole();

    /**
     * Prompt the user for a line.
     *
     * @param prompt
     * @param mask
     * @return
     * @throws java.io.IOException
     */
    String readLine(String prompt, final Character mask) throws IOException;

    Terminal getTerminal();

    History getHistory();

    Registry getRegistry();

    SessionFactory getFactory();

    /**
     * Close this session. After the session is closed, it will throw
     * IllegalStateException when it is used.
     */
    void close();
}
