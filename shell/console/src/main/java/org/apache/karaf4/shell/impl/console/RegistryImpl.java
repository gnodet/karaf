package org.apache.karaf4.shell.impl.console;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Registry;

public class RegistryImpl implements Registry {

    private final Registry parent;
    private final Map<Object, Object> services = new LinkedHashMap<Object, Object>();

    public RegistryImpl(Registry parent) {
        this.parent = parent;
    }

    @Override
    public List<Command> getCommands() {
        return getServices(Command.class);
    }

    @Override
    public <T> void register(Callable<T> factory, Class<T> clazz) {
        synchronized (services) {
            services.put(factory, new Factory<T>(clazz, factory));
        }
    }

    @Override
    public void register(Object service) {
        synchronized (services) {
            services.put(service, service);
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
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory) service).clazz)) {
                        try {
                            return clazz.cast(((Factory) service).callable.call());
                        } catch (Exception e) {
                            // TODO: log exception
                        }
                    }
                } else if (clazz.isInstance(service)) {
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
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory) service).clazz)) {
                        try {
                            list.add(clazz.cast(((Factory) service).callable.call()));
                        } catch (Exception e) {
                            // TODO: log exception
                        }
                    }
                } else if (clazz.isInstance(service)) {
                    list.add(clazz.cast(service));
                }
            }
        }
        if (parent != null) {
            list.addAll(parent.getServices(clazz));
        }
        return list;
    }

    @Override
    public boolean hasService(Class<?> clazz) {
        synchronized (services) {
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory) service).clazz)) {
                        return true;
                    }
                } else if (clazz.isInstance(service)) {
                    return true;
                }
            }
        }
        if (parent != null) {
            return parent.hasService(clazz);
        }
        return false;
    }

    static class Factory<T> {

        final Class<T> clazz;
        final Callable<T> callable;

        Factory(Class<T> clazz, Callable<T> callable) {
            this.clazz = clazz;
            this.callable = callable;
        }

    }

}
