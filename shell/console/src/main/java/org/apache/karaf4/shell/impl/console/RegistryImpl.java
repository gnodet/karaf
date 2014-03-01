package org.apache.karaf4.shell.impl.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Registry;

public class RegistryImpl implements Registry {

    private final Registry parent;
    private List<Object> services = new ArrayList<Object>();

    public RegistryImpl(Registry parent) {
        this.parent = parent;
    }

    @Override
    public List<Command> getCommands() {
        return getServices(Command.class);
    }

    @Override
    public void register(Object service) {
        synchronized (services) {
            services.add(service);
        }
    }

    @Override
    public void unregister(Object service) {
        synchronized (services) {
            services.remove(service);
        }
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        synchronized (services) {
            for (Object service : services) {
                if (clazz.isInstance(service)) {
                    return clazz.cast(service);
                }
            }
        }
        if (parent != null) {
            return parent.getService(clazz);
        }
        return null;
    }

    @Override
    public <T> List<T> getServices(Class<T> clazz) {
        List<T> list = new ArrayList<T>();
        synchronized (services) {
            for (Object service : services) {
                if (clazz.isInstance(service)) {
                    list.add(clazz.cast(service));
                }
            }
        }
        if (parent != null) {
            list.addAll(parent.getServices(clazz));
        }
        return list;
    }

}
