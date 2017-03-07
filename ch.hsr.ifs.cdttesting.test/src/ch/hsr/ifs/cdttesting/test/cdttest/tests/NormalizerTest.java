package ch.hsr.ifs.cdttesting.test.cdttest.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;

/**
 * @author tstauber
 *
 *         Tests for RTS normalizer to strip formatting differences
 *
 */
public class NormalizerTest {

	@Test
	public void leadingLineBreaks() {
		//@formatter:off
		final String input = "\n\n\n#include <cstdint>";

		final String should = "#include <cstdint>";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void trailingLineBreaks() {
		//@formatter:off
		final String input = "#include <cstdint>\n\n\n";

		final String should = "#include <cstdint>";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void shortenMultipleSpaces() {
		//@formatter:off
		final String input = "int      foo   {42};";

		final String should = "int foo {42};";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void removeLeadingSpaces() {
		//@formatter:off
		final String input = "    int foo {42};";

		final String should = "int foo {42};";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void removeTrailingSpaces() {
		//@formatter:off
		final String input = "int foo {42};    ";

		final String should = "int foo {42};";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void replaceLineBreaks() {
		//@formatter:off
		final String input = "#include <cstdint>\n"+
							 "int main() {\r\n" +
							 "  int foo = 42;\n" +
							 "}";

		final String should = "#include <cstdint>↵"+
				 			  "int main() {↵" +
				 			  "int foo = 42;↵" +
				 			  "}";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

	@Test
	public void removeRTSComments() {
		//@formatter:off
		final String input = "#include <cstdint>\n" +
							 "/*TODO write more tests */";

		final String should = "#include <cstdint>";
		//@formatter:on

		assertEquals(should, CDTTestingTest.normalize(input));
	}

}
