
package com.asoroka.sidora.excel2tabular.integration;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.io.Resources;

/**
 * Some convenient utilities for tests.
 * 
 * @author ajs6f
 */
public class TestUtils {

    /**
     * A cheap and cheerful 2-tuple.
     * 
     * @author ajs6f
     * @param <A>
     * @param <B>
     */
    protected static class Pair<A, B> {

        public A a;

        public B b;

        private Pair(final A a2, final B b2) {
            this.a = a2;
            this.b = b2;
        }

        public static <A, B> Pair<A, B> of(final A a, final B b) {
            return new Pair<>(a, b);
        }
    }

    /**
     * Zips two lists together into a list of {@link Pair}s that is as long as the shorter input.
     * 
     * @param listA
     * @param listB
     * @return a zipped list
     */
    protected static <A, B> List<Pair<A, B>> zip(final List<A> listA, final List<B> listB) {
        if (listA.size() != listB.size()) {
            throw new AssertionError("Results size doesn't match expected size! Got " + listA.size() +
                    " to match against " + listB.size());
        }
        final List<Pair<A, B>> zipped = new LinkedList<>();
        for (int i = 0; i < listA.size(); i++) {
            zipped.add(Pair.of(listA.get(i), listB.get(i)));
        }
        return zipped;
    }

    protected static List<String> readLines(final URL result) throws IOException {
        return Resources.readLines(result, UTF_8);
    }

    private static final Pattern NO_TRAILING_COMMAS = compile("(.+?),*$");

    protected static void
            compareLines(final List<String> checkLines, final List<String> resultLines, final Logger log) {
        int lineNum = 0;
        for (final Pair<String, String> line : zip(checkLines, resultLines)) {
            lineNum++;
            try {
                assertEquals("Got bad line in results!", line.a, line.b);
            } catch (final AssertionError e) {
                final Matcher lineAmatcher = NO_TRAILING_COMMAS.matcher(line.a);
                String lineAwithoutTrailingCommas = null;
                if (lineAmatcher.find()) {
                    lineAwithoutTrailingCommas = lineAmatcher.group(1);
                } else {
                    log.error("Mismatch for expected value at line {}!", lineNum);
                    throw e;
                }
                final Matcher lineBmatcher = NO_TRAILING_COMMAS.matcher(line.b);
                String lineBwithoutTrailingCommas = null;
                if (lineBmatcher.find()) {
                    lineBwithoutTrailingCommas = lineBmatcher.group(1);
                } else {
                    log.error("Mismatch for actual value at line {}!", lineNum);
                    throw e;
                }
                final boolean differByTrailingCommas = lineAwithoutTrailingCommas.equals(lineBwithoutTrailingCommas);
                if (differByTrailingCommas) {
                    log.warn("Found difference by trailing commas at line number {}", lineNum);
                    log.warn("Expected: {}", line.a);
                    log.warn("Actual: {}", line.b);
                }
                else {
                    log.error("Error matching at line: {}", lineNum);
                    log.error(e.getLocalizedMessage());
                    throw e;
                }
            }
        }
    }
}
