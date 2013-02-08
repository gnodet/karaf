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
package org.apache.karaf.tooling.exam.options.configs;

import org.apache.karaf.tooling.exam.options.ConfigurationPointer;

/**
 * Pre configured property file pointers to the most commonly used properties in /etc/config.properties and
 * /etc/org.apache.karaf.management.cfg.
 */
public interface ManagementCfg {

    static final String FILE_PATH = "etc/org.apache.karaf.management.cfg";

    /**
     * Port of the registry for the exported RMI service
     */
    static final ConfigurationPointer RMI_REGISTRY_PORT = new CustomPropertiesPointer("rmiRegistryPort");

    /**
     * Port of the registry for the exported RMI service
     */
    static final ConfigurationPointer RMI_SERVER_PORT = new CustomPropertiesPointer("rmiServerPort");

    static class CustomPropertiesPointer extends ConfigurationPointer {

        public CustomPropertiesPointer(String key) {
            super(FILE_PATH, key);
        }

    }

}
