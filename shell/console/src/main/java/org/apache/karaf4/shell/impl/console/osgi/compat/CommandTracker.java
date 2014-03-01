package org.apache.karaf4.shell.impl.console.osgi.compat;

import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.impl.console.ConsoleSessionImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Created by gnodet on 27/02/14.
 */
public class CommandTracker implements BundleActivator, ServiceTrackerCustomizer {

    Registry registry;
    BundleContext context;
    ServiceTracker tracker;

    public CommandTracker(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))",
                CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));
        this.tracker = new ServiceTracker(context, filter, this);
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
                            public int complete(Session session, String buffer, int cursor, List<String> candidates) {
                                return completer.complete(buffer, cursor, candidates);
                            }
                        };
                    }

                    @Override
                    public Object execute(Session session, List<Object> arguments) throws Exception {
                        CommandSession commandSession = ((ConsoleSessionImpl) session).getSession();
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
            registry.unregister((org.apache.karaf4.shell.api.console.Command) service);
        }
        context.ungetService(reference);
    }
}
