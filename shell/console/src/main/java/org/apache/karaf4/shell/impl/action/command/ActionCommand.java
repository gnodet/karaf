package org.apache.karaf4.shell.impl.action.command;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf4.shell.api.action.Action;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.action.lifecycle.Destroy;
import org.apache.karaf4.shell.api.action.lifecycle.Init;
import org.apache.karaf4.shell.api.action.lifecycle.Reference;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.History;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.api.console.Terminal;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ActionCommand implements org.apache.karaf4.shell.api.console.Command {

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

    protected <T> T getDependency(Class<T> clazz) {
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
                        } else if (field.getType() == List.class) {
                            // TODO
                            value = new ArrayList();
                        } else {
                            value = getDependency(field.getType());
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
        public int complete(Session session, String buffer, int cursor, List<String> candidates) {
            Object service = session.getRegistry().getServices(clazz);
            if (service instanceof Completer) {
                return ((Completer) service).complete(session, buffer, cursor, candidates);
            }
            return -1;
        }
    }

}
