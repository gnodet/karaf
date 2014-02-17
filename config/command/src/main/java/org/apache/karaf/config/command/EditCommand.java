/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.command;

import java.util.Dictionary;

import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Completer;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.inject.Service;
import org.osgi.service.cm.Configuration;

@Command(scope = "config", name = "edit", description = "Creates or edits a configuration.", detailedDescription="classpath:edit.txt")
@Service
public class EditCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "pid", description = "PID of the configuration or of the factory if --factory is given. Pid can also be specified as ldap query", required = true, multiValued = false)
    @Completer(ConfigurationCompleter.class)
    String pid;

    @Option(name = "--force", aliases = {}, description = "Force the edition of this config, even if another one was under edition", required = false, multiValued = false)
    boolean force;
    
    @Option(name = "--factory", aliases = {}, description = "Define this config as a factory config. Will be crearted on calling update", required = false, multiValued = false)
    boolean factory;

    @SuppressWarnings("rawtypes")
    protected Object doExecute() throws Exception {
        String oldPid = (String) this.session.get(PROPERTY_CONFIG_PID);
        if (oldPid != null && !oldPid.equals(pid) && !force) {
            System.err.println("Another config is being edited.  Cancel / update first, or use the --force option");
            return null;
        }
        
        if (pid.startsWith("(")) {
        	Configuration[] configs = this.configRepository.getConfigAdmin().listConfigurations(pid);
        	if (configs.length == 0) {
        		throw new RuntimeException("Filter matches no config");
        	}
        	if (configs.length > 1) {
        		throw new RuntimeException("Filter matches more than one config");
        	}
        	pid = configs[0].getPid();
        	System.out.println("Editing config " + pid);
        }

        Dictionary props = this.configRepository.getConfigProperties(pid);
        this.session.put(PROPERTY_CONFIG_PID, pid);
        this.session.put(PROPERTY_FACTORY, factory);
        this.session.put(PROPERTY_CONFIG_PROPS, props);
        return null;
    }

}
