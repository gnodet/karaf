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
package org.apache.karaf.bundle.command;

import org.apache.karaf4.shell.api.action.Argument;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.console.Session;
import org.apache.karaf4.shell.api.action.lifecycle.Reference;
import org.apache.karaf4.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

@Command(scope = "bundle", name = "start-level", description = "Gets or sets the start level of a bundle.")
@Service
public class StartLevel extends BundleCommandWithConfirmation {

    @Argument(index = 1, name = "startLevel", description = "The bundle's new start level", required = false, multiValued = false)
    Integer level;

    @Reference
    Session session;

    protected void doExecute(Bundle bundle) throws Exception {
        // Get package instance service.
        BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
        if (bsl == null) {
            System.out.println("StartLevel service is unavailable.");
            return;
        }
        if (level == null) {
            System.out.println("Level " + bsl.getStartLevel());
        }
        else if ((level < 50) && (bsl.getStartLevel() > 50) && !force){
            for (;;) {
                String msg = "You are about to designate bundle as a system bundle.  Do you wish to continue (yes/no): ";
                String str = session.readLine(msg, null);
                if ("yes".equalsIgnoreCase(str)) {
                    bsl.setStartLevel(level);
                    break;
                } else if ("no".equalsIgnoreCase(str)) {
                    break;
                }
            }

        } else {
            bsl.setStartLevel(level);
        }
    }

}
