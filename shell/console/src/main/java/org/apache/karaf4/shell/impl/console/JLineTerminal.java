package org.apache.karaf4.shell.impl.console;

import org.apache.karaf4.shell.api.console.Terminal;

/**
* Created by gnodet on 27/02/14.
*/
public class JLineTerminal implements Terminal {

    private final jline.Terminal terminal;

    public JLineTerminal(jline.Terminal terminal) {
        this.terminal = terminal;
    }

    public jline.Terminal getTerminal() {
        return terminal;
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
    public boolean isAnsiSupported() {
        return terminal.isAnsiSupported();
    }

    @Override
    public boolean isEchoEnabled() {
        return terminal.isEchoEnabled();
    }

    @Override
    public void setEchoEnabled(boolean enabled) {
        terminal.setEchoEnabled(enabled);
    }
}
