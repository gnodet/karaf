/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
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

import org.aesh.readline.Buffer;
import org.aesh.readline.ConsoleBuffer;
import org.aesh.readline.InputProcessor;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.completion.SimpleCompletionHandler;
import org.aesh.utils.Config;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.Expander;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Levenshtein;
import org.jline.utils.WCWidth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.aesh.util.Parser.toCodePoints;
import static org.jline.reader.impl.ReaderUtils.getInt;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class CompletionHandlerImpl extends SimpleCompletionHandler {

    private final LineReaderImpl reader;
    private final Completer completer;

    private CompletionStatus status = CompletionStatus.COMPLETE;

    public CompletionHandlerImpl(LineReaderImpl reader, Completer completer) {
        this.reader = reader;
        this.completer = completer;
    }

    @Override
    public void addCompletion(Completion completion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCompletion(Completion completion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCompletions(List list) {
        if (list != null && !list.isEmpty()) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void complete(InputProcessor inputProcessor) {
        ConsoleBuffer buf = inputProcessor.buffer();
        Buffer buffer = buf.buffer();
        String str = buffer.asString();

        Expander expander = reader.getExpander();
        Parser parser = reader.getParser();
        History history = reader.getHistory();

        try {
            if (expander != null) {
                String exp = expander.expandHistory(history, str);
                if (!exp.equals(str)) {
                    buf.replace(exp);
                    return;
                }
            }
        } catch (IllegalArgumentException e) {
            return;
        }

        List<Candidate> candidates = new ArrayList<>();
        ParsedLine line;
        try {
            line = parser.parse(buffer.asString(), buffer.multiCursor(), Parser.ParseContext.COMPLETE);
            if (completer != null) {
                completer.complete(reader, line, candidates);
            }
        } catch (Exception e) {
            return;
        }

        if (expander != null) {
            String w = expander.expandVar(line.word());
            if (!line.word().equals(w)) {
                buf.moveCursor(line.word().length() - line.wordCursor());
                buf.delete(- line.word().length());
                buf.insert(toCodePoints(w));
                return;
            }
        }

        boolean caseInsensitive = reader.isSet(LineReader.Option.CASE_INSENSITIVE);
        int errors = getInt(reader, LineReader.ERRORS, 2);

        NavigableMap<String, List<Candidate>> sortedCandidates =
                new TreeMap<>(caseInsensitive ? String.CASE_INSENSITIVE_ORDER : null);
        for (Candidate cand : candidates) {
            sortedCandidates
                    .computeIfAbsent(AttributedString.fromAnsi(cand.value()).toString(), s -> new ArrayList<>())
                    .add(cand);
        }

        // Find matchers
        List<Function<Map<String, List<Candidate>>,
                      Map<String, List<Candidate>>>> matchers;
        Predicate<String> exact;

        String wd = line.word();
        matchers = Arrays.asList(
                simpleMatcher(s -> s.startsWith(wd)),
                simpleMatcher(s -> s.contains(wd)),
                typoMatcher(wd, errors)
        );
        exact = s -> s.equals(wd);

        // Find matching candidates
        Map<String, List<Candidate>> matching = Collections.emptyMap();
        for (Function<Map<String, List<Candidate>>,
                Map<String, List<Candidate>>> matcher : matchers) {
            matching = matcher.apply(sortedCandidates);
            if (!matching.isEmpty()) {
                break;
            }
        }

        // If we have no matches, bail out
        if (matching.isEmpty()) {
            return;
        }

        // Check if there's a single possible match
        Candidate completion = null;
        // If there's a single possible completion
        if (matching.size() == 1) {
            completion = matching.values().stream().flatMap(Collection::stream)
                    .findFirst().orElse(null);
        }
        // Or if RECOGNIZE_EXACT is set, try to find an exact match
        else if (reader.isSet(LineReader.Option.RECOGNIZE_EXACT)) {
            completion = matching.values().stream().flatMap(Collection::stream)
                    .filter(Candidate::complete)
                    .filter(c -> exact.test(c.value()))
                    .findFirst().orElse(null);
        }

        // Complete and exit
        if (completion != null && !completion.value().isEmpty()) {
            if (completion.value().startsWith(line.word())) {
                // TODO: bug when passing an empty array, so extra check
                if (!completion.value().equals(line.word())) {
                    buf.insert(toCodePoints(completion.value().substring(line.word().length())));
                }
            } else {
                buf.moveCursor(line.word().length() - line.wordCursor());
                buf.delete(- line.word().length());
                buf.insert(toCodePoints(completion.value()));
            }
            if (completion.complete()) {
                if (buffer.cursor() == buffer.length() || buffer.get(buffer.cursor()) != ' ') {
                    buf.insert(toCodePoints(" "));
                } else {
                    buf.moveCursor(1);
                }
            }
            return;
        }

        // Find current word and move to end
        String current = line.word();
        buf.moveCursor(current.length() - line.wordCursor());

        // Now, we need to find the unambiguous completion
        String commonPrefix = null;
        for (String key : matching.keySet()) {
            commonPrefix = commonPrefix == null ? key : getCommonStart(commonPrefix, key, caseInsensitive);
        }
        boolean hasUnambiguous = commonPrefix.startsWith(current) && !commonPrefix.equals(current);
        if (hasUnambiguous) {
            buf.insert(toCodePoints(commonPrefix.substring(current.length())));
            return;
        }

        List<Candidate> possible = matching.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());

        // If we list only and if there's a big
        // number of items, we should ask the user
        // for confirmation, display the list
        // and redraw the line at the bottom
        String completed = line.word();
        mergeCandidates(possible);
        Size size = reader.getTerminal().getSize();
        PostResult postResult = computePost(possible, null, null, completed);
        int lines = postResult.lines;
        int listMax = getInt(reader, LineReader.LIST_MAX, 100);
        if (listMax > 0 && possible.size() >= listMax || lines >= size.getRows() - 1) {
            if (status == CompletionStatus.COMPLETE) {
                status = CompletionStatus.ASKING_FOR_COMPLETIONS;
                buf.writeOut(Config.CR);
                buf.writeOut("Display all " + possible.size() + " possibilities? (" + lines + " lines)?");
                return;
            }
        }
        status = CompletionStatus.COMPLETE;
        buf.writeOut(Config.CR);
        buf.writeOut(postResult.post.toAnsi(reader.getTerminal()));
        buf.writeOut(Config.CR);
        buffer.setIsPromptDisplayed(false);
        buf.drawLine();
    }

    private static Function<Map<String, List<Candidate>>,
                            Map<String, List<Candidate>>> simpleMatcher(Predicate<String> pred) {
        return m -> m.entrySet().stream()
                .filter(e -> pred.test(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Function<Map<String, List<Candidate>>,
                            Map<String, List<Candidate>>> typoMatcher(String word, int errors) {
        return m -> {
            Map<String, List<Candidate>> map = m.entrySet().stream()
                    .filter(e -> distance(word, e.getKey()) < errors)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (map.size() > 1) {
                map.computeIfAbsent(word, w -> new ArrayList<>())
                        .add(new Candidate(word, word, "original", null, null, null, false));
            }
            return map;
        };
    }

    private static int distance(String word, String cand) {
        if (word.length() < cand.length()) {
            int d1 = Levenshtein.distance(word, cand.substring(0, Math.min(cand.length(), word.length())));
            int d2 = Levenshtein.distance(word, cand);
            return Math.min(d1, d2);
        } else {
            return Levenshtein.distance(word, cand);
        }
    }

    private static String getCommonStart(String str1, String str2, boolean caseInsensitive) {
        int[] s1 = str1.codePoints().toArray();
        int[] s2 = str2.codePoints().toArray();
        int len = 0;
        while (len < Math.min(s1.length, s2.length)) {
            int ch1 = s1[len];
            int ch2 = s2[len];
            if (ch1 != ch2 && caseInsensitive) {
                ch1 = Character.toUpperCase(ch1);
                ch2 = Character.toUpperCase(ch2);
                if (ch1 != ch2) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
            }
            if (ch1 != ch2) {
                break;
            }
            len++;
        }
        return new String(s1, 0, len);
    }

    private static void mergeCandidates(List<Candidate> possible) {
        // Merge candidates if the have the same key
        Map<String, List<Candidate>> keyedCandidates = new HashMap<>();
        for (Candidate candidate : possible) {
            if (candidate.key() != null) {
                List<Candidate> cands = keyedCandidates.computeIfAbsent(candidate.key(), s -> new ArrayList<>());
                cands.add(candidate);
            }
        }
        if (!keyedCandidates.isEmpty()) {
            for (List<Candidate> candidates : keyedCandidates.values()) {
                if (candidates.size() >= 1) {
                    possible.removeAll(candidates);
                    // Candidates with the same key are supposed to have
                    // the same description
                    candidates.sort(Comparator.comparing(Candidate::value));
                    Candidate first = candidates.get(0);
                    String disp = candidates.stream()
                            .map(Candidate::displ)
                            .collect(Collectors.joining(" "));
                    possible.add(new Candidate(first.value(), disp, first.group(),
                            first.descr(), first.suffix(), null, first.complete()));
                }
            }
        }
    }

    static class PostResult {
        final AttributedString post;
        final int lines;
        final int selectedLine;

        PostResult(AttributedString post, int lines, int selectedLine) {
            this.post = post;
            this.lines = lines;
            this.selectedLine = selectedLine;
        }
    }

    private static int wcwidth(String str) {
        return AttributedString.fromAnsi(str).columnLength();
    }

    private PostResult computePost(List<Candidate> possible, Candidate selection, List<Candidate> ordered, String completed) {
        List<Object> strings = new ArrayList<>();
        boolean groupName = reader.isSet(LineReader.Option.GROUP);
        if (groupName) {
            LinkedHashMap<String, TreeMap<String, Candidate>> sorted = new LinkedHashMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                sorted.computeIfAbsent(group != null ? group : "", s -> new TreeMap<>())
                        .put(cand.value(), cand);
            }
            for (Map.Entry<String, TreeMap<String, Candidate>> entry : sorted.entrySet()) {
                String group = entry.getKey();
                if (group.isEmpty() && sorted.size() > 1) {
                    group = "others";
                }
                if (!group.isEmpty()) {
                    strings.add(group);
                }
                strings.add(new ArrayList<>(entry.getValue().values()));
                if (ordered != null) {
                    ordered.addAll(entry.getValue().values());
                }
            }
        } else {
            Set<String> groups = new LinkedHashSet<>();
            TreeMap<String, Candidate> sorted = new TreeMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                if (group != null) {
                    groups.add(group);
                }
                sorted.put(cand.value(), cand);
            }
            for (String group : groups) {
                strings.add(group);
            }
            strings.add(new ArrayList<>(sorted.values()));
            if (ordered != null) {
                ordered.addAll(sorted.values());
            }
        }
        return toColumns(strings, selection, completed);
    }

    private static final String DESC_PREFIX = "(";
    private static final String DESC_SUFFIX = ")";
    private static final int MARGIN_BETWEEN_DISPLAY_AND_DESC = 1;
    private static final int MARGIN_BETWEEN_COLUMNS = 3;

    @SuppressWarnings("unchecked")
    private PostResult toColumns(List<Object> items, Candidate selection, String completed) {
        int[] out = new int[2];
        int width = reader.getTerminal().getSize().getColumns();
        // TODO: support Option.LIST_PACKED
        // Compute column width
        int maxWidth = 0;
        for (Object item : items) {
            if (item instanceof String) {
                int len = wcwidth((String) item);
                maxWidth = Math.max(maxWidth, len);
            }
            else if (item instanceof List) {
                for (Candidate cand : (List<Candidate>) item) {
                    int len = wcwidth(cand.displ());
                    if (cand.descr() != null) {
                        len += MARGIN_BETWEEN_DISPLAY_AND_DESC;
                        len += DESC_PREFIX.length();
                        len += wcwidth(cand.descr());
                        len += DESC_SUFFIX.length();
                    }
                    maxWidth = Math.max(maxWidth, len);
                }
            }
        }
        // Build columns
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (Object list : items) {
            toColumns(list, width, maxWidth, sb, selection, completed, out);
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return new PostResult(sb.toAttributedString(), out[0], out[1]);
    }

    @SuppressWarnings("unchecked")
    private void toColumns(Object items, int width, int maxWidth, AttributedStringBuilder sb, Candidate selection, String completed, int[] out) {
        if (maxWidth <= 0) {
            return;
        }
        // This is a group
        if (items instanceof String) {
            sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                    .append((String) items)
                    .style(AttributedStyle.DEFAULT)
                    .append("\n");
            out[0]++;
        }
        // This is a Candidate list
        else if (items instanceof List) {
            List<Candidate> candidates = (List<Candidate>) items;
            maxWidth = Math.min(width, maxWidth);
            int c = width / maxWidth;
            while (c > 1 && c * maxWidth + (c - 1) * MARGIN_BETWEEN_COLUMNS >= width) {
                c--;
            }
            int columns = c;
            int lines = (candidates.size() + columns - 1) / columns;
            IntBinaryOperator index;
            if (reader.isSet(LineReader.Option.LIST_ROWS_FIRST)) {
                index = (i, j) -> i * columns + j;
            } else {
                index = (i, j) -> j * lines + i;
            }
            for (int i = 0; i < lines; i++) {
                for (int j = 0; j < columns; j++) {
                    int idx = index.applyAsInt(i, j);
                    if (idx < candidates.size()) {
                        Candidate cand = candidates.get(idx);
                        boolean hasRightItem = j < columns - 1 && index.applyAsInt(i, j + 1) < candidates.size();
                        AttributedString left = AttributedString.fromAnsi(cand.displ());
                        AttributedString right = AttributedString.fromAnsi(cand.descr());
                        int lw = left.columnLength();
                        int rw = 0;
                        if (right != null) {
                            int rem = maxWidth - (lw + MARGIN_BETWEEN_DISPLAY_AND_DESC
                                    + DESC_PREFIX.length() + DESC_SUFFIX.length());
                            rw = right.columnLength();
                            if (rw > rem) {
                                right = AttributedStringBuilder.append(
                                        right.columnSubSequence(0, rem - WCWidth.wcwidth('…')),
                                        "…");
                                rw = right.columnLength();
                            }
                            right = AttributedStringBuilder.append(DESC_PREFIX, right, DESC_SUFFIX);
                            rw += DESC_PREFIX.length() + DESC_SUFFIX.length();
                        }
                        if (cand == selection) {
                            out[1] = i;
                            sb.style(AttributedStyle.INVERSE);
                            if (left.toString().startsWith(completed)) {
                                sb.append(left.toString(), 0, completed.length());
                                sb.append(left.toString(), completed.length(), left.length());
                            } else {
                                sb.append(left.toString());
                            }
                            for (int k = 0; k < maxWidth - lw - rw; k++) {
                                sb.append(' ');
                            }
                            if (right != null) {
                                sb.append(right);
                            }
                            sb.style(AttributedStyle.DEFAULT);
                        } else {
                            if (left.toString().startsWith(completed)) {
                                sb.style(sb.style().foreground(AttributedStyle.CYAN));
                                sb.append(left, 0, completed.length());
                                sb.style(AttributedStyle.DEFAULT);
                                sb.append(left, completed.length(), left.length());
                            } else {
                                sb.append(left);
                            }
                            if (right != null || hasRightItem) {
                                for (int k = 0; k < maxWidth - lw - rw; k++) {
                                    sb.append(' ');
                                }
                            }
                            if (right != null) {
                                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK + AttributedStyle.BRIGHT));
                                sb.append(right);
                                sb.style(AttributedStyle.DEFAULT);
                            }
                        }
                        if (hasRightItem) {
                            for (int k = 0; k < MARGIN_BETWEEN_COLUMNS; k++) {
                                sb.append(' ');
                            }
                        }
                    }
                }
                sb.append('\n');
            }
            out[0] += lines;
        }
    }

}
