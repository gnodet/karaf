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
package org.apache.karaf.features.command;

import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.InstalledFeatureCompleter;
import org.apache.karaf4.shell.api.action.Argument;
import org.apache.karaf4.shell.api.action.Command;
import org.apache.karaf4.shell.api.action.Completion;
import org.apache.karaf4.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "uninstall", description = "Uninstalls a feature with the specified name and version.")
@Service
public class UninstallFeatureCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "features", description = "The name and version of the features to uninstall. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    @Completion(InstalledFeatureCompleter.class)
    List<String> features;

    protected void doExecute(FeaturesService admin) throws Exception {
        // iterate in the provided feature
        for (String feature : features) {
            String[] split = feature.split("/");
            String name = split[0];
            String version = null;
            if (split.length == 2) {
                version = split[1];
            }
    	    if (version != null && version.length() > 0) {
    		    admin.uninstallFeature(name, version);
    	    } else {
    		    admin.uninstallFeature(name );
    	    }
        }
    }
}
