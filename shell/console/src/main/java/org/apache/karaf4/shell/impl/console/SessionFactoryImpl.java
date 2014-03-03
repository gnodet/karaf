package org.apache.karaf4.shell.impl.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.api.console.Terminal;
import org.apache.karaf4.shell.impl.console.commands.ExitCommand;
import org.apache.karaf4.shell.impl.console.commands.SubShellCommand;
import org.apache.karaf4.shell.impl.console.commands.help.HelpCommand;
import org.apache.karaf4.shell.support.ShellUtil;

public class SessionFactoryImpl extends RegistryImpl implements SessionFactory, Registry {

    final CommandProcessorImpl commandProcessor;
    final ThreadIO threadIO;
    final List<Session> sessions = new ArrayList<Session>();
    final Map<String, SubShellCommand> subshells = new HashMap<String, SubShellCommand>();
    boolean closed;

    public SessionFactoryImpl(ThreadIO threadIO) {
        super(null);
        this.threadIO = threadIO;
        commandProcessor = new CommandProcessorImpl(threadIO);
        register(new ExitCommand());
        new HelpCommand(this);
    }

    public CommandProcessorImpl getCommandProcessor() {
        return commandProcessor;
    }

    @Override
    public Registry getRegistry() {
        return this;
    }

    @Override
    public void register(Object service) {
        if (service instanceof Command) {
            Command command = (Command) service;
            String scope = command.getScope();
            String name = command.getName();
            if (!Session.SCOPE_GLOBAL.equals(scope)) {
                if (!subshells.containsKey(scope)) {
                    SubShellCommand subShell = new SubShellCommand(scope);
                    subshells.put(scope, subShell);
                    register(subShell);
                }
                subshells.get(scope).increment();
            }
            commandProcessor.addCommand(scope, new CommandWrapper(command), name);
        }
        super.register(service);
    }

    @Override
    public void unregister(Object service) {
        super.unregister(service);
        if (service instanceof Command) {
            Command command = (Command) service;
            String scope = command.getScope();
            String name = command.getName();
            commandProcessor.removeCommand(scope, name);
            if (!Session.SCOPE_GLOBAL.equals(scope)) {
                if (subshells.get(scope).decrement() == 0) {
                    SubShellCommand subShell = subshells.remove(scope);
                    unregister(subShell);
                }
            }
        }
    }

    @Override
    public Session create(InputStream in, PrintStream out, PrintStream err, Terminal term, String encoding, Runnable closeCallback) {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("ConsoleFactory has been closed");
            }
            final Session session = new ConsoleSessionImpl(this, commandProcessor, threadIO, in, out, err, term, encoding, closeCallback);
            final Terminal terminal = session.getTerminal();
            session.put("USER", ShellUtil.getCurrentUserName());
            session.put("APPLICATION", System.getProperty("karaf.name", "root"));
            session.put("#LINES", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getHeight());
                }
            });
            session.put("#COLUMNS", new Function() {
                public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                    return Integer.toString(terminal.getWidth());
                }
            });
            session.put(".jline.terminal", terminal);
            addSystemProperties(session);
            session.put("pid", getPid());
            sessions.add(session);
            return session;
        }
    }

    @Override
    public Session createSession(InputStream in, PrintStream out, PrintStream err) {
        CommandSession session = commandProcessor.createSession(in, out, err);
        Session s = new HeadlessSessionImpl(this, session);
        session.put(".session", s);
        return s;
    }

    private String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = name.split("@");
        return parts[0];
    }

    private void addSystemProperties(Session session) {
        Properties sysProps = System.getProperties();
        Iterator<Object> it = sysProps.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            session.put(key, System.getProperty(key));
        }
    }

    public void stop() {
        synchronized (this) {
            closed = true;
            for (Session session : sessions) {
                session.close();
            }
            commandProcessor.stop();
        }
    }

}
