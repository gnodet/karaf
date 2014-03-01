package org.apache.karaf4.shell.impl.console;

import java.util.List;

import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Session;

public class CommandWrapper implements Function {

    private final Command command;

    public CommandWrapper(Command command) {
        this.command = command;
    }

    @Override
    public Object execute(final CommandSession commandSession, List<Object> arguments) throws Exception {
        // TODO: remove the hack for .session
        Session session = (Session) commandSession.get(".session");
        // When need to translate closures to a compatible type for the command
        for (int i = 0; i < arguments.size(); i++) {
            Object v = arguments.get(i);
            if (v instanceof Closure) {
                final Closure closure = (Closure) v;
                arguments.set(i, new org.apache.karaf4.shell.api.console.Function() {
                    @Override
                    public Object execute(Session session, List<Object> arguments) throws Exception {
                        return closure.execute(commandSession, arguments);
                    }
                });
            }
        }
        return command.execute(session, arguments);
    }

}
