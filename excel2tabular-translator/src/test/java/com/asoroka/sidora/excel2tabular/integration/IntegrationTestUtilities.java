
package com.asoroka.sidora.excel2tabular.integration;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.totallylazy.Functions.returns;
import static com.googlecode.totallylazy.Predicates.always;
import static com.googlecode.totallylazy.Predicates.equalTo;
import static com.googlecode.totallylazy.Sequences.zip;
import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.ComparisonFailure;
import org.slf4j.Logger;

import com.google.common.io.Resources;
import com.googlecode.totallylazy.Block;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Predicate;
import com.googlecode.totallylazy.Rules;

/**
 * Some convenient utilities for integration tests.
 * 
 * @author ajs6f
 */
public class IntegrationTestUtilities {

    protected static List<String> readLines(final URL result) throws IOException {
        return Resources.readLines(result, UTF_8);
    }

    protected static final Pattern NO_TRAILING_COMMAS = compile("(.+?),*$");

    private static Block<Lines> doNothing = new Block<Lines>() {

        @Override
        protected void execute(final Lines ignored) {
            // do nothing
        }
    };

    private static Block<Lines> throwComparisonFailure(final int lineNum) {
        return new Block<Lines>() {

            @Override
            protected void execute(final Lines lines) {
                throw new ComparisonFailure("Mismatch at line: " + lineNum, lines.first(), lines.second());
            }
        };
    }

    private static Block<Lines> logMatch(final Logger log, final String message) {
        return new Block<Lines>() {

            @Override
            protected void execute(final Lines lines) {
                log.warn(new ComparisonFailure(message, lines.first(), lines.second()).getLocalizedMessage());
            }
        };
    }

    protected static String withoutTrailingCommas(final String input) {
        final Matcher matcher = NO_TRAILING_COMMAS.matcher(input);
        matcher.find();
        return matcher.group(1);
    }

    private static Predicate<Lines> matchesUpToTrailingCommas = new Predicate<Lines>() {

        @Override
        public boolean matches(final Lines lines) {
            return withoutTrailingCommas(lines.first())
                    .equals(withoutTrailingCommas(lines.second()));
        }
    };

    private static Predicate<Lines> matchesUpToQuotationCharacters = new Predicate<Lines>() {

        @Override
        public boolean matches(final Lines lines) {
            final String a = withoutTrailingCommas(lines.first()).replace("\"", "");
            final String b = withoutTrailingCommas(lines.second()).replace("\"", "");
            return a.equals(b);
        }
    };

    protected static void compareLines(final Iterable<String> checkLines, final Iterable<String> resultLines,
            final Logger log) {
        int lineNum = 0;
        for (final Pair<String, String> pair : zip(checkLines, resultLines)) {
            final Lines lines = new Lines(pair);
            lineNum++;
            Rules.<Lines, Void> rules()
                    .addLast(equalTo(), doNothing)
                    .addLast(matchesUpToTrailingCommas, logMatch(log, "Different trailing commas: line " + lineNum))
                    .addLast(matchesUpToQuotationCharacters, logMatch(log, "Different quote chars: line " + lineNum))
                    .addLast(always(), throwComparisonFailure(lineNum))
                    .apply(lines);
        }
    }

    private static class Lines extends Pair<String, String> {

        protected Lines(final Pair<String, String> lines) {
            super(returns(lines.first()), returns(lines.second()));
        }
    }
}
