/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.karaf.jaas.modules.JaasHelper;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

/**
 * SSHD {@link org.apache.sshd.server.Command} factory which provides access to
 * Shell.
 */
public class ShellFactoryImpl implements Factory<Command> {
    private SessionFactory sessionFactory;

    public ShellFactoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Command create() {
        return new ShellImpl();
    }

    public class ShellImpl implements Command, SessionAware {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private ServerSession session;

        private boolean closed;

        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void setSession(ServerSession session) {
            this.session = session;
        }

        public void start(final Environment env) throws IOException {
            try {
                final Subject subject = ShellImpl.this.session != null ? ShellImpl.this.session
                        .getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY) : null;
                final Terminal terminal = new SshTerminal(env);
                Runnable destroyCallback = new Runnable() {
                    public void run() {
                        destroy();
                    }
                };
                String encoding = env.getEnv().get("LC_CTYPE");
                if (encoding != null && encoding.indexOf('.') > 0) {
                    encoding = encoding.substring(encoding.indexOf('.') + 1);
                }
                final Session session = sessionFactory.create(in,
                        lfToCrLfPrintStream(out), lfToCrLfPrintStream(err), terminal, encoding, destroyCallback);
                for (Map.Entry<String, String> e : env.getEnv().entrySet()) {
                    session.put(e.getKey(), e.getValue());
                }
                JaasHelper.doAs(subject, new PrivilegedAction<Object>() {
                    public Object run() {
                        new Thread(session, "Karaf ssh console user " + ShellUtil.getCurrentUserName()).start();
                        return null;
                    }
                });
            } catch (Exception e) {
                throw (IOException) new IOException("Unable to start shell").initCause(e);
            }
        }

        private PrintStream lfToCrLfPrintStream(OutputStream stream) {
            return new PrintStream(new LfToCrLfFilterOutputStream(stream), true);
        }

        public void destroy() {
            if (!closed) {
                closed = true;
                ShellFactoryImpl.flush(out, err);
                ShellFactoryImpl.close(in, out, err);
                callback.onExit(0);
            }
        }

    }

    private static void flush(OutputStream... streams) {
        for (OutputStream s : streams) {
            try {
                s.flush();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    // TODO: remove this class when sshd use lf->crlf conversion by default
    public class LfToCrLfFilterOutputStream extends FilterOutputStream {

        private boolean lastWasCr;

        public LfToCrLfFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (!lastWasCr && b == '\n') {
                out.write('\r');
                out.write('\n');
            } else {
                out.write(b);
            }
            lastWasCr = b == '\r';
        }

    }

}
