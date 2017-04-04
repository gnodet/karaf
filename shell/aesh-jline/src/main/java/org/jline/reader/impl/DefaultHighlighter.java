/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

public class DefaultHighlighter implements Highlighter {

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        return AttributedString.fromAnsi(buffer);
    }

}
