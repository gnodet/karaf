package org.apache.karaf4.shell.impl.console.commands.help;

import org.apache.karaf4.shell.api.console.Session;

public interface HelpProvider {

    String getHelp(Session session, String path);

}
