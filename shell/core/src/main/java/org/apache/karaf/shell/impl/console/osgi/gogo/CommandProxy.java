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

import java.util.List;

import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;

public class CommandProxy implements Command {
    private BundleContext context;
    private ServiceReference reference;
    private String function;
    private Object target;
    private String description;

    public CommandProxy(BundleContext context, ServiceReference reference, String function) {
        this.context = context;
        this.reference = reference;
        this.function = function;
    }

    @Override
    public String getScope() {
        return reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
    }

    @Override
    public String getName() {
        return function;
    }

    public Object getTarget() {
        return (context != null ? context.getService(reference) : target);
    }

    public void ungetTarget() {
        if (context != null) {
            try {
                context.ungetService(reference);
            } catch (IllegalStateException e) {
                // ignore - probably due to shutdown
                // java.lang.IllegalStateException: BundleContext is no longer valid
            }
        }
    }

    public Object execute(Session session, List<Object> arguments)
            throws Exception {

        // TODO: Check for --help option

        CommandSession commandSession = (CommandSession) session.get(".commandSession");
        Object tgt = getTarget();
        try {
            if (tgt instanceof Function) {
                return ((Function) tgt).execute(commandSession, arguments);
            } else {
                return Reflective.invoke(commandSession, tgt, function, arguments);
            }
        } finally {
            ungetTarget();
        }
    }

    @Override
    public String getDescription() {
        if (description == null) {
            try {
                Object tgt = getTarget();
                try {
                    Descriptor descriptor = tgt.getClass().getAnnotation(Descriptor.class);
                    if (descriptor != null) {
                        description = descriptor.value();
                    }
                } finally {
                    ungetTarget();
                }
            } catch (Throwable t) {
                // Ignore
            }
            if (description == null) {
                description = getScope() + ":" + getName();
            }
        }
        return description;
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        // TODO: generate completer from @Description and @Parameter annotations
        return null;
    }

}
