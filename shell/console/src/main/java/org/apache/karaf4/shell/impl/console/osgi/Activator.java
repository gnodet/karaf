package org.apache.karaf4.shell.impl.console.osgi;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.impl.action.osgi.CommandExtender;
import org.apache.karaf4.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf4.shell.impl.console.TerminalFactory;
import org.apache.karaf4.shell.impl.console.osgi.compat.CommandTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private static final String START_CONSOLE = "karaf.startLocalConsole";

    private ThreadIOImpl threadIO;

    private SessionFactoryImpl consoleFactory;
    private ServiceRegistration consoleFactoryRegistration;

    private CommandExtender actionExtender;

    private TerminalFactory terminalFactory;
    private LocalConsoleManager localConsoleManager;

    private CommandTracker commandTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        threadIO = new ThreadIOImpl();
        threadIO.start();

        consoleFactory = new SessionFactoryImpl(threadIO);
        consoleFactory.getCommandProcessor().addConverter(new Converters(context));
        consoleFactory.getCommandProcessor().addConstant(".context", context.getBundle(0).getBundleContext());
        consoleFactoryRegistration = context.registerService(SessionFactory.class.getName(), consoleFactory, null);

        actionExtender = new CommandExtender(consoleFactory);
        actionExtender.start(context);

        commandTracker = new CommandTracker(consoleFactory);
        commandTracker.start(context);

        if (Boolean.parseBoolean(context.getProperty(START_CONSOLE))) {
            terminalFactory = new TerminalFactory();
            localConsoleManager = new LocalConsoleManager(context, terminalFactory, consoleFactory);
            localConsoleManager.start();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        consoleFactoryRegistration.unregister();
        consoleFactory.stop();
        localConsoleManager.stop();
        actionExtender.stop(context);
        commandTracker.stop(context);
        threadIO.stop();
        terminalFactory.destroy();
    }
}
