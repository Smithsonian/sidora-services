
package com.asoroka.sidora.excel2csv;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.readLines;
import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import com.asoroka.sidora.excel2csv.Excel2Csv;
import com.asoroka.sidora.excel2csv.Excel2CsvImpl;
import com.google.common.io.Resources;

public class Excel2CsvImplTest {

    private final Excel2Csv testExcel2Csv = new Excel2CsvImpl();

    final private static Logger log = getLogger(Excel2CsvImplTest.class);

    @Test
    public void testSimple() throws IOException {
        final URL inputUrl = new File("src/test/resources/xls/small-test.xls").toURI().toURL();
        final URL result = testExcel2Csv.apply(inputUrl).toURI().toURL();
        log.debug("Result:\n{}", Resources.toString(result, UTF_8));
        final URL checkFile = new File("src/test/resources/xls/small-test.csv").toURI().toURL();
        log.debug("Checkfile:\n{}", Resources.toString(checkFile, UTF_8));

        final List<String> resultLines = readLines(result, UTF_8);
        final List<String> checkLines = readLines(checkFile, UTF_8);
        for (final Pair<String, String> line : zip(checkLines, resultLines)) {
            assertEquals("Got bad line in results!", line.a, line.b);
        }
    }

    public static <A, B> List<Pair<A, B>> zip(final List<A> listA, final List<B> listB) {

        final List<Pair<A, B>> pairList = new LinkedList<>();
        final int smallerListSize = min(listA.size(), listB.size());
        for (int index = 0; index < smallerListSize; index++) {
            pairList.add(Pair.of(listA.get(index), listB.get(index)));
        }
        return pairList;
    }

    static class Pair<A, B> {

        public A a;

        public B b;

        public Pair(final A a2, final B b2) {
            this.a = a2;
            this.b = b2;
        }

        static <A, B> Pair<A, B> of(final A a, final B b) {
            return new Pair<>(a, b);
        }
    }
}
