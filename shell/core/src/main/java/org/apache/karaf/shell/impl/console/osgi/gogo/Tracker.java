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
package org.apache.karaf.shell.impl.console.osgi.gogo;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Tracker extends ServiceTracker<Object, List<Command>> {

    private final SessionFactory sessionFactory;

    public Tracker(BundleContext context, SessionFactory sessionFactory) throws InvalidSyntaxException {
        super(context, context.createFilter(String.format("(&(%s=*)(%s=*))",
                CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION)), null);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Command> addingService(ServiceReference reference) {
        Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
        Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
        List<Command> commands = new ArrayList<>();

        if (scope != null && function != null) {
            if (function.getClass().isArray()) {
                for (Object f : ((Object[]) function)) {
                    Command target = new CommandProxy(context, reference, f.toString());
                    sessionFactory.getRegistry().register(target);
                    commands.add(target);
                }
            } else {
                Command target = new CommandProxy(context, reference, function.toString());
                sessionFactory.getRegistry().register(target);
                commands.add(target);
            }
            return commands;
        }
        return null;
    }

    @Override
    public void removedService(ServiceReference<Object> reference, List<Command> tracked) {
        for (Command command : tracked) {
            sessionFactory.getRegistry().unregister(command);
        }
        super.removedService(reference, tracked);
    }

}
