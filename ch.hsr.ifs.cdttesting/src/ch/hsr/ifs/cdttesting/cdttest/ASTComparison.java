package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerList;

import ch.hsr.ifs.iltis.core.data.AbstractPair;
import ch.hsr.ifs.iltis.core.data.Wrapper;
import ch.hsr.ifs.iltis.core.functional.Functional;

public class ASTComparison {

	/**
	 * Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
	 * applied with the {@link IASTTranslationUnit} from the expected code. To use
	 * this method the flag {@code instantiateExpectedProject} has to be set to
	 * true.
	 *
	 * @author tstauber
	 *
	 */
	public static void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit currentAST,
			final boolean failOnProblemNode) {

		ComparisonResult result = equalsIncludes(expectedAST.getIncludeDirectives(), currentAST.getIncludeDirectives(),
				false);

		switch (result.state) {
		case EQUAL:
			assertTrue(true);
			break;
		case ADDITIONAL_INCLUDE:
			assertEqualsWithAttributes("The current AST does have includes in addition to the ones in expected AST.",
					result.attributes);
			break;
		case INCLUDE_ORDER:
			assertEqualsWithAttributes("The order of the includes are not matching.", result.attributes);
			break;
		case MISSING_INCLUDE:
			assertEqualsWithAttributes("The current AST misses some of the includes from the expected AST.",
					result.attributes);
			break;
		default:
			break;
		}

		result = equals(expectedAST, currentAST, failOnProblemNode);

		switch (result.state) {
		case EQUAL:
			assertTrue(true);
			break;
		case DIFFERENT_AMOUNT_OF_CHILDREN:
			assertEqualsWithAttributes("Different amount of children.", result.attributes);
			break;
		case DIFFERENT_TYPE:
			assertEqualsWithAttributes("Different type.", result.attributes);
			break;
		case DIFFERENT_SIGNATURE:
			assertEqualsWithAttributes("Different normalized signatures.", result.attributes);
			break;
		case PROBLEM_NODE:
			assertEqualsWithAttributes("Encountered a IASTProblem node.", result.attributes);
			break;
		default:
			break;
		}
	}

	protected static void assertEqualsWithAttributes(String msg, Map<ComparisonAttribute, String> attributes) {
		String lineNo = attributes.get(ComparisonAttribute.LINE_NO);
		String expected = attributes.get(ComparisonAttribute.EXPECTED);
		String actual = attributes.get(ComparisonAttribute.ACTUAL);
		assertEquals(msg + lineNo != null ? " On line no: " + lineNo : "" + " -> ", expected, actual);
	}

	public static ComparisonResult equalsIncludes(final IASTPreprocessorIncludeStatement[] expectedStmt,
			final IASTPreprocessorIncludeStatement[] actualStmt, final boolean ignoreOrdering) {
		StringBuffer expectedStr = new StringBuffer();
		StringBuffer actualStr = new StringBuffer();
		if (ignoreOrdering) {
			Set<String> actual = Arrays.stream(actualStmt).map((node) -> node.getRawSignature())
					.collect(Collectors.toSet());
			Set<String> expected = Arrays.stream(expectedStmt).map((node) -> node.getRawSignature())
					.collect(Collectors.toSet());
			if (actual.equals(expected)) {
				return new ComparisonResult(ComparisonState.EQUAL);
			}

			Set<String> onlyInActual = new HashSet<>(actual);
			onlyInActual.removeAll(expected);
			Set<String> onlyInExpected = new HashSet<>(expected);
			onlyInExpected.removeAll(actual);

			final Map<ComparisonAttribute, String> attributes = new HashMap<>();
			attributes.put(ComparisonAttribute.EXPECTED, onlyInExpected.stream().collect(Collectors.joining("\n")));
			attributes.put(ComparisonAttribute.ACTUAL, onlyInActual.stream().collect(Collectors.joining("\n")));
			if (!onlyInActual.isEmpty()) {
				return new ComparisonResult(ComparisonState.ADDITIONAL_INCLUDE, attributes);
			} else {
				return new ComparisonResult(ComparisonState.MISSING_INCLUDE, attributes);
			}

		} else {
			Wrapper<Integer> count = new Wrapper<>(0);
			Functional.zip(expectedStmt, actualStmt)
					.filter((pair) -> !AbstractPair.allElementEquals(pair, ASTComparison::equalsRaw))
					.forEachOrdered((pair) -> {
						count.wrapped++;
						expectedStr.append((pair.first() == null ? "---" : pair.first().toString()).concat("\n"));
						actualStr.append((pair.second() == null ? "---" : pair.second().toString()).concat("\n"));
					});
			if (count.wrapped == 0) {
				return new ComparisonResult(ComparisonState.EQUAL);
			}
			final Map<ComparisonAttribute, String> attributes = new HashMap<>();
			attributes.put(ComparisonAttribute.EXPECTED, expectedStr.toString());
			attributes.put(ComparisonAttribute.ACTUAL, actualStr.toString());
			return new ComparisonResult(ComparisonState.INCLUDE_ORDER, attributes);
		}
	}

	protected static boolean equalsRaw(IASTNode left, IASTNode right) {
		return left.getRawSignature().equals(right.getRawSignature());
	}

	public static ComparisonResult equals(final IASTNode expected, final IASTNode actual,
			final boolean failOnProblemNode) {
		final IASTNode[] lChilds = expected.getChildren();
		final IASTNode[] rChilds = actual.getChildren();
		final IASTFileLocation fileLocation = actual.getOriginalNode().getFileLocation();
		final String lineNo = fileLocation == null ? "?" : String.valueOf(fileLocation.getStartingLineNumber());
		final Map<ComparisonAttribute, String> attributes = new HashMap<>();
		attributes.put(ComparisonAttribute.EXPECTED, expected.getRawSignature());
		attributes.put(ComparisonAttribute.ACTUAL, actual.getRawSignature());
		attributes.put(ComparisonAttribute.LINE_NO, lineNo);

		if (lChilds.length != rChilds.length) {
			return new ComparisonResult(ComparisonState.DIFFERENT_AMOUNT_OF_CHILDREN, attributes);
		} else if (!expected.getClass().equals(actual.getClass())) {
			return new ComparisonResult(ComparisonState.DIFFERENT_TYPE, attributes);
		} else if (lChilds.length != 0) {
			for (int i = 0; i < lChilds.length; i++) {
				if (lChilds[i] instanceof IASTProblem || rChilds[i] instanceof IASTProblem) {
					return new ComparisonResult(ComparisonState.PROBLEM_NODE, attributes);
				}
				final ComparisonResult childResult = equals(lChilds[i], rChilds[i], failOnProblemNode);
				if (childResult.state != ComparisonState.EQUAL) {
					return childResult;
				}
			}
		} else if (expected instanceof ICPPASTCompoundStatement || expected instanceof ICPPASTInitializerList) {
			return new ComparisonResult(ComparisonState.EQUAL);
		} else if (!normalize(expected.getRawSignature()).equals(normalize(actual.getRawSignature()))) {
			return new ComparisonResult(ComparisonState.DIFFERENT_SIGNATURE, attributes);
		}
		return new ComparisonResult(ComparisonState.EQUAL);
	}

	/**
	 * Normalizes the passed {@link String} by removing all testeditor-comments,
	 * removing leading/trailing whitespace and line-breaks, replacing all remaining
	 * line-breaks by ↵ and reducing all groups of whitespace to a single space.
	 *
	 * @author tstauber
	 *
	 * @param in
	 *            The {@link String} that should be normalized.
	 *
	 * @return A normalized copy of the parameter in.
	 **/
	public static String normalize(final String in) {
		// @formatter:off
		return in.replaceAll("/\\*.*\\*/", "") // Remove all test-editor-comments
				.replaceAll("(^((\\r?\\n)|\\s)*|((\\r?\\n)|\\s)*$)", "") // Remove all leading and trailing
																			// linebreaks/whitespace
				.replaceAll("\\s*(\\r?\\n)+\\s*", "↵") // Replace all linebreaks with linebreak-symbol
				.replaceAll("\\s+", " "); // Reduce all groups of whitespace to a single space
		// @formatter:on
	}

	public enum ComparisonState {
		DIFFERENT_TYPE, DIFFERENT_AMOUNT_OF_CHILDREN, DIFFERENT_SIGNATURE, EQUAL, PROBLEM_NODE, ADDITIONAL_INCLUDE, MISSING_INCLUDE, INCLUDE_ORDER
	}

	public enum ComparisonAttribute {
		EXPECTED, ACTUAL, LINE_NO
	}

	public static class ComparisonResult {
		public ComparisonState state;
		public Map<ComparisonAttribute, String> attributes;

		public ComparisonResult(final ComparisonState state, final Map<ComparisonAttribute, String> attributes) {
			this.state = state;
			this.attributes = attributes;
		}

		public ComparisonResult(final ComparisonState state) {
			this.state = state;
			this.attributes = new HashMap<>();
		}
	}
}
