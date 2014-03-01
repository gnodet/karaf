package org.apache.karaf4.shell.impl.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf4.shell.api.console.History;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.api.console.Terminal;

public class HeadlessSessionImpl implements Session {

    private boolean quit;

    SessionFactory factory;
    CommandSession session;
    Registry registry;

    public HeadlessSessionImpl(SessionFactory factory, CommandSession session) {
        this.factory = factory;
        this.session = session;
        this.registry = new RegistryImpl(factory.getRegistry());
    }

    public CommandSession getSession() {
        return session;
    }

    @Override
    public Object execute(CharSequence commandline) throws Exception {
        return session.execute(commandline);
    }

    @Override
    public Object get(String name) {
        return session.get(name);
    }

    @Override
    public void put(String name, Object value) {
        session.put(name, value);
    }

    @Override
    public InputStream getKeyboard() {
        return session.getKeyboard();
    }

    @Override
    public PrintStream getConsole() {
        return session.getConsole();
    }

    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        // TODO: handle mask
        InputStream in = getKeyboard();
        PrintStream out = getConsole();

        StringBuilder sb = new StringBuilder();
        out.print(prompt);

        while (!quit)
        {
            out.flush();
            int c = in.read();

            switch (c)
            {
                case -1:
                case 4: // EOT, ^D from telnet
                    quit = true;
                    break;

                case '\r':
                    break;

                case '\n':
                    if (sb.length() > 0)
                    {
                        return sb.toString();
                    }
                    out.print(prompt);
                    break;

                case '\b':
                    if (sb.length() > 0)
                    {
                        out.print("\b \b");
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    break;

                default:
                    sb.append((char) c);
                    break;
            }
        }

        return null;
    }

    @Override
    public Terminal getTerminal() {
        return null;
    }

    @Override
    public History getHistory() {
        return null;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public SessionFactory getFactory() {
        return factory;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        quit = true;
        session.close();
    }

}
