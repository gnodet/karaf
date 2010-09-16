/**
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
package org.apache.karaf.shell.console.completer;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.basic.SimpleCommand;
import org.apache.felix.gogo.runtime.shell.CommandSessionImpl;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.junit.Test;
import org.osgi.service.command.CommandSession;

import static org.junit.Assert.*;

public class ArgumentCompleterTest {

    @Test
    public void testParser1() throws Exception {
        Parser parser = new Parser("echo foo | cat bar ; ta", 23);
        List<List<List<String>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(0, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(2, parser.c3);
    }

    @Test
    public void testParser2() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta", 23);
        List<List<List<String>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(2, parser.c3);
    }

    @Test
    public void testParser3() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta", 22);
        List<List<List<String>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(0, parser.c2);
        assertEquals(1, parser.c3);
    }

    @Test
    public void testParser4() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta reta", 27);
        List<List<List<String>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(1, parser.c2);
        assertEquals(3, parser.c3);
    }

    @Test
    public void testParser5() throws Exception {
        Parser parser = new Parser("echo foo ; cat bar | ta reta", 24);
        List<List<List<String>>> p = parser.program();
        assertEquals(1, parser.c0);
        assertEquals(1, parser.c1);
        assertEquals(1, parser.c2);
        assertEquals(0, parser.c3);
    }

    @Test
    public void testCompleteOptions() throws Exception {
        CommandSession session = new DummyCommandSession();
        Completer comp = new ArgumentCompleter(session, new MyFunction(), "my:action");
        assertEquals(Arrays.asList("--check", "--foo", "--help", "-c", "-f"), complete(comp, "action -"));
        assertEquals(Arrays.asList(), complete(comp, "action --foo "));
        assertEquals(Arrays.asList("action "), complete(comp, "acti"));
        assertEquals(Arrays.asList("my:action "), complete(comp, "my:ac"));
        assertEquals(Arrays.asList("--foo "), complete(comp, "action --f"));
        assertEquals(Arrays.asList("--help "), complete(comp, "action --h"));
        assertEquals(Arrays.asList("-c "), complete(comp, "action -c"));
        assertEquals(Arrays.asList("--check "), complete(comp, "action -f 2 --c"));
        assertEquals(Arrays.asList("foo1 "), complete(comp, "action -f 2 --check foo1"));
        assertEquals(Arrays.asList("bar1", "bar2"), complete(comp, "action -f 2 --check foo1 "));
    }

    protected List<String> complete(Completer completer, String buf) {
        List<String> candidates = new ArrayList<String>();
        completer.complete(buf, buf.length(), candidates);
        return candidates;
    }

    public static class MyFunction extends SimpleCommand implements CompletableFunction {
        public MyFunction() {
            super(MyAction.class);
        }
        public List<Completer> getCompleters() {
            return Arrays.<Completer>asList(
                    new StringsCompleter(Arrays.asList("foo1", "foo2")),
                    new StringsCompleter(Arrays.asList("bar1", "bar2"))
            );
        }
    }

    public static class MyAction implements Action {
        @Option(name = "-f", aliases = { "--foo" })
        int f;
        @Option(name = "-c", aliases = "--check")
        boolean check;

        public Object execute(CommandSession session) throws Exception {
            return null;
        }
    }

    protected static class DummyCommandSession implements CommandSession {
        public Object convert(Class<?> type, Object instance) {
            return null;
        }
        public CharSequence format(Object target, int level) {
            return null;
        }
        public void put(String name, Object value) {
        }
        public Object get(String name) {
            return null;
        }
        public PrintStream getConsole() {
            return null;
        }
        public InputStream getKeyboard() {
            return null;
        }
        public void close() {
        }
        public Object execute(CharSequence commandline) throws Exception {
            return null;
        }
    }

}
