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
package org.apache.karaf4.shell.impl.console.osgi.compat;

import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf4.shell.api.console.CommandLine;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.impl.console.HeadlessSessionImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CommandTracker implements BundleActivator, ServiceTrackerCustomizer<Object, Object> {

    Registry registry;
    BundleContext context;
    ServiceTracker<?, ?> tracker;

    public CommandTracker(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))",
                CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));
        this.tracker = new ServiceTracker<Object, Object>(context, filter, this);
        this.tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = context.getService(reference);
        if (service instanceof CommandWithAction) {
            final CommandWithAction oldCommand = (CommandWithAction) service;
            final Command cmd = oldCommand.getActionClass().getAnnotation(Command.class);
            if (cmd != null) {
                final org.apache.karaf4.shell.api.console.Command command = new org.apache.karaf4.shell.api.console.Command() {
                    @Override
                    public String getScope() {
                        return cmd.scope();
                    }

                    @Override
                    public String getName() {
                        return cmd.name();
                    }

                    @Override
                    public String getDescription() {
                        return cmd.description();
                    }

                    @Override
                    public Completer getCompleter(final boolean scoped) {
                        final ArgumentCompleter completer = new ArgumentCompleter(oldCommand, scoped);
                        return new Completer() {
                            @Override
                            public int complete(Session session, CommandLine commandLine, List<String> candidates) {
                                return completer.complete(session, commandLine, candidates);
                            }
                        };
                    }

                    @Override
                    public Object execute(Session session, List<Object> arguments) throws Exception {
                        // TODO: remove not really nice cast
                        CommandSession commandSession = ((HeadlessSessionImpl) session).getSession();
                        return oldCommand.execute(commandSession, arguments);
                    }
                };
                registry.register(command);
                return command;
            }
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        if (service instanceof org.apache.karaf4.shell.api.console.Command) {
            registry.unregister(service);
        }
        context.ungetService(reference);
    }
}
