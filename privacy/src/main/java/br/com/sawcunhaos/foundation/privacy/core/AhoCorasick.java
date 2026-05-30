
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.privacy.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Immutable Aho-Corasick automaton for multi-literal matching in a single O(n) pass.
 *
 * <p>Used for the literal {@code log-patterns} so that N needles are found in one scan of the message
 * instead of N independent {@code indexOf} sweeps. The automaton is built once at engine build time and
 * shared across threads (it is read-only after construction).</p>
 */
final class AhoCorasick {

    /** A match: the needle index plus the end position (exclusive) in the haystack. */
    record Match(int needleIndex, int end, int length) {
    }

    private final int[] gotoDefaultMiss;
    private final List<Map<Character, Integer>> next = new ArrayList<>();
    private final List<Integer> fail = new ArrayList<>();
    private final List<List<Integer>> output = new ArrayList<>();
    private final String[] needles;

    private AhoCorasick(final String[] needles) {
        this.needles = needles;
        this.gotoDefaultMiss = new int[0];
        newNode(); // root = 0
        for (int i = 0; i < needles.length; i++) {
            insert(needles[i], i);
        }
        buildFailureLinks();
    }

    /**
     * Builds an automaton, or returns {@code null} when there are no needles (so the engine can skip
     * the literal stage entirely).
     *
     * @param needles the literal strings to match
     * @return an automaton, or {@code null} when {@code needles} is empty
     */
    static AhoCorasick build(final List<String> needles) {
        if (needles == null || needles.isEmpty()) {
            return null;
        }
        return new AhoCorasick(needles.toArray(new String[0]));
    }

    private int newNode() {
        next.add(new HashMap<>());
        fail.add(0);
        output.add(new ArrayList<>());
        return next.size() - 1;
    }

    private void insert(final String word, final int index) {
        int node = 0;
        for (int i = 0; i < word.length(); i++) {
            final char c = word.charAt(i);
            final Integer child = next.get(node).get(c);
            if (child == null) {
                final int created = newNode();
                next.get(node).put(c, created);
                node = created;
            } else {
                node = child;
            }
        }
        output.get(node).add(index);
    }

    private void buildFailureLinks() {
        final Queue<Integer> queue = new ArrayDeque<>();
        for (final int child : next.get(0).values()) {
            fail.set(child, 0);
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            final int node = queue.poll();
            for (final Map.Entry<Character, Integer> e : next.get(node).entrySet()) {
                final char c = e.getKey();
                final int child = e.getValue();
                queue.add(child);
                int f = fail.get(node);
                while (f != 0 && !next.get(f).containsKey(c)) {
                    f = fail.get(f);
                }
                final Integer target = next.get(f).get(c);
                final int failState = (target != null && target != child) ? target : 0;
                fail.set(child, failState);
                output.get(child).addAll(output.get(failState));
            }
        }
    }

    /**
     * Scans the haystack and returns all matches in one pass.
     *
     * @param text the haystack
     * @return the matches found (possibly empty)
     */
    List<Match> search(final CharSequence text) {
        final List<Match> matches = new ArrayList<>();
        int node = 0;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            while (node != 0 && !next.get(node).containsKey(c)) {
                node = fail.get(node);
            }
            final Integer nxt = next.get(node).get(c);
            node = nxt != null ? nxt : 0;
            if (!output.get(node).isEmpty()) {
                for (final int needleIndex : output.get(node)) {
                    final int len = needles[needleIndex].length();
                    matches.add(new Match(needleIndex, i + 1, len));
                }
            }
        }
        return matches;
    }
}
