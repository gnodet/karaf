/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jline.reader.impl;

import org.aesh.readline.history.SearchDirection;
import org.jline.reader.History;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.aesh.util.Parser.arrayContains;
import static org.aesh.util.Parser.toCodePoints;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class HistoryWrapper extends org.aesh.readline.history.History {

    private final History history;
    private SearchDirection direction = SearchDirection.REVERSE;

    public HistoryWrapper(History history) {
        this.history = history;
    }

    @Override
    public void push(int[] entry) {

    }

    @Override
    public int[] find(int[] search) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int[] get(int index) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int size() {
        return history.size();
    }

    @Override
    public void setSearchDirection(SearchDirection direction) {
        this.direction = direction;
    }

    @Override
    public SearchDirection getSearchDirection() {
        return direction;
    }

    @Override
    public int[] getNextFetch() {
        if (history.next()) {
            return getCurrent();
        }
        return null;
    }

    @Override
    public int[] getPreviousFetch() {
        if (history.previous()) {
            return getCurrent();
        }
        return null;
    }

    @Override
    public int[] search(int[] search) {
        if(direction == SearchDirection.REVERSE) {
            int[] cur;
            while ((cur = getPreviousFetch()) != null) {
                if (arrayContains(cur, search)) {
                    return cur;
                }
            }
            return null;
        }
        else {
            int[] cur;
            while ((cur = getNextFetch()) != null) {
                if (arrayContains(cur, search)) {
                    return cur;
                }
            }
            return null;
        }
    }

    @Override
    public void setCurrent(int[] line) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int[] getCurrent() {
        return toCodePoints(history.current());
    }

    @Override
    public List<int[]> getAll() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void clear() {
        try {
            history.purge();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        try {
            history.save();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
