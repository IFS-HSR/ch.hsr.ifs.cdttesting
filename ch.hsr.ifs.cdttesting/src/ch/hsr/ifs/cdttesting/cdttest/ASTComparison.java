package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerList;

import ch.hsr.ifs.cdttesting.cdttest.ASTComparison.ComparisonAttrID;
import ch.hsr.ifs.iltis.core.data.AbstractPair;
import ch.hsr.ifs.iltis.core.data.Wrapper;
import ch.hsr.ifs.iltis.core.functional.Functional;
import ch.hsr.ifs.iltis.core.functional.OptionalUtil;


/**
 * A utility class providing static methods to compare two IASTNodes. This can
 * be used to compare whole ASTs if the root nodes are passed.
 * 
 * @author tstauber
 *
 */
public class ASTComparison {

   /**
    * Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
    * applied with the {@link IASTTranslationUnit} from the expected code. To use
    * this method the flag {@code instantiateExpectedProject} has to be set to
    * true.
    * 
    * @param expectedAST
    *        The expected translation unit
    * @param actualAST
    *        The actual translation unit
    * @param failOnProblemNode
    *        When {@code true}, comparison fails on syntactically invalid code.
    *
    * @author tstauber
    *
    */
   public static void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit actualAST, final boolean failOnProblemNode) {

      ComparisonResult result = equalsIncludes(expectedAST.getIncludeDirectives(), actualAST.getIncludeDirectives(), false);
      assertEqualsWithAttributes(result);
      result = equals(expectedAST, actualAST, failOnProblemNode);
      assertEqualsWithAttributes(result);
   }

   protected static void assertEqualsWithAttributes(ComparisonResult result) {
      String lineNo = result.getAttributeString(ComparisonAttrID.LINE_NO);
      String expected = result.getAttributeString(ComparisonAttrID.EXPECTED);
      String actual = result.getAttributeString(ComparisonAttrID.ACTUAL);
      if (result.state == ComparisonState.EQUAL) {
         assertTrue(true);
      } else {
         assertEquals(result.getStateDecription() + lineNo, expected, actual);
      }
   }

   /**
    * Used to compare two arrays of includes. Optionally the order can be declared
    * relevant.
    * 
    * @param expectedStmt
    *        The expected include statements
    * @param actualStmt
    *        The actual include statements
    * @param ignoreOrdering
    *        When {@code true} the includes ordering will be ignored.
    * @return A {@link ComparisonResult} containing
    *         {@link ComparisonAttrID.EXPECTED} and
    *         {@link ComparisonAttrID.ACTUAL}
    */
   public static ComparisonResult equalsIncludes(final IASTPreprocessorIncludeStatement[] expectedStmt,
         final IASTPreprocessorIncludeStatement[] actualStmt, final boolean ignoreOrdering) {
      StringBuffer expectedStr = new StringBuffer();
      StringBuffer actualStr = new StringBuffer();
      if (ignoreOrdering) {
         Set<String> actual = Arrays.stream(actualStmt).map((node) -> node.getRawSignature()).collect(Collectors.toSet());
         Set<String> expected = Arrays.stream(expectedStmt).map((node) -> node.getRawSignature()).collect(Collectors.toSet());
         if (actual.equals(expected)) { return new ComparisonResult(ComparisonState.EQUAL); }

         Set<String> onlyInActual = new HashSet<>(actual);
         onlyInActual.removeAll(expected);
         Set<String> onlyInExpected = new HashSet<>(expected);
         onlyInExpected.removeAll(actual);

         ComparisonAttribute[] attributes = { new ComparisonAttribute(ComparisonAttrID.EXPECTED, onlyInExpected.stream().collect(Collectors.joining(
               "\n"))), new ComparisonAttribute(ComparisonAttrID.ACTUAL, onlyInActual.stream().collect(Collectors.joining("\n"))) };
         if (!onlyInActual.isEmpty()) {
            return new ComparisonResult(ComparisonState.ADDITIONAL_INCLUDE, attributes);
         } else {
            return new ComparisonResult(ComparisonState.MISSING_INCLUDE, attributes);
         }

      } else {
         Wrapper<Integer> count = new Wrapper<>(0);
         Functional.zip(expectedStmt, actualStmt).filter((pair) -> !AbstractPair.allElementEquals(pair, ASTComparison::equalsRaw)).forEachOrdered((
               pair) -> {
            count.wrapped++;
            expectedStr.append((pair.first() == null ? "---" : pair.first().toString()).concat("\n"));
            actualStr.append((pair.second() == null ? "---" : pair.second().toString()).concat("\n"));
         });
         if (count.wrapped == 0) { return new ComparisonResult(ComparisonState.EQUAL); }
         ComparisonAttribute[] attributes = { new ComparisonAttribute(ComparisonAttrID.EXPECTED, expectedStr.toString()), new ComparisonAttribute(
               ComparisonAttrID.ACTUAL, actualStr.toString()) };
         return new ComparisonResult(ComparisonState.INCLUDE_ORDER, attributes);
      }
   }

   protected static boolean equalsRaw(IASTNode left, IASTNode right) {
      return left.getRawSignature().equals(right.getRawSignature());
   }

   /**
    * Used to compare two {@link IASTNode}s. It can be configured to fail if an {@link IASTProblemNode} is encountered.
    * 
    * @param expected
    *        The first IASTNode
    * @param actual
    *        The second IASTNode
    * @param failOnProblemNode
    *        When {@code true}, comparison fails on syntactically invalid code.
    * @return
    */
   public static ComparisonResult equals(final IASTNode expected, final IASTNode actual, final boolean failOnProblemNode) {
      final IASTNode[] lChilds = expected.getChildren();
      final IASTNode[] rChilds = actual.getChildren();
      final IASTFileLocation fileLocation = actual.getOriginalNode().getFileLocation();
      final String lineNo = fileLocation == null ? "?" : String.valueOf(fileLocation.getStartingLineNumber());
      ComparisonAttribute[] attributes = { new ComparisonAttribute(ComparisonAttrID.EXPECTED, expected.getRawSignature()), new ComparisonAttribute(
            ComparisonAttrID.ACTUAL, actual.getRawSignature()), new ComparisonAttribute(ComparisonAttrID.LINE_NO, lineNo) };

      if (lChilds.length != rChilds.length) {
         return new ComparisonResult(ComparisonState.DIFFERENT_AMOUNT_OF_CHILDREN, attributes);
      } else if (!expected.getClass().equals(actual.getClass())) {
         return new ComparisonResult(ComparisonState.DIFFERENT_TYPE, attributes);
      } else if (lChilds.length != 0) {
         for (int i = 0; i < lChilds.length; i++) {
            if (lChilds[i] instanceof IASTProblem || rChilds[i] instanceof IASTProblem) { return new ComparisonResult(ComparisonState.PROBLEM_NODE,
                  attributes); }
            final ComparisonResult childResult = equals(lChilds[i], rChilds[i], failOnProblemNode);
            if (childResult.state != ComparisonState.EQUAL) { return childResult; }
         }
      } else if (expected instanceof ICPPASTCompoundStatement || expected instanceof ICPPASTInitializerList) {
         return new ComparisonResult(ComparisonState.EQUAL);
      } else if (!normalizeCPP(expected.getRawSignature()).equals(normalizeCPP(actual.getRawSignature()))) { return new ComparisonResult(
            ComparisonState.DIFFERENT_SIGNATURE, attributes); }
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
    *        The {@link String} that should be normalized.
    *
    * @return A normalized copy of the parameter in.
    **/
   public static String normalizeCPP(final String in) {
      // @formatter:off
		return in.replaceAll("/\\*.*\\*/", "") // Remove all test-editor-comments
				.replaceAll("(^((\\r?\\n)|\\s)*|((\\r?\\n)|\\s)*$)", "") // Remove all leading and trailing linebreaks/whitespace
				.replaceAll("\\s*(\\r?\\n)+\\s*", "↵") // Replace all linebreaks with linebreak-symbol
				.replaceAll("\\s+", " "); // Reduce all groups of whitespace to a single space
		// @formatter:on
   }

   protected enum ComparisonState {
      //@formatter:off
      DIFFERENT_TYPE("Different type."), DIFFERENT_AMOUNT_OF_CHILDREN("Different amount of children."),
      DIFFERENT_SIGNATURE("Different normalized signatures."), EQUAL(""),
      PROBLEM_NODE("Encountered a IASTProblem node."), ADDITIONAL_INCLUDE("Additional includes found."),
      MISSING_INCLUDE("Includes are missing."), INCLUDE_ORDER("The order of the includes differs.");
      //@formatter:on

      String desc;

      ComparisonState(String desc) {
         this.desc = desc;
      }
   }

   protected enum ComparisonAttrID {
      //@formatter:off
      EXPECTED(""), ACTUAL(""), LINE_NO(" On line no: ");
      //@formatter:on

      String prefix;

      ComparisonAttrID(String prefix) {
         this.prefix = prefix;
      }
   }

   /**
    * 
    * @author tstauber
    *
    */
   protected static class ComparisonAttribute {

      public ComparisonAttrID id;
      public String           value;

      public ComparisonAttribute(final ComparisonAttrID id, String value) {
         this.id = id;
         this.value = value;
      }

      public String toString() {
         return " " + id.prefix + " " + this.value;
      }
   }

   /**
    * A helper class holding a {@link ComparisonResult} and a map of
    * {@link ComparisonAttrID} -> {@link String}
    * 
    * @author tstauber
    *
    */
   protected static class ComparisonResult {

      public ComparisonState           state;
      public List<ComparisonAttribute> attributes;

      public ComparisonResult(final ComparisonState state, final ComparisonAttribute... attributes) {
         this.state = state;
         this.attributes = Arrays.asList(attributes);
      }

      public ComparisonResult(final ComparisonState state) {
         this.state = state;
         this.attributes = new ArrayList<>();
      }

      public String getAttributeString(ComparisonAttrID id) {
         return OptionalUtil.returnIfPresentElse(attributes.stream().filter((attr) -> attr.id == id).findFirst(), (o) -> o.toString(), "");
      }

      public String getStateDecription() {
         return state.desc;
      }
   }
}
