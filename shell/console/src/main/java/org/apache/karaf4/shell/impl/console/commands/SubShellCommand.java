package org.apache.karaf4.shell.impl.console.commands;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.karaf4.shell.api.console.Session;

public class SubShellCommand extends TopLevelCommand {

    private final String name;
    private final AtomicInteger references = new AtomicInteger();

    public SubShellCommand(String name) {
        this.name = name;
    }

    public void increment() {
        references.incrementAndGet();
    }

    public int decrement() {
        return references.decrementAndGet();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Enter the subshell";
    }

    @Override
    protected void doExecute(Session session) throws Exception {
        session.put(Session.SUBSHELL, name);
        session.put(Session.SCOPE, name + ":" + session.get(Session.SCOPE));
    }

}
