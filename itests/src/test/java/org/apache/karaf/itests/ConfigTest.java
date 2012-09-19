/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ConfigTest extends KarafTestSupport {

    @Test
    public void listCommand() throws Exception {
        String configListOutput = executeCommand("config:list");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
        configListOutput = executeCommand("config:list \"(service.pid=org.apache.karaf.features)\"");
        System.out.println(configListOutput);
        assertFalse(configListOutput.isEmpty());
    }

    @Test
    public void listViaMBean() throws Exception {
        JMXConnector connector = null;
        try {
            connector = this.getJMXConnector();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            ObjectName name = new ObjectName("org.apache.karaf:type=config,name=root");
            List<String> configs = (List<String>) connection.invoke(name, "list", new Object[]{ }, new String[]{ });
            assertTrue(configs.size() > 0);
            assertTrue(configs.contains("org.apache.karaf.features"));
            Map<String, String> properties = (Map<String, String>) connection.invoke(name, "proplist", new Object[]{ "org.apache.karaf.features" }, new String[]{ "java.lang.String" });
            assertTrue(properties.keySet().size() > 0);
        } finally {
            if (connector != null)
                connector.close();
        }
    }

}
