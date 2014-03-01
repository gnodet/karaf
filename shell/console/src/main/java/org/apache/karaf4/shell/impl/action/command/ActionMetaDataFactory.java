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
package org.apache.karaf4.shell.impl.action.command;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf4.shell.api.action.Action;
import org.apache.karaf4.shell.api.action.Argument;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.action.Option;


public class ActionMetaDataFactory {

    public ActionMetaData create(Class<? extends Action> actionClass) {
        Command command = getCommand(actionClass);
        Map<Option, Field> options = new HashMap<Option, Field>();
        Map<Argument, Field> arguments = new HashMap<Argument, Field>();
        List<Argument> orderedArguments = new ArrayList<Argument>();

        for (Class<?> type = actionClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    options.put(option, field);
                }

                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    argument = replaceDefaultArgument(field, argument);
                    arguments.put(argument, field);
                    int index = argument.index();
                    while (orderedArguments.size() <= index) {
                        orderedArguments.add(null);
                    }
                    if (orderedArguments.get(index) != null) {
                        throw new IllegalArgumentException("Duplicate argument index: " + index + " on Action " + actionClass.getName());
                    }
                    orderedArguments.set(index, argument);
                }
            }
        }
        assertIndexesAreCorrect(actionClass, orderedArguments);

        return new ActionMetaData(actionClass, command, options, arguments, orderedArguments, null);
    }

    public Command getCommand(Class<? extends Action> actionClass) {
        return actionClass.getAnnotation(Command.class);
    }

    private Argument replaceDefaultArgument(Field field, Argument argument) {
        if (Argument.DEFAULT.equals(argument.name())) {
            final Argument delegate = argument;
            final String name = field.getName();
            argument = new Argument() {
                public String name() {
                    return name;
                }

                public String description() {
                    return delegate.description();
                }

                public boolean required() {
                    return delegate.required();
                }

                public int index() {
                    return delegate.index();
                }

                public boolean multiValued() {
                    return delegate.multiValued();
                }

                public String valueToShowInHelp() {
                    return delegate.valueToShowInHelp();
                }

                public Class<? extends Annotation> annotationType() {
                    return delegate.annotationType();
                }
            };
        }
        return argument;
    }
    
    private void assertIndexesAreCorrect(Class<? extends Action> actionClass, List<Argument> orderedArguments) {
        for (int i = 0; i < orderedArguments.size(); i++) {
            if (orderedArguments.get(i) == null) {
                throw new IllegalArgumentException("Missing argument for index: " + i + " on Action " + actionClass.getName());
            }
        }
    }
}
