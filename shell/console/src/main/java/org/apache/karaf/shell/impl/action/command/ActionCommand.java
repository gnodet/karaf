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
package org.apache.karaf.shell.impl.action.command;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Destroy;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.converter.GenericType;
import org.apache.karaf.shell.support.converter.ReifiedType;

public class ActionCommand implements org.apache.karaf.shell.api.console.Command {

    private final Class<? extends Action> actionClass;

    public ActionCommand(Class<? extends Action> actionClass) {
        this.actionClass = actionClass;
    }

    public Class<? extends Action> getActionClass() {
        return actionClass;
    }

    @Override
    public String getScope() {
        return actionClass.getAnnotation(Command.class).scope();
    }

    @Override
    public String getName() {
        return actionClass.getAnnotation(Command.class).name();
    }

    @Override
    public String getDescription() {
        return actionClass.getAnnotation(Command.class).description();
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        return new ArgumentCompleter(this, scoped);
    }

    protected Completer getCompleter(Class<?> clazz) {
        return new DelayedCompleter(clazz);
    }

    protected Object getDependency(ReifiedType type) {
        throw new UnsupportedOperationException("Completers are not supported");
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        Action action = createNewAction(session);
        try {
            if (new DefaultActionPreparator().prepare(action, session, arguments)) {
                return action.execute();
            }
        } finally {
            releaseAction(action);
        }
        return null;
    }

    protected Action createNewAction(Session session) {
        try {
            Action action = actionClass.newInstance();
            // Inject services
            for (Class<?> cl = actionClass; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        Object value;
                        if (field.getType() == Session.class) {
                            value = session;
                        } else if (field.getType() == Terminal.class) {
                            value = session.getTerminal();
                        } else if (field.getType() == History.class) {
                            value = session.getHistory();
                        } else if (field.getType() == Registry.class) {
                            value = session.getRegistry();
                        } else if (field.getType() == SessionFactory.class) {
                            value = session.getFactory();
                        } else {
                            value = getDependency(new GenericType(field.getGenericType()));
                        }
                        if (value == null) {
                            throw new RuntimeException("No OSGi service matching " + field.getType().getName());
                        }
                        field.setAccessible(true);
                        field.set(action, value);
                    }
                }
            }
            for (Method method : actionClass.getDeclaredMethods()) {
                Init ann = method.getAnnotation(Init.class);
                if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                    method.setAccessible(true);
                    method.invoke(action);
                }
            }
            return action;
        } catch (Exception e) {
            throw new RuntimeException("Unable to creation command action " + actionClass.getName(), e);
        }
    }

    protected void releaseAction(Action action) throws Exception {
        for (Method method : actionClass.getDeclaredMethods()) {
            Destroy ann = method.getAnnotation(Destroy.class);
            if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                method.setAccessible(true);
                method.invoke(action);
            }
        }
    }

    public static class DelayedCompleter implements Completer {
        private final Class<?> clazz;

        public DelayedCompleter(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
            Object service = session.getRegistry().getServices(clazz);
            if (service instanceof Completer) {
                return ((Completer) service).complete(session, commandLine, candidates);
            }
            return -1;
        }
    }

}
