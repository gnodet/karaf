package org.apache.karaf4.shell.impl.console;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.support.completers.StringsCompleter;

public class CommandNamesCompleter extends org.apache.karaf4.shell.support.completers.CommandNamesCompleter {

    @Override
    public int complete(Session session, String buffer, int cursor, List<String> candidates) {
        // TODO: optimize
        List<Command> list = session.getRegistry().getCommands();
        Set<String> names = new HashSet<String>();
        for (Command command : list) {
            names.add(command.getScope() + ":" + command.getName());
            names.add(command.getName());
        }
        int res = new StringsCompleter(names).complete(session, buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

}
