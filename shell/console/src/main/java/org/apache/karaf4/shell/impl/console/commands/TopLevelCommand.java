package org.apache.karaf4.shell.impl.console.commands;

import java.io.PrintStream;
import java.util.List;

import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.support.CommandException;

import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public abstract class TopLevelCommand implements Command {

    @Override
    public String getScope() {
        return "*";
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        return null;
//        return new StringsCompleter(new String[] { getName() });
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        if (arguments.contains("--help")) {
            printHelp(System.out);
            return null;
        }
        if (!arguments.isEmpty()) {
            String msg = COLOR_RED
                    + "Error executing command "
                    + INTENSITY_BOLD + getName() + INTENSITY_NORMAL
                    + COLOR_DEFAULT + ": " + "too many arguments specified";
            throw new CommandException(msg);
        }
        doExecute(session);
        return null;
    }

    protected void printHelp(PrintStream out) {
        out.println(INTENSITY_BOLD + "DESCRIPTION" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + getName() + INTENSITY_NORMAL);
        out.println();
        out.print("\t");
        out.println(getDescription());
        out.println();
        out.println(INTENSITY_BOLD + "SYNTAX" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(getName() + " [options]");
        out.println();
        out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "--help" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Display this help message");
        out.println();
    }

    protected abstract void doExecute(Session session) throws Exception;

}
