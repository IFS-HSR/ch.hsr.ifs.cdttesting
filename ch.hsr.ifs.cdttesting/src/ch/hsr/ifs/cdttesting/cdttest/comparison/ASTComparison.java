package ch.hsr.ifs.cdttesting.cdttest.comparison;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerList;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTAttributeOwner;
import org.eclipse.cdt.internal.core.dom.rewrite.astwriter.ASTWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonAttrID;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;
import ch.hsr.ifs.iltis.core.collections.CollectionUtil;
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
@SuppressWarnings({ "unused", "restriction" })
public class ASTComparison {

   private static final String NODE_MISSING = "--NODE MISSING--";

   /**
    * Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
    * applied with the {@link IASTTranslationUnit} from the expected code. To use
    * this method the flag {@code instantiateExpectedProject} has to be set to
    * true.
    * 
    * TODO redo
    * @param expectedAST
    *        The expected translation unit
    * @param actualAST
    *        The actual translation unit
    * @param failOnProblemNode
    *        When {@code true}, comparison fails on syntactically invalid code.
    * @param ignoreIncludes
    *        When {@code true}, includes will not be compared.
    *
    * @author tstauber
    *
    */
   public static void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit actualAST, EnumSet<ComparisonArg> args) {
      ComparisonResult result = equalsAST(expectedAST, actualAST, args);
      String lineNo = result.getAttributeString(ComparisonAttrID.LINE_NO);
      String expected = result.getAttributeString(ComparisonAttrID.EXPECTED);
      String actual = result.getAttributeString(ComparisonAttrID.ACTUAL);
      
      switch (result.state) {
      case EQUAL:
         break;
      case DIFFERENT_NUMBER_OF_CHILDREN:
         String mismatch = result.getAttributeString(ComparisonAttrID.FIRST_MISMATCH);
         assertEquals(result.getStateDecription() + mismatch + lineNo, expected, actual);
         break;
      default:
         assertEquals(result.getStateDecription() + lineNo, expected, actual);
      }
   }
   
   /**
    * TODO
    * @param expected
    * @param actual
    * @param args
    * @return
    */
   public static ComparisonResult equalsAST(final IASTTranslationUnit expected, final IASTTranslationUnit actual, EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.COMPARE_INCLUDE_DIRECTIVES)) {
         ComparisonResult result = equalsIncludes(expected, actual, args.contains(ComparisonArg.IGNORE_INCLUDE_ORDER));
         if (result.isNotEqual()) return result;
      }

      NodeComparisonVisitor comparisonVisitor = new NodeComparisonVisitor(expected, actual, args);
      return comparisonVisitor.compare();
   }
   
   /**
    * TODO
    * @param expected
    * @param actual
    * @return
    */
   public static ComparisonResult equalsAST(final IASTTranslationUnit expected, final IASTTranslationUnit actual) {
      return equalsAST(expected, actual, EnumSet.noneOf(ComparisonArg.class));
   }

   /**
    * Used to compare two arrays of includes. Optionally the order can be declared
    * relevant.
    * TODO redo
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
   public static ComparisonResult equalsIncludes(final IASTTranslationUnit expectedTu, final IASTTranslationUnit actualTu,
         final boolean ignoreOrdering) {
      Stream<IASTPreprocessorIncludeStatement> expectedStmt = getFilteredIncludeStmts(expectedTu);
      Stream<IASTPreprocessorIncludeStatement> actualStmt = getFilteredIncludeStmts(actualTu);
      StringBuffer expectedStr = new StringBuffer();
      StringBuffer actualStr = new StringBuffer();
      if (ignoreOrdering) {
         Set<String> actual = actualStmt.map((node) -> node.getRawSignature()).collect(Collectors.toSet());
         Set<String> expected = expectedStmt.map((node) -> node.getRawSignature()).collect(Collectors.toSet());
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
            expectedStr.append((pair.first() == null ? NODE_MISSING : pair.first().toString()).concat("\n"));
            actualStr.append((pair.second() == null ? NODE_MISSING : pair.second().toString()).concat("\n"));
         });
         if (count.wrapped == 0) { return new ComparisonResult(ComparisonState.EQUAL); }
         ComparisonAttribute[] attributes = { new ComparisonAttribute(ComparisonAttrID.EXPECTED, expectedStr.toString()), new ComparisonAttribute(
               ComparisonAttrID.ACTUAL, actualStr.toString()) };
         return new ComparisonResult(ComparisonState.INCLUDE_ORDER, attributes);
      }
   }

   private static Stream<IASTPreprocessorIncludeStatement> getFilteredIncludeStmts(IASTTranslationUnit ast) {
      return Arrays.stream(ast.getIncludeDirectives()).filter(IASTPreprocessorIncludeStatement::isPartOfTranslationUnitFile);
   }

   protected static <T extends IASTNode> boolean equalsRaw(T left, T right) {
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
   public static ComparisonResult equals(final IASTNode expected, final IASTNode actual, EnumSet<ComparisonArg> args ) {
      if (expected == null && actual == null) {
         /* If both nodes are null, they are considered equal */
         return new ComparisonResult(ComparisonState.EQUAL);
      } else if (expected == null || actual == null) {
         /* One of the nodes does not have a counterpart */
         return new ComparisonResult(ComparisonState.NO_COUNTERPART, generateBasicComparisonResultAttributes(expected, actual));
      }

      if (!expected.getClass().equals(actual.getClass())) {
         /* Return if node types differ */
         return new ComparisonResult(ComparisonState.DIFFERENT_TYPE, generateBasicComparisonResultAttributes(expected, actual));
      }

      if ((expected instanceof IASTProblem || expected instanceof IASTProblemHolder || actual instanceof IASTProblem ||
           actual instanceof IASTProblemHolder) && !args.contains(ComparisonArg.CONTINUE_ON_PROBLEM_NODE)) {
         /* Problem in AST occured */
         return new ComparisonResult(ComparisonState.PROBLEM_NODE, generateBasicComparisonResultAttributes(expected, actual));
      }

      if (getFilteredChildren(expected).count() != getFilteredChildren(actual).count()) {
         /* This captures different numbers of children here to provide more context */
         return new ComparisonResult(ComparisonState.DIFFERENT_NUMBER_OF_CHILDREN, generateBasicComparisonResultAttributes(expected, actual));
      }

      if (!typeSensitiveNodeEquals(expected, actual)) {
         /* Nodes do not seem to be equal */
         return new ComparisonResult(ComparisonState.DIFFERENT_SIGNATURE, generateBasicComparisonResultAttributes(expected, actual));
      }
      return new ComparisonResult(ComparisonState.EQUAL);
   }

   private static Stream<IASTNode> getFilteredChildren(IASTNode node) {
      return Arrays.stream(node.getChildren()).filter(IASTNode::isPartOfTranslationUnitFile);
   }

   private static <T extends IASTNode> boolean typeSensitiveNodeEquals(T expected, T actual) {
      if (!expected.getClass().equals(actual.getClass())) { return false; }

      if (expected instanceof ICPPASTCompositeTypeSpecifier) {
         ICPPASTCompositeTypeSpecifier e = (ICPPASTCompositeTypeSpecifier) expected;
         ICPPASTCompositeTypeSpecifier a = (ICPPASTCompositeTypeSpecifier) actual;
         return equalsICPPASTDeclSpecifier(e, a) && e.isFinal() == a.isFinal() && e.isVirtual() == a.isVirtual() && e.getKey() == a.getKey();
      }
      if (expected instanceof ICPPASTFunctionDeclarator) {
         ICPPASTFunctionDeclarator e = (ICPPASTFunctionDeclarator) expected;
         ICPPASTFunctionDeclarator a = (ICPPASTFunctionDeclarator) actual;
         return e.isConst() == a.isConst() && e.isFinal() == a.isFinal() && e.isPureVirtual() == a.isPureVirtual() && e.isVolatile() == a
               .isVolatile();
      }
      if (expected instanceof ICPPASTFunctionDefinition) {
         ICPPASTFunctionDefinition e = (ICPPASTFunctionDefinition) expected;
         ICPPASTFunctionDefinition a = (ICPPASTFunctionDefinition) actual;
         return e.isDefaulted() == a.isDefaulted() && e.isDeleted() == a.isDeleted();
      }
      if (expected instanceof ICPPASTNamedTypeSpecifier) {
         ICPPASTNamedTypeSpecifier e = (ICPPASTNamedTypeSpecifier) expected;
         ICPPASTNamedTypeSpecifier a = (ICPPASTNamedTypeSpecifier) actual;
         return equalsICPPASTDeclSpecifier(e, a) && e.isTypename() == a.isTypename();
      }
      if (expected instanceof ICPPASTTemplateDeclaration) {
         ICPPASTTemplateDeclaration e = (ICPPASTTemplateDeclaration) expected;
         ICPPASTTemplateDeclaration a = (ICPPASTTemplateDeclaration) actual;
         return e.isExported() == a.isExported();
      }
      if (expected instanceof ICPPASTNamespaceDefinition) {
         ICPPASTNamespaceDefinition e = (ICPPASTNamespaceDefinition) expected;
         ICPPASTNamespaceDefinition a = (ICPPASTNamespaceDefinition) actual;
         return e.isInline() == a.isInline();
      }
      if (expected instanceof ICPPASTFunctionCallExpression) {
         ICPPASTFunctionCallExpression e = (ICPPASTFunctionCallExpression) expected;
         ICPPASTFunctionCallExpression a = (ICPPASTFunctionCallExpression) actual;
         return e.isLValue() == a.isLValue();
      }
      if (expected instanceof ICPPASTTypeId) {
         ICPPASTTypeId e = (ICPPASTTypeId) expected;
         ICPPASTTypeId a = (ICPPASTTypeId) actual;
         return e.isPackExpansion() == a.isPackExpansion();
      }
      if (expected instanceof ICPPASTUnaryExpression) {
         ICPPASTUnaryExpression e = (ICPPASTUnaryExpression) expected;
         ICPPASTUnaryExpression a = (ICPPASTUnaryExpression) actual;
         return e.isLValue() == a.isLValue() && e.getOperator() == a.getOperator() && e.getExpressionType().isSameType(a.getExpressionType());
      }

      if (expected instanceof IASTTranslationUnit || expected instanceof ICPPASTInitializerList || expected instanceof IASTSimpleDeclaration ||
          expected instanceof ICPPASTDeclarator || expected instanceof ICPPASTQualifiedName || expected instanceof IASTInitializer ||
          expected instanceof ICPPASTParameterDeclaration || expected instanceof IASTStatement || expected instanceof ICPPASTTemplateId ||
          expected instanceof ICPPASTSimpleTypeConstructorExpression) {
         /* Relevant information is contained in the children */
         /* Must be at the end, as some subtypes need special comparison */
         return true;
      } else {
         /* Default case */
         return equalsNormalizedRaw(expected, actual);
      }
   }

   private static boolean equalsICPPASTDeclSpecifier(ICPPASTDeclSpecifier e, ICPPASTDeclSpecifier a) {
      // @formatter:off
      return e.isConst() == a.isConst() && 
             e.isVirtual() == a.isVirtual() && 
             e.isVolatile() == a.isVolatile() && 
             e.isConstexpr() == a.isConstexpr() &&
             e.isExplicit() == a.isExplicit() &&
             e.isFriend() == a.isFriend() &&
             e.isRestrict() == a.isRestrict() && 
             e.isThreadLocal() == a.isThreadLocal();
      // @formatter:on
   }

   protected static <T extends IASTNode> boolean equalsNormalizedRaw(T left, T right) {
      return normalizeCPP(left.getRawSignature()).equals(normalizeCPP(right.getRawSignature()));
   }

   /**
    * Generates the basic comparison attributes (EXPECTED, ACTUAL, LINE_NO).
    * Not both of expected and actual should be null.
    */
   protected static <T extends IASTNode> List<ComparisonAttribute> generateBasicComparisonResultAttributes(final T expected, final T actual) {
      final IASTFileLocation fileLocation = (expected != null ? expected : actual).getOriginalNode().getFileLocation();
      final String lineNoText = fileLocation == null ? "?" : String.valueOf(fileLocation.getStartingLineNumber());
      final String expectedText = expected == null ? NODE_MISSING : normalizeCPP(expected.getRawSignature());
      final String actualText = actual == null ? NODE_MISSING : normalizeCPP(actual.getRawSignature());
      return Arrays.asList(new ComparisonAttribute(ComparisonAttrID.EXPECTED, expectedText), new ComparisonAttribute(ComparisonAttrID.ACTUAL,
            actualText), new ComparisonAttribute(ComparisonAttrID.LINE_NO, lineNoText));
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
		      .replaceAll("\t", "   ") // Replace all tabs with three spaces
		      .replaceAll("(^\\s*|\\s*$)", "") // Remove all leading and trailing linebreaks
		      .replaceAll("^\\s*$", "") // Remove empty lines
				.replaceAll("\\s*(\\r?\\n)+\\s*", "\n") // Replace all linebreaks with simple newline
				.replaceAll("\\s*\n*\\s*\\{", " {") // Move all opening curly braces on the line of their statement
				.replaceAll("([^:])\\s*:\\s*([^:])", "$1 : $2") // Surround : token with spaces
				.replaceAll(" +", " "); // Reduce all groups of whitespace to a single space
		// @formatter:on
   }

   public enum ComparisonState {
      //@formatter:off
      DIFFERENT_TYPE("Different type."), DIFFERENT_NUMBER_OF_CHILDREN("Different number of children."), DIFFERENT_COMMENT("Different comments."),
      DIFFERENT_SIGNATURE("Different normalized signatures."), NO_COUNTERPART("Node has no counterpart."), PROBLEM_NODE("Encountered a IASTProblem node."),
      MISSING_INCLUDE("Includes are missing."), INCLUDE_ORDER("The order of the includes differs."), ADDITIONAL_INCLUDE("Additional includes found."), EQUAL("");
      //@formatter:on

      String desc;

      ComparisonState(String desc) {
         this.desc = desc;
      }
   }

   protected enum ComparisonAttrID {
      //@formatter:off
      EXPECTED(""), ACTUAL(""), LINE_NO(" On line no: "), FIRST_MISMATCH("First mismatch: ");
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
   public static class ComparisonResult {

      public ComparisonState           state;
      public List<ComparisonAttribute> attributes;

      public ComparisonResult(final ComparisonState state, final ComparisonAttribute... attributes) {
         this.state = state;
         this.attributes = Arrays.asList(attributes);
      }

      public ComparisonResult(final ComparisonState state, final List<ComparisonAttribute> attributes) {
         this.state = state;
         this.attributes = attributes;
      }

      public ComparisonResult(final ComparisonState state) {
         this.state = state;
         this.attributes = new ArrayList<>();
      }

      public boolean isEqual() {
         return state == ComparisonState.EQUAL;
      }

      public boolean isNotEqual() {
         return !isEqual();
      }

      public String getAttributeString(ComparisonAttrID id) {
         return OptionalUtil.returnIfPresentElse(attributes.stream().filter((attr) -> attr.id == id).findFirst(), (o) -> o.toString(), "");
      }

      public String getStateDecription() {
         return state.desc;
      }
   }

   public enum ComparisonArg {
      //@formatter:off
      COMPARE_INCLUDE_DIRECTIVES(), COMPARE_COMMENTS(), CONTINUE_ON_PROBLEM_NODE(), IGNORE_INCLUDE_ORDER();
      //@formatter:on
      
      public static EnumSet<ComparisonArg> emptySet(){
         return EnumSet.noneOf(ComparisonArg.class);
      }
   }

}
