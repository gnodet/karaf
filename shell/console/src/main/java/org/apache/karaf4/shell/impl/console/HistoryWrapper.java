package org.apache.karaf4.shell.impl.console;

import org.apache.karaf4.shell.api.console.History;

public class HistoryWrapper implements History {

    private final jline.console.history.History history;

    public HistoryWrapper(jline.console.history.History history) {
        this.history = history;
    }

    @Override
    public void clear() {
        history.clear();
    }

    public int first() {
        return history.iterator().next().index() + 1;
    }

    public int last() {
        return first() + history.size() - 1;
    }

    @Override
    public CharSequence get(int index) {
        return history.get(index - 1);
    }
}
