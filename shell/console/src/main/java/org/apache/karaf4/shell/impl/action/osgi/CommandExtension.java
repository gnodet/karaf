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
package org.apache.karaf4.shell.impl.action.osgi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.extender.Extension;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf4.shell.api.action.Action;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.action.lifecycle.Destroy;
import org.apache.karaf4.shell.api.action.lifecycle.Init;
import org.apache.karaf4.shell.api.action.lifecycle.Reference;
import org.apache.karaf4.shell.api.action.lifecycle.Service;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.History;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.api.console.Terminal;
import org.apache.karaf4.shell.impl.action.command.ActionCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands extension
 */
public class CommandExtension implements Extension, Satisfiable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExtension.class);

    private final Bundle bundle;
    private final Registry registry;
    private final CountDownLatch started;
    private final MultiServiceTracker tracker;
    private final List<Satisfiable> satisfiables = new ArrayList<Satisfiable>();


    public CommandExtension(Bundle bundle, Registry registry) {
        this.bundle = bundle;
        this.registry = registry;
        this.started = new CountDownLatch(1);
        this.tracker = new MultiServiceTracker(bundle.getBundleContext(), this);
    }

    @Override
    public void found() {
        for (Satisfiable s : satisfiables) {
            s.found();
        }
    }

    @Override
    public void updated() {
        for (Satisfiable s : satisfiables) {
            s.updated();
        }
    }

    @Override
    public void lost() {
        for (Satisfiable s : satisfiables) {
            s.lost();
        }
    }

    public void start() throws Exception {
        try {
            String header = bundle.getHeaders().get(CommandExtender.KARAF_COMMANDS);
            Clause[] clauses = Parser.parseHeader(header);
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            for (Clause clause : clauses) {
                String name = clause.getName();
                int options = BundleWiring.LISTRESOURCES_LOCAL;
                name = name.replace('.', '/');
                if (name.endsWith("*")) {
                    options |= BundleWiring.LISTRESOURCES_RECURSE;
                    name = name.substring(0, name.length() - 1);
                }
                if (!name.startsWith("/")) {
                    name = "/" + name;
                }
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                Collection<String> classes = wiring.listResources(name, "*.class", options);
                for (String className : classes) {
                    className = className.replace('/', '.').replace(".class", "");
                    inspectClass(bundle.loadClass(className));
                }
            }
            tracker.open();
        } finally {
            started.countDown();
        }
    }

    public void destroy() {
        try {
            started.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("The wait for bundle being started before destruction has been interrupted.", e);
        }
        tracker.close();
    }

    private void inspectClass(final Class<?> clazz) throws Exception {
        Service reg = clazz.getAnnotation(Service.class);
        if (reg == null) {
            return;
        }
        if (Action.class.isAssignableFrom(clazz)) {
            final Command cmd = clazz.getAnnotation(Command.class);
            if (cmd == null) {
                throw new IllegalArgumentException("Command " + clazz.getName() + " is not annotated with @Command");
            }
            // Create trackers
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() != BundleContext.class
                                && field.getType() != Session.class
                                && field.getType() != Terminal.class
                                && field.getType() != History.class
                                && field.getType() != Registry.class
                                && field.getType() != SessionFactory.class) {
                            tracker.track(field.getType());
                        }
                    }
                }
            }
            satisfiables.add(new AutoRegisterCommand((Class<? extends Action>) clazz));
        }
        if (Completer.class.isAssignableFrom(clazz)) {
            // Create trackers
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() != BundleContext.class) {
                            tracker.track(field.getType());
                        }
                    }
                }
            }
            satisfiables.add(new AutoRegisterCompleter((Class<? extends Completer>) clazz));
        }
    }

    public class AutoRegisterCompleter implements Satisfiable {

        private final Class<? extends Completer> clazz;
        private Completer completer;

        public AutoRegisterCompleter(Class<? extends Completer> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void found() {
            try {
                // Create completer
                completer = clazz.newInstance();
                Set<String> classes = new HashSet<String>();
                // Inject services
                for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                    classes.add(cl.getName());
                    for (Class c : cl.getInterfaces()) {
                        classes.add(c.getName());
                    }
                    for (Field field : cl.getDeclaredFields()) {
                        if (field.getAnnotation(Reference.class) != null) {
                            Object value;
                            if (field.getType() == BundleContext.class) {
                                value = CommandExtension.this.bundle.getBundleContext();
                            } else {
                                value = CommandExtension.this.tracker.getService(field.getType());
                            }
                            if (value == null) {
                                throw new RuntimeException("No OSGi service matching " + field.getType().getName());
                            }
                            field.setAccessible(true);
                            field.set(completer, value);
                        }
                    }
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    Init ann = method.getAnnotation(Init.class);
                    if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                        method.setAccessible(true);
                        method.invoke(completer);
                    }
                }
                registry.register(completer);
            } catch (Exception e) {
                throw new RuntimeException("Unable to creation service " + clazz.getName(), e);
            }
        }

        @Override
        public void updated() {
            lost();
            found();
        }

        @Override
        public void lost() {
            if (completer != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    Destroy ann = method.getAnnotation(Destroy.class);
                    if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                        method.setAccessible(true);
                        try {
                            method.invoke(completer);
                        } catch (Exception e) {
                            LOGGER.warn("Error destroying service", e);
                        }
                    }
                }
                registry.unregister(completer);
            }
        }

    }

    public class AutoRegisterCommand extends ActionCommand implements Satisfiable {

        public AutoRegisterCommand(Class<? extends Action> actionClass) {
            super(actionClass);
        }

        @Override
        public void found() {
            registry.register(this);
        }

        @Override
        public void updated() {
        }

        @Override
        public void lost() {
            registry.unregister(this);
        }

        @Override
        protected <T> T getDependency(Class<T> clazz) {
            if (clazz == BundleContext.class) {
                return clazz.cast(CommandExtension.this.bundle.getBundleContext());
            }  else {
                return CommandExtension.this.tracker.getService(clazz);
            }
        }

        @Override
        protected Completer getCompleter(Class<?> clazz) {
            return new ProxyServiceCompleter(CommandExtension.this.bundle.getBundleContext(), clazz);
        }

    }

    public static class ProxyServiceCompleter implements Completer {
        private final BundleContext context;
        private final Class<?> clazz;

        public ProxyServiceCompleter(BundleContext context, Class<?> clazz) {
            this.context = context;
            this.clazz = clazz;
        }

        @Override
        public int complete(Session session, String buffer, int cursor, List<String> candidates) {
            Object service = session.getRegistry().getService(clazz);
            if (service instanceof Completer) {
                return ((Completer) service).complete(session, buffer, cursor, candidates);
            }
            ServiceReference<?> ref = context.getServiceReference(clazz);
            if (ref != null) {
                Object completer = context.getService(ref);
                if (completer instanceof Completer) {
                    try {
                        return ((Completer) completer).complete(session, buffer, cursor, candidates);
                    } finally {
                        context.ungetService(ref);
                    }
                }
            }
            return -1;
        }
    }

}
