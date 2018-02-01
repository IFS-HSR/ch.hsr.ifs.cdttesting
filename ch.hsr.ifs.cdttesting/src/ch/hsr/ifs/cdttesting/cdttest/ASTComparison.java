package ch.hsr.ifs.cdttesting.cdttest;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerList;


public class ASTComparison {

	// TODO feature to enable failure if node of type CPPASTProblemId occurs

	public static Pair<ComparisonState, String[]> equals(final IASTNode expected, final IASTNode actual) {
		final IASTNode[] lChilds = expected.getChildren();
		final IASTNode[] rChilds = actual.getChildren();
		final IASTFileLocation fileLocation = actual.getOriginalNode().getFileLocation();
		final String lineNo = fileLocation == null ? "?" : String.valueOf(fileLocation.getStartingLineNumber());
		final String[] description = new String[] { expected.getRawSignature(), actual.getRawSignature(), lineNo };

		if (lChilds.length != rChilds.length) {
			return new Pair<>(ComparisonState.DIFFERENT_AMOUNT_OF_CHILDREN, description);
		} else if (!expected.getClass().equals(actual.getClass())) {
			return new Pair<>(ComparisonState.DIFFERENT_TYPE, description);
		} else if (lChilds.length != 0) {
			for (int i = 0; i < lChilds.length; i++) {
				final Pair<ComparisonState, String[]> childResult = equals(lChilds[i], rChilds[i]);
				if (childResult.first != ComparisonState.EQUAL) {
					return childResult;
				}
			}
		} else if (expected instanceof ICPPASTCompoundStatement || expected instanceof ICPPASTInitializerList) {
			return new Pair<>(ComparisonState.EQUAL, null);
		} else if (!normalize(expected.getRawSignature()).equals(normalize(actual.getRawSignature()))) {
			return new Pair<>(ComparisonState.DIFFERENT_SIGNATURE, description);
		}
		return new Pair<>(ComparisonState.EQUAL, null);
	}

	public static enum ComparisonState {
		DIFFERENT_TYPE, DIFFERENT_AMOUNT_OF_CHILDREN, DIFFERENT_SIGNATURE, EQUAL
	}

	public static class Pair<T1, T2> {
		public T1 first;
		public T2 second;

		public Pair(final T1 first, final T2 second) {
			this.first = first;
			this.second = second;
		}
	}
	

	/**
	 * Normalizes the passed {@link String} by removing all testeditor-comments,
	 * removing leading/trailing whitespace and line-breaks, replacing all
	 * remaining line-breaks by ↵ and reducing all groups of whitespace to a
	 * single space.
	 *
	 * @author tstauber
	 *
	 * @param in
	 *            The {@link String} that should be normalized.
	 *
	 * @return A normalized copy of the parameter in.
	 **/
	public static String normalize(final String in) {
		//@formatter:off
		return in.replaceAll("/\\*.*\\*/", "")								//Remove all test-editor-comments
				.replaceAll("(^((\\r?\\n)|\\s)*|((\\r?\\n)|\\s)*$)", "")	//Remove all leading and trailing linebreaks/whitespace
				.replaceAll("\\s*(\\r?\\n)+\\s*", "↵")					//Replace all linebreaks with linebreak-symbol
				.replaceAll("\\s+", " ");									//Reduce all groups of whitespace to a single space
		//@formatter:on
	}
}
