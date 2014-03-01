package org.apache.karaf4.shell.impl.console;

import jline.TerminalSupport;
import org.apache.karaf4.shell.api.console.Terminal;

public class KarafTerminal extends TerminalSupport {

    private final Terminal terminal;

    public KarafTerminal(Terminal terminal) {
        super(true);
        this.terminal = terminal;
    }

    @Override
    public synchronized boolean isAnsiSupported() {
        return terminal.isAnsiSupported();
    }

    @Override
    public int getWidth() {
        return terminal.getWidth();
    }

    @Override
    public int getHeight() {
        return terminal.getHeight();
    }

    @Override
    public synchronized boolean isEchoEnabled() {
        return terminal.isEchoEnabled();
    }

    @Override
    public synchronized void setEchoEnabled(boolean enabled) {
        terminal.setEchoEnabled(enabled);
    }
}
