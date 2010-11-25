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
package org.apache.karaf.shell.itests;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.testing.AbstractIntegrationTest;
import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.apache.karaf.testing.Helper.felixProvisionalApis;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;

//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

@RunWith(JUnit4TestRunner.class)
public class CoreTest extends AbstractIntegrationTest {

    @Inject
    BundleContext bundleContext;

    @Test
    public void testHelp() throws Exception {
        getOsgiService(Function.class, "(&(osgi.command.scope=osgi)(osgi.command.function=list))", 10000);

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(in, new PrintStream(out), new PrintStream(err));
        cs.execute("osgi:list --help");
        cs.close();

        System.out.println(out.toString());
        System.err.println(err.toString());
    }

    @Test
    public void testInstallCommand() throws Exception {

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(in, new PrintStream(out), new PrintStream(err));

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        Bundle b = getInstalledBundle("org.apache.karaf.shell.log");
        b.start();

        getOsgiService(Function.class, "(&(osgi.command.scope=log)(osgi.command.function=display))", 10000);
        Thread.sleep(500);

        cs.execute("log:display");

        b.stop();

        Thread.sleep(500);

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        cs.close();

        System.out.println(out.toString());
        System.err.println(err.toString());
    }

    @Configuration
    public static Option[] configuration() throws Exception {
        Option[] options = combine(
            // Default karaf environment
            Helper.getDefaultOptions(
                // this is how you set the default log level when using pax logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("TRACE")
            ),

//            workingDirectory("target/paxrunner/core/"),

            waitForFrameworkStartup(),

            // Test on both equinox and felix
            equinox(), felix(),

            felixProvisionalApis()
        );
        // Stop the shell log bundle
        Helper.findMaven(options, "org.apache.karaf.shell", "org.apache.karaf.shell.log").noStart();
        return options;
    }

    private static <T> T unwrap(T stream) {
        try {
            Method mth = stream.getClass().getMethod("getRoot", null);
            return (T) mth.invoke(stream, null);
        } catch (Throwable t) {
            return stream;
        }
    }

}
