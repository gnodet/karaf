package org.apache.karaf4.shell.impl.action.classloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.karaf4.shell.api.action.Action;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.impl.action.command.ActionCommand;

public class Discovery {

    private final Registry registry;

    public Discovery(Registry registry) {
        this.registry = registry;
    }

    public void discover(ClassLoader loader) throws IOException, ClassNotFoundException {
        discover(loader, "META-INF/services/org/apache/karaf/shell/commands");
    }

    public void discover(ClassLoader loader, String resource) throws IOException, ClassNotFoundException {
        Enumeration<URL> urls = loader.getResources(resource);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    final Class<?> actionClass = loader.loadClass(line);
                    if (!Action.class.isAssignableFrom(actionClass)) {
                        throw new IllegalArgumentException("Class " + actionClass.getName() + " does not implement " + Action.class.getName());
                    }
                    if (actionClass.getAnnotation(Command.class) == null) {
                        throw new IllegalArgumentException("Class " + actionClass.getName() + " is not annotated with @Command");
                    }
                    ActionCommand command = new ActionCommand((Class<? extends Action>) actionClass) {
                        @Override
                        protected Completer getCompleter(Class<?> clazz) {
                            // TODO
                            return null;
                        }
                        @Override
                        protected <T> T getDependency(Class<T> clazz) {
                            // TODO
                            return null;
                        }
                    };
                    registry.register(command);
                }
                line = r.readLine();
            }
            r.close();
        }
    }

    private static List<URL> getUrls(ClassLoader classLoader) throws IOException {
        List<URL> list = new ArrayList<URL>();
        ArrayList<URL> urls = Collections.list(classLoader.getResources("META-INF"));
        for (URL url : urls) {
            String externalForm = url.toExternalForm();
            int i = externalForm.lastIndexOf("META-INF");
            externalForm = externalForm.substring(0, i);
            url = new URL(externalForm);
            list.add(url);
        }
        list.addAll(Collections.list(classLoader.getResources("")));
        return list;
    }

}
