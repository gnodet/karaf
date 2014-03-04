/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;

public class HeadlessSessionImpl implements Session {

    final SessionFactory factory;
    final CommandSession session;
    final Registry registry;

    public HeadlessSessionImpl(SessionFactory factory, CommandProcessor processor, InputStream in, PrintStream out, PrintStream err) {
        // Factory
        this.factory = factory;
        // Registry
        this.registry = new RegistryImpl(factory.getRegistry());
        this.registry.register(factory);
        this.registry.register(registry);
        this.registry.register(this);
        // Session
        this.session = processor.createSession(in, out, err);
        this.session.put(".session", this);
    }

    public CommandSession getSession() {
        return session;
    }

    @Override
    public Object execute(CharSequence commandline) throws Exception {
        return session.execute(commandline);
    }

    @Override
    public Object get(String name) {
        return session.get(name);
    }

    @Override
    public void put(String name, Object value) {
        session.put(name, value);
    }

    @Override
    public InputStream getKeyboard() {
        return session.getKeyboard();
    }

    @Override
    public PrintStream getConsole() {
        return session.getConsole();
    }

    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal getTerminal() {
        return null;
    }

    @Override
    public History getHistory() {
        return null;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public SessionFactory getFactory() {
        return factory;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveCommand(String name) {
        // TODO: optimize
        if (!name.contains(":")) {
            String[] scopes = ((String) get(Session.SCOPE)).split(":");
            List<Command> commands = registry.getCommands();
            for (String scope : scopes) {
                for (Command command : commands) {
                    if ((Session.SCOPE_GLOBAL.equals(scope) || command.getScope().equals(scope)) && command.getName().equals(name)) {
                        return command.getScope() + ":" + name;
                    }
                }
            }
        }
        return name;
    }

    @Override
    public void close() {
        session.close();
    }

}
