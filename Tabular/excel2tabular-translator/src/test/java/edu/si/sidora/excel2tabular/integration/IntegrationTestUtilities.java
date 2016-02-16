
package edu.si.sidora.excel2tabular.integration;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.totallylazy.Functions.returns;
import static com.googlecode.totallylazy.Predicates.equalTo;
import static com.googlecode.totallylazy.Sequences.zip;
import static com.googlecode.totallylazy.matchers.Matchers.matcher;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ComparisonFailure;

import com.google.common.io.Resources;
import com.googlecode.totallylazy.Pair;

/**
 * Some convenient utilities for integration tests.
 * 
 * @author ajs6f
 */
public class IntegrationTestUtilities {

    protected static List<String> readLines(final URL result) throws IOException {
        return Resources.readLines(result, UTF_8);
    }

    private static final Pattern REMOVE_TRAILING_COMMAS = compile("(.+?),*$");

    protected static String withoutTrailingCommas(final String input) {
        final java.util.regex.Matcher matcher = REMOVE_TRAILING_COMMAS.matcher(input);
        matcher.find();
        return matcher.group(1);
    }

    private static Matcher<LinePair> matchesUpToTrailingCommas = new TypeSafeMatcher<LinePair>() {

        @Override
        public boolean matchesSafely(final LinePair lines) {
            return withoutTrailingCommas(lines.first())
                    .equals(withoutTrailingCommas(lines.second()));
        }

        @Override
        public void describeTo(final Description d) {
            d.appendText("are equal modulo trailing commas");
        }
    };

    private static Matcher<LinePair> matchesUpToQuotationCharacters = new TypeSafeMatcher<LinePair>() {

        @Override
        public boolean matchesSafely(final LinePair lines) {
            final String a = withoutTrailingCommas(lines.first()).replace("\"", "");
            final String b = withoutTrailingCommas(lines.second()).replace("\"", "");
            return a.equals(b);
        }

        @Override
        public void describeTo(final Description d) {
            d.appendText("are equal modulo trailing commas and quotation chars");
        }
    };

    @SuppressWarnings("unchecked")
    protected static void compareLines(final Iterable<String> checkLines, final Iterable<String> resultLines) {
        int lineNum = 0;
        for (final Pair<String, String> pair : zip(checkLines, resultLines)) {
            final LinePair lines = new LinePair(pair);
            lineNum++;
            if (not(anyOf(
                    matcher(equalTo()),
                    matchesUpToTrailingCommas,
                    matchesUpToQuotationCharacters))
                    .matches(lines)) {
                throw new ComparisonFailure("Mismatch at line: " + lineNum, lines.first(), lines.second());
            }
        }
    }

    private static class LinePair extends Pair<String, String> {

        protected LinePair(final Pair<String, String> lines) {
            super(returns(lines.first()), returns(lines.second()));
        }
    }
}
