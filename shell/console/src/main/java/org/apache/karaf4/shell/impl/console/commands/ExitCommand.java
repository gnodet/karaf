package org.apache.karaf4.shell.impl.console.commands;

import org.apache.karaf4.shell.api.console.Session;

public class ExitCommand extends TopLevelCommand {

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "Exit from the current shell";
    }

    @Override
    protected void doExecute(Session session) throws Exception {
        // get the current sub-shell
        String currentSubShell = (String) session.get(Session.SUBSHELL);
        if (!currentSubShell.isEmpty()) {
            if (currentSubShell.contains(":")) {
                int index = currentSubShell.lastIndexOf(":");
                session.put(Session.SUBSHELL, currentSubShell.substring(0, index));
            } else {
                session.put(Session.SUBSHELL, "");
            }
            String currentScope = (String) session.get(Session.SCOPE);
            int index = currentScope.indexOf(":");
            session.put(Session.SCOPE, currentScope.substring(index + 1));
        }
    }
}
