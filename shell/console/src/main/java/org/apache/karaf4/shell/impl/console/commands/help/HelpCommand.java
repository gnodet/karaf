package org.apache.karaf4.shell.impl.console.commands.help;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.util.properties.InterpolationHelper;
import org.apache.karaf4.shell.api.console.Command;
import org.apache.karaf4.shell.api.console.Completer;
import org.apache.karaf4.shell.api.console.Registry;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.console.SessionFactory;
import org.apache.karaf4.shell.support.CommandException;
import org.apache.karaf4.shell.support.completers.StringsCompleter;

import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf4.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public class HelpCommand implements Command {

    public HelpCommand(SessionFactory factory) {
        Registry registry = factory.getRegistry();
        registry.register(this);
        registry.register(new SimpleHelpProvider());
        registry.register(new SingleCommandHelpProvider());
        registry.register(new CommandListHelpProvider());
    }

    @Override
    public String getScope() {
        return "*";
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays this help or help about a command";
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        if (arguments.contains("--help")) {
            printHelp(System.out);
            return null;
        }
        if (arguments.size() > 1) {
            String msg = COLOR_RED
                    + "Error executing command "
                    + INTENSITY_BOLD + getName() + INTENSITY_NORMAL
                    + COLOR_DEFAULT + ": " + "too many arguments specified";
            throw new CommandException(msg);
        }
        String path = arguments.isEmpty() ? null : arguments.get(0) == null ? null : arguments.get(0).toString();
        String help = getHelp(session, path);
        if (help != null) {
            System.out.println(help);
        }
        return null;
    }

    @Override
    public Completer getCompleter(final boolean scoped) {
        return new Completer() {
            @Override
            public int complete(Session session, String buffer, int cursor, List<String> candidates) {
                // TODO: use CommandNamesCompleter and better completion wrt parsing etc...
                // TODO: also this completion method always display 'help ' before the posible completions
                StringsCompleter completer = new StringsCompleter();
                Set<String> completions = completer.getStrings();
                for (Command command : session.getRegistry().getCommands()) {
                    if (!"*".equals(command.getScope())) {
                        completions.add(getName() + " " + command.getScope() + ":" + command.getName());
                    }
                    completions.add(getName() + " " + command.getName());
                }
                completions.add(getName() + " --help");
                return completer.complete(session, buffer, cursor, candidates);
            }
        };
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
        out.println(getName() + " [options] [command]");
        out.println();
        out.println(INTENSITY_BOLD + "ARGUMENTS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "command" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Command to display help for");
        out.println();
        out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "--help" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Display this help message");
        out.println();
    }

    public String getHelp(final Session session, String path) {
        if (path == null) {
            path = "%root%";
        }
        Map<String,String> props = new HashMap<String,String>();
        props.put("data", "${" + path + "}");
        final List<HelpProvider> providers = session.getRegistry().getServices(HelpProvider.class);
        InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
            public String getValue(final String key) {
                for (HelpProvider hp : providers) {
                    String result = hp.getHelp(session, key);
                    if (result != null) {
                        return removeNewLine(result);
                    }
                }
                return null;
            }
        });
        return props.get("data");
    }

    private String removeNewLine(String help) {
        if (help != null && help.endsWith("\n")) {
            help = help.substring(0, help.length()  -1);
        }
        return help;
    }

}
