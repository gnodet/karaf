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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import jline.TerminalSupport;
import org.apache.sshd.common.PtyMode;
import org.apache.sshd.server.Environment;

public class SshTerminal extends TerminalSupport {

    private static final int DELETE = 127;
    private static final int BACKSPACE = '\b';

    private Environment environment;
    private String encoding = System.getProperty("input.encoding", "UTF-8");
    private boolean deleteSendsBackspace = false;
    private boolean backspaceSendsDelete = false;


    public SshTerminal(Environment environment) {
        super(true);
        setAnsiSupported(true);
        this.environment = environment;
        Integer verase = this.environment.getPtyModes().get(PtyMode.VERASE);
        deleteSendsBackspace = verase != null && verase == DELETE;
    }

    public void init() throws Exception {
    }

    public void restore() throws Exception {
    }

    @Override
    public int getWidth() {
        int width = 0;
        try {
            width = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_COLUMNS));
        } catch (Throwable t) {
            // Ignore
        }
        return width > 0 ? width : super.getWidth();
    }

    @Override
    public int getHeight() {
        int height = 0;
        try {
            height = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_LINES));
        } catch (Throwable t) {
            // Ignore
        }
        return height > 0 ? height : super.getHeight();
    }

    @Override
    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        return new FilterInputStream(in) {
            @Override
            public int read() throws IOException {
                int c = super.read();
                if (c == DELETE && deleteSendsBackspace) {
                    c = BACKSPACE;
                } else if (c == BACKSPACE && backspaceSendsDelete) {
                    return DELETE;
                }
                return c;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return 0;
                }

                int c = read();
                if (c == -1) {
                    return -1;
                }
                b[off] = (byte)c;

                int i = 1;
                try {
                    for (; i < len ; i++) {
                    c = read();
                    if (c == -1) {
                        break;
                    }
                    b[off + i] = (byte)c;
                    }
                } catch (IOException ee) {
                }
                return i;
            }
        };
    }

}
