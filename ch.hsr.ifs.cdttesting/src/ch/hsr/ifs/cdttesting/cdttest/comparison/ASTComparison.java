package ch.hsr.ifs.cdttesting.cdttest.comparison;

import static ch.hsr.ifs.iltis.core.functional.Functional.as;
import static ch.hsr.ifs.iltis.core.functional.Functional.moveToElseTo;
import static ch.hsr.ifs.iltis.core.functional.Functional.zip;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTAliasDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCastExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeleteExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExpressionList;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFieldReference;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerList;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLinkageSpecification;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPackExpandable;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPackExpansionExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDirective;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.core.runtime.Path;

import ch.hsr.ifs.iltis.core.collections.CollectionUtil;
import ch.hsr.ifs.iltis.core.data.AbstractPair;
import ch.hsr.ifs.iltis.core.functional.StreamFactory;
import ch.hsr.ifs.iltis.core.functional.StreamPair;
import ch.hsr.ifs.iltis.core.functional.functions.Consumer;
import ch.hsr.ifs.iltis.core.resources.WorkspaceUtil;


/**
 * A utility class providing static methods to compare two IASTNodes. This can
 * be used to compare whole ASTs if the root nodes are passed.
 * 
 * @author tstauber
 *
 */
public class ASTComparison {

   public static final String NODE_MISSING = "--NODE MISSING--";

   /**
    * Compares two {@link IASTTranslationUnit} and asserts the result to be {@link ComparisonState#EQUAL}. The comparison can be configured using
    * {@link ComparisonArg}.
    * 
    * @see ComparisonArg
    * 
    * @param expectedAST
    *        The expected AST
    * @param actualAST
    *        The actual AST
    * @param args
    *        The comparison arguments
    */
   public static void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit actualAST, final EnumSet<ComparisonArg> args) {
      equalsAST(expectedAST, actualAST, args).ifUnequal(r -> assertEquals(r.getDescriptionString(), r.getExpectedStrings(), r.getActualStrings()));
   }

   /**
    * Compares two AST and returns a {@link ComparisonResult}. The comparison can be configured using {@link ComparisonArg}.
    * 
    * @see ComparisonArg
    * 
    * @param expected
    *        The expected AST
    * @param actual
    *        The actual AST
    * @param args
    *        The arguments
    * @return The result of the comparison
    */
   public static ComparisonResult equalsAST(final IASTTranslationUnit expected, final IASTTranslationUnit actual, final EnumSet<ComparisonArg> args) {
      if (expected == null || actual == null) return new ComparisonResult(ComparisonState.AST_WAS_NULL);

      if (args.contains(ComparisonArg.COMPARE_INCLUDE_DIRECTIVES)) {
         final ComparisonResult result = equalsIncludes(expected, actual, args);
         if (result.isUnequal()) return addTuLevelComparisonAttributes(result, expected, actual, args);
      }
      final NodeComparisonVisitor comparisonVisitor = new NodeComparisonVisitor(expected, actual, args);
      return addTuLevelComparisonAttributes(comparisonVisitor.compare(), expected, actual, args);
   }

   private static ComparisonResult addTuLevelComparisonAttributes(final ComparisonResult result, final IASTTranslationUnit expected,
         final IASTTranslationUnit actual, final EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.PRINT_WHOLE_ASTS_ON_FAIL)) {
         result.attributes.add(new ComparisonAttribute(ComparisonAttrID.EXPECTED_CONTEXT, "TU", getTURawSignatureOrMissing(expected, args)));
         result.attributes.add(new ComparisonAttribute(ComparisonAttrID.ACTUAL_CONTEXT, "TU", getTURawSignatureOrMissing(actual, args)));
      }
      result.attributes.add(new ComparisonAttribute(ComparisonAttrID.TU_NAME, new Path(actual.getFilePath()).lastSegment()));
      return result;
   }

   /**
    * Used to compare two arrays of includes. If the arguments contain {@link ComparisonArg#IGNORE_INCLUDE_ORDER} the order of the includes will be
    * ignored.
    * 
    * @param expectedTu
    *        The expected AST
    * @param actualTu
    *        The actual AST
    * @param args
    *        The comparison arguments
    * @return A {@link ComparisonResult} containing
    *         {@link ComparisonAttrID.EXPECTED} and
    *         {@link ComparisonAttrID.ACTUAL}
    */
   public static ComparisonResult equalsIncludes(final IASTTranslationUnit expectedTu, final IASTTranslationUnit actualTu,
         final EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.IGNORE_INCLUDE_ORDER)) {
         return equalsIncludesUnordered(expectedTu, actualTu);
      } else {
         return equalsIncludesOrdered(expectedTu, actualTu);
      }
   }

   private static ComparisonResult equalsIncludesOrdered(final IASTTranslationUnit expected, final IASTTranslationUnit actual) {
      final String expectedStmt = collectToString(getFilteredIncludeStmts(expected));
      final String actualStmt = collectToString(getFilteredIncludeStmts(actual));

      final String lineNo = getLineNo(zip(getFilteredIncludeStmts(expected), getFilteredIncludeStmts(actual)).filter(
            AbstractPair::notAllElementEquals).map(StreamPair::first).findFirst());

      if (expectedStmt.equals(actualStmt)) {
         /* All includes are present */
         return new ComparisonResult(ComparisonState.EQUAL);
      } else {
         /* Include order differs */
         return new ComparisonResult(ComparisonState.INCLUDE_ORDER, generateBasicIncludeComparisonAttributes(lineNo, expectedStmt, actualStmt));
      }
   }

   private static ComparisonResult equalsIncludesUnordered(final IASTTranslationUnit expected, final IASTTranslationUnit actual) {
      final Set<IASTPreprocessorIncludeStatement> actualIncludes = getFilteredIncludeStmts(actual).collect(Collectors.toSet());
      final Set<IASTPreprocessorIncludeStatement> expectedIncludes = getFilteredIncludeStmts(expected).collect(Collectors.toSet());

      if (CollectionUtil.haveSameElements(actualIncludes, expectedIncludes, ASTComparison::equalsRaw)) { return new ComparisonResult(
            ComparisonState.EQUAL); }

      final Set<IASTPreprocessorIncludeStatement> onlyInActual = new HashSet<>();
      final Set<IASTPreprocessorIncludeStatement> onlyInExpected = new HashSet<>();

      moveToElseTo(StreamFactory.stream(actualIncludes.stream(), expectedIncludes.stream()), stmt -> CollectionUtil.firstMatch(expectedIncludes,
            ASTComparison::equalsRaw, stmt).isPresent(), onlyInActual, stmt -> CollectionUtil.firstMatch(actualIncludes, ASTComparison::equalsRaw,
                  stmt).isPresent(), onlyInExpected);

      final List<ComparisonAttribute> attributes = generateBasicIncludeComparisonAttributes(getLineNo(onlyInExpected, 0), collectToString(
            onlyInExpected.stream()), collectToString(onlyInActual.stream()));
      if (!onlyInActual.isEmpty()) {
         return new ComparisonResult(ComparisonState.ADDITIONAL_INCLUDE, attributes);
      } else {
         return new ComparisonResult(ComparisonState.MISSING_INCLUDE, attributes);
      }
   }

   private static String getLineNo(final Collection<IASTPreprocessorIncludeStatement> nodes, final int index) {
      return nodes.size() > index ? getLineNo(nodes.stream().skip(index - 1).findFirst()) : "?";
   }

   private static String getLineNo(final Optional<? extends IASTNode> node) {
      return getLineNo(node.orElse(null));
   }

   private static String getLineNo(final IASTNode node) {
      if (node == null || node.getFileLocation() == null) { return "?"; }
      return String.valueOf(node.getFileLocation().getStartingLineNumber());
   }

   private static String collectToString(final Stream<IASTPreprocessorIncludeStatement> nodes) {
      return nodes.map(ASTComparison::getRawSignatureOrMissing).collect(Collectors.joining("\n"));
   }

   private static List<ComparisonAttribute> generateBasicIncludeComparisonAttributes(final String firstMismatchLineNo, final String expected,
         final String actual) {
      return Arrays.asList(new ComparisonAttribute(ComparisonAttrID.LINE_NO, firstMismatchLineNo), new ComparisonAttribute(
            ComparisonAttrID.EXPECTED_INCLUDES, expected), new ComparisonAttribute(ComparisonAttrID.ACTUAL_INCLUDES, actual));
   }

   private static Stream<IASTPreprocessorIncludeStatement> getFilteredIncludeStmts(final IASTTranslationUnit ast) {
      return Stream.of(ast.getIncludeDirectives()).filter(IASTPreprocessorIncludeStatement::isPartOfTranslationUnitFile);
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
   public static ComparisonResult equals(final IASTNode expected, final IASTNode actual, final EnumSet<ComparisonArg> args) {
      if (expected == null && actual == null || expected instanceof EOTNode && actual instanceof EOTNode) {
         /* If both nodes are null, they are considered equal */
         return new ComparisonResult(ComparisonState.EQUAL);
      } else if (expected == null || expected instanceof EOTNode || actual == null || actual instanceof EOTNode) {
         /* One of the nodes does not have a counterpart */
         return new ComparisonResult(ComparisonState.NO_COUNTERPART, generateBasicComparisonResultAttributes(expected, actual, args));
      }

      if (!expected.getClass().equals(actual.getClass())) {
         /* Return if node types differ */
         return new ComparisonResult(ComparisonState.DIFFERENT_TYPE, generateBasicComparisonResultAttributes(expected, actual, args));
      }

      if ((expected instanceof IASTProblem || expected instanceof IASTProblemHolder || actual instanceof IASTProblem ||
           actual instanceof IASTProblemHolder) && args.contains(ComparisonArg.ABORT_ON_PROBLEM_NODE)) {
         /* Problem in AST occured */
         return new ComparisonResult(ComparisonState.PROBLEM_NODE, generateBasicComparisonResultAttributes(expected, actual, args));
      }

      final List<IASTNode> expectedChildren = getFilteredChildren(expected).collect(Collectors.toList());
      final List<IASTNode> actualChildren = getFilteredChildren(actual).collect(Collectors.toList());
      if (expectedChildren.size() != actualChildren.size()) {
         /* This captures different numbers of children here to provide more context */
         return new ComparisonResult(ComparisonState.DIFFERENT_NUMBER_OF_CHILDREN, generateBasicComparisonResultAttributes(expected, actual, args));
      }

      if (!typeSensitiveNodeEquals(expected, actual, args)) {
         /* Nodes do not seem to be equal */
         return new ComparisonResult(ComparisonState.DIFFERENT_SIGNATURE, generateBasicComparisonResultAttributes(expected, actual, args));
      }
      return new ComparisonResult(ComparisonState.EQUAL);
   }

   private static Stream<IASTNode> getFilteredChildren(final IASTNode node) {
      return Stream.of(node.getChildren()).filter(IASTNode::isPartOfTranslationUnitFile);
   }

   private static <T extends IASTNode> boolean typeSensitiveNodeEquals(final T expected, final T actual, final EnumSet<ComparisonArg> args) {
      if (!expected.getClass().equals(actual.getClass())) { return false; }

      if (expected instanceof IASTDeclaration) {
         /* Declaration */

         /* Terminal checks */
         if (expected instanceof ICPPASTFunctionDefinition) {
            final ICPPASTFunctionDefinition et = as(expected);
            final ICPPASTFunctionDefinition at = as(actual);
            return et.isDefaulted() == at.isDefaulted() && et.isDeleted() == at.isDeleted();
         } else if (expected instanceof ICPPASTTemplateDeclaration) {
            final ICPPASTTemplateDeclaration et = as(expected);
            final ICPPASTTemplateDeclaration at = as(actual);
            return et.isExported() == at.isExported();
         } else if (expected instanceof ICPPASTNamespaceDefinition) {
            final ICPPASTNamespaceDefinition et = as(expected);
            final ICPPASTNamespaceDefinition at = as(actual);
            return et.isInline() == at.isInline();
         } else if (expected instanceof ICPPASTVisibilityLabel) {
            final ICPPASTVisibilityLabel et = as(expected);
            final ICPPASTVisibilityLabel at = as(actual);
            return et.getVisibility() == at.getVisibility();
         } else if (expected instanceof ICPPASTLinkageSpecification) {
            final ICPPASTLinkageSpecification et = as(expected);
            final ICPPASTLinkageSpecification at = as(actual);
            return et.getLiteral().equals(at.getLiteral());
         } else if (expected instanceof ICPPASTUsingDeclaration) {
            final ICPPASTUsingDeclaration et = as(expected);
            final ICPPASTUsingDeclaration at = as(actual);
            return et.isTypename() == at.isTypename();
         } else if (expected instanceof IASTSimpleDeclaration || expected instanceof ICPPASTAliasDeclaration ||
                    expected instanceof ICPPASTUsingDirective) {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof ICPPASTTemplateParameter) {
         /* Template Parameter */

         final ICPPASTTemplateParameter e = as(expected);
         final ICPPASTTemplateParameter a = as(actual);
         if (e.isParameterPack() != a.isParameterPack()) { return false; }
         if (expected instanceof ICPPASTSimpleTypeTemplateParameter) {
            final ICPPASTSimpleTypeTemplateParameter et = as(expected);
            final ICPPASTSimpleTypeTemplateParameter at = as(actual);
            return et.getParameterType() == at.getParameterType();
         } else if (expected instanceof ICPPASTParameterDeclaration || expected instanceof ICPPASTTemplatedTypeTemplateParameter) {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         }

      } else if (expected instanceof ICPPASTDeclarator) {
         /* Declarator */

         /* Terminal checks */
         if (expected instanceof ICPPASTFunctionDeclarator) {
            final ICPPASTFunctionDeclarator e = as(expected);
            final ICPPASTFunctionDeclarator a = as(actual);
            return e.isConst() == a.isConst() && e.isFinal() == a.isFinal() && e.isPureVirtual() == a.isPureVirtual() && e.isVolatile() == a
                  .isVolatile();
         } else {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof ICPPASTDeclSpecifier) {
         /* DeclSpecifier */

         final ICPPASTDeclSpecifier e = as(expected);
         final ICPPASTDeclSpecifier a = as(actual);
         if (e.isConst() != a.isConst() || e.isVirtual() != a.isVirtual() || e.isVolatile() != a.isVolatile() || e.isConstexpr() != a.isConstexpr() ||
             e.isExplicit() != a.isExplicit() || e.isFriend() != a.isFriend() || e.isRestrict() != a.isRestrict() || e.isThreadLocal() != a
                   .isThreadLocal() || e.getStorageClass() != a.getStorageClass()) { return false; }

         /* Terminal checks */
         if (expected instanceof ICPPASTNamedTypeSpecifier) {
            final ICPPASTNamedTypeSpecifier et = as(expected);
            final ICPPASTNamedTypeSpecifier at = as(actual);
            return et.isTypename() == at.isTypename();
         } else if (expected instanceof ICPPASTCompositeTypeSpecifier) {
            final ICPPASTCompositeTypeSpecifier et = as(expected);
            final ICPPASTCompositeTypeSpecifier at = as(actual);
            return et.isFinal() == at.isFinal() && et.isVirtual() == at.isVirtual() && et.getKey() == at.getKey();
         } else if (expected instanceof ICPPASTElaboratedTypeSpecifier) {
            final ICPPASTElaboratedTypeSpecifier et = as(expected);
            final ICPPASTElaboratedTypeSpecifier at = as(actual);
            return et.getKind() == at.getKind();
         } else if (expected instanceof ICPPASTEnumerationSpecifier) {
            final ICPPASTEnumerationSpecifier et = as(expected);
            final ICPPASTEnumerationSpecifier at = as(actual);
            return et.isOpaque() == at.isOpaque() && et.isScoped() == at.isScoped();
         } else {
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof IASTExpression) {
         /* Expressions */

         if (expected instanceof ICPPASTUnaryExpression) {
            final ICPPASTUnaryExpression et = as(expected);
            final ICPPASTUnaryExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTBinaryExpression) {
            final ICPPASTBinaryExpression et = as(expected);
            final ICPPASTBinaryExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTLiteralExpression) {
            final ICPPASTLiteralExpression et = as(expected);
            final ICPPASTLiteralExpression at = as(actual);
            return et.getKind() == at.getKind() && equalsRaw(et, at, args);
         } else if (expected instanceof ICPPASTCastExpression) {
            final ICPPASTCastExpression et = as(expected);
            final ICPPASTCastExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTNewExpression) {
            final ICPPASTNewExpression et = as(expected);
            final ICPPASTNewExpression at = as(actual);
            return et.isArrayAllocation() == at.isArrayAllocation() && et.isGlobal() == at.isGlobal() && et.isNewTypeId() == at.isNewTypeId();
         } else if (expected instanceof ICPPASTDeleteExpression) {
            final ICPPASTDeleteExpression et = as(expected);
            final ICPPASTDeleteExpression at = as(actual);
            return et.isVectored() == at.isVectored() && et.isGlobal() == at.isGlobal();
         } else if (expected instanceof ICPPASTFieldReference) {
            final ICPPASTFieldReference et = as(expected);
            final ICPPASTFieldReference at = as(actual);
            return et.isPointerDereference() == at.isPointerDereference() && et.isTemplate() == at.isTemplate();
         } else if (expected instanceof ICPPASTLambdaExpression) {
            final ICPPASTLambdaExpression et = as(expected);
            final ICPPASTLambdaExpression at = as(actual);
            return et.getCaptureDefault() == at.getCaptureDefault();
         } else if (expected instanceof ICPPASTFunctionCallExpression || expected instanceof ICPPASTSimpleTypeConstructorExpression ||
                    expected instanceof ICPPASTLambdaExpression || expected instanceof ICPPASTPackExpansionExpression ||
                    expected instanceof IASTIdExpression || expected instanceof ICPPASTArraySubscriptExpression ||
                    expected instanceof ICPPASTExpressionList || expected instanceof IASTConditionalExpression) {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof ICPPASTReferenceOperator) {
         /* ReferenceOperator */

         final ICPPASTReferenceOperator et = as(expected);
         final ICPPASTReferenceOperator at = as(actual);
         return et.isRValueReference() == at.isRValueReference();
      } else if (expected instanceof IASTPointer) {
         /* Pointers */

         final IASTPointer et = as(expected);
         final IASTPointer at = as(actual);
         return et.isConst() == at.isConst() && et.isRestrict() == at.isRestrict() && et.isVolatile() == at.isVolatile();
      } else if (expected instanceof IASTStatement) {
         /* Statements */

         return defaultHandler(expected, actual, args);
      } else if (expected instanceof ICPPASTPackExpandable) {
         /* ICPPASTPackExpandable */

         final ICPPASTPackExpandable e = as(expected);
         final ICPPASTPackExpandable a = as(actual);
         if (e.isPackExpansion() != a.isPackExpansion()) { return false; }

         if (expected instanceof ICPPASTInitializerList) {
            final ICPPASTInitializerList et = as(expected);
            final ICPPASTInitializerList at = as(actual);
            return et.getSize() == at.getSize();
         } else if (expected instanceof ICPPASTBaseSpecifier) {
            final ICPPASTBaseSpecifier et = as(expected);
            final ICPPASTBaseSpecifier at = as(actual);
            return et.isVirtual() == at.isVirtual() && et.getVisibility() == at.getVisibility();
         } else if (expected instanceof ICPPASTCapture) {
            final ICPPASTCapture et = as(expected);
            final ICPPASTCapture at = as(actual);
            return et.capturesThisPointer() == at.capturesThisPointer() && et.isByReference() == at.isByReference();
         } else if (expected instanceof ICPPASTTypeId) {
            return true;
         } else {
            return defaultHandler(expected, actual, args);
         }

      } else if (expected instanceof ICPPASTName) {
         /* Names */

         final ICPPASTName e = as(expected);
         final ICPPASTName a = as(actual);
         if (e.isQualified() != a.isQualified()) { return false; }

         if (expected instanceof ICPPASTTemplateId) {
            final ICPPASTTemplateId et = as(expected);
            final ICPPASTTemplateId at = as(actual);
            return et.isDeclaration() == at.isDeclaration();
         } else if (expected instanceof ICPPASTQualifiedName) {
            final ICPPASTQualifiedName et = as(expected);
            final ICPPASTQualifiedName at = as(actual);
            return et.isFullyQualified() == at.isFullyQualified();
         } else if (expected instanceof ICPPASTTemplateId) {
            /* Relevant information is contained in the children */
            return defaultHandler(expected, actual, args);
         }
      } else {
         /* OTHER */
         if (expected instanceof IASTTranslationUnit || expected instanceof IASTArrayModifier || expected instanceof IASTInitializer) {
            /* Relevant information is contained in the children */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      }

      /* Default case */
      return equalsNormalizedRaw(expected, actual);
   }

   private static boolean defaultHandler(final IASTNode expected, final IASTNode actual, final EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.DEBUG_COMPARE_SIGNATURE_FOR_DEFAULT_HANDLER)) { return equalsRaw(expected, actual, args); }
      return true;
   }

   private static boolean equalsRaw(final IASTNode expected, final IASTNode actual, final EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.DEBUG_NO_NORMALIZING)) {
         return equalsRaw(expected, actual);
      } else {
         return equalsNormalizedRaw(expected, actual);
      }
   }

   protected static <T extends IASTNode> boolean equalsRaw(final T left, final T right) {
      return left.getRawSignature().equals(right.getRawSignature());
   }

   protected static <T extends IASTNode> boolean equalsNormalizedRaw(final T left, final T right) {
      return normalizeCPP(left.getRawSignature()).equals(normalizeCPP(right.getRawSignature()));
   }

   protected static String getSignature(final IASTNode node) {
      final String signature = node.getRawSignature();
      if (signature.length() == 0) {
         /* Provide signature of parent for reference */
         return "-- No signature. Providing parent-signature for context --\n" + getSignature(node.getParent());
      }
      return signature;
   }

   /**
    * Generates the basic comparison attributes (EXPECTED, ACTUAL, LINE_NO).
    * Not both of expected and actual should be null.
    */
   protected static <T extends IASTNode> List<ComparisonAttribute> generateBasicComparisonResultAttributes(final T expected, final T actual,
         final EnumSet<ComparisonArg> args) {
      final List<ComparisonAttribute> attributes = new ArrayList<>();

      attributes.add(new ComparisonAttribute(ComparisonAttrID.EXPECTED, getSimpleClassNameOrNULL(expected), getRawSignatureOrMissing(expected)));
      attributes.add(new ComparisonAttribute(ComparisonAttrID.ACTUAL, getSimpleClassNameOrNULL(actual), getRawSignatureOrMissing(actual)));

      final IASTFileLocation fileLocation = (expected != null ? expected : actual).getOriginalNode().getFileLocation();

      if (args.contains(ComparisonArg.PRINT_CONTEXT_ON_FAIL)) {
         attributes.add(new ComparisonAttribute(ComparisonAttrID.EXPECTED_CONTEXT, "some lines", getRawSignatureContextOrMissing(expected, args)));
         attributes.add(new ComparisonAttribute(ComparisonAttrID.ACTUAL_CONTEXT, "some lines", getRawSignatureContextOrMissing(actual, args)));
      }

      if (fileLocation != null) {
         attributes.add(new ComparisonAttribute(ComparisonAttrID.LINE_NO, String.valueOf(fileLocation.getStartingLineNumber())));
      }
      return attributes;
   }

   private static <T extends IASTNode> String getSimpleClassNameOrNULL(final T node) {
      if (node == null) { return "NULL"; }
      return node.getClass().getSimpleName();
   }

   protected static String getRawSignatureOrMissing(final IASTNode node) {
      return node == null ? NODE_MISSING : getSignature(node);
   }

   protected static String getRawSignatureContextOrMissing(final IASTNode node, final EnumSet<ComparisonArg> args) {
      if (node == null) { return NODE_MISSING; }
      IASTNode parent = node;
      IASTFileLocation loc = node.getFileLocation();
      if (loc == null) {
         node.isPartOfTranslationUnitFile();
         return getRawSignatureOrMissing(node);
      }
      while (loc.getNodeLength() < 60 && loc.getEndingLineNumber() - loc.getStartingLineNumber() < 3 && parent.getParent() != null && parent
            .getParent().getFileLocation() != null) {
         parent = parent.getParent();
         loc = parent.getFileLocation();
      }
      final String signature = parent.getRawSignature();;
      if (args.contains(ComparisonArg.DO_NOT_PRINT_RELATIVE_LINE_NOS)) {
         return signature;
      } else {
         final String[] lines = signature.split(WorkspaceUtil.getWorkspaceLineSeparator());
         final int beginningLineNo = loc.getStartingLineNumber();
         for (int i = 0; i < lines.length; i++) {
            lines[i] = String.format("%02d| ", beginningLineNo + i) + lines[i];
         }
         return String.join(WorkspaceUtil.getWorkspaceLineSeparator(), lines);
      }
   }

   protected static String getTURawSignatureOrMissing(final IASTNode node, final EnumSet<ComparisonArg> args) {
      if (node == null || node.getTranslationUnit() == null) {
         return NODE_MISSING;
      } else {
         final String signature = node.getTranslationUnit().getRawSignature();
         if (args.contains(ComparisonArg.DO_NOT_PRINT_RELATIVE_LINE_NOS)) {
            return signature;
         } else {
            final String[] lines = signature.split(WorkspaceUtil.getWorkspaceLineSeparator());
            for (int i = 0; i < lines.length; i++) {
               lines[i] = String.format("%02d| ", i + 1) + lines[i];
            }
            return String.join(WorkspaceUtil.getWorkspaceLineSeparator(), lines);
         }
      }
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
      DIFFERENT_SIGNATURE("Different raw signature."), NO_COUNTERPART("Node has no counterpart."), PROBLEM_NODE("Encountered a IASTProblem node."),
      MISSING_INCLUDE("Missing includes found."), INCLUDE_ORDER("Different include order."), ADDITIONAL_INCLUDE("Additional includes found."), EQUAL(""),
      INTERRUPTED("The comparison encountered an InterruptedException and is therefore invalid!"), AST_WAS_NULL("AST was null.");
      //@formatter:on

      String desc;

      ComparisonState(final String desc) {
         this.desc = desc;
      }
   }

   public enum ComparisonAttrID {
      EXPECTED(true, false, "\nNode "), ACTUAL(false, true, "\nNode "), EXPECTED_CONTEXT(true, false, "\nContext "), ACTUAL_CONTEXT(false, true,
            "\nContext "), EXPECTED_INCLUDES(true, false, "\nIncludes "), ACTUAL_INCLUDES(false, true, "\nIncludes "), LINE_NO(false, false,
                  " On line"), FIRST_MISMATCH(false, false, "First mismatch "), TU_NAME(false, false, " In file");

      String  prefix;
      boolean expected;
      boolean actual;

      ComparisonAttrID(final boolean expected, final boolean actual, final String prefix) {
         this.prefix = prefix;
         this.expected = expected;
         this.actual = actual;
      }

   }

   public static class ComparisonAttribute {

      public ComparisonAttrID id;
      public String           info;
      public String           value;

      public ComparisonAttribute(final ComparisonAttrID id, final String value) {
         this(id, "", value);
      }

      /**
       * Formatted in the style "{id.prefix} {info}:\n{value}"
       */
      public ComparisonAttribute(final ComparisonAttrID id, final String info, final String value) {
         this.id = id;
         this.info = info;
         this.value = value;
      }

      @Override
      public String toString() {
         return id.prefix + info + (isNotCompareAttr() ? ": " : ":\n") + value;
      }

      private boolean isNotCompareAttr() {
         return !id.actual && !id.expected;
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

      public ComparisonState                state;
      public ArrayList<ComparisonAttribute> attributes;

      public ComparisonResult(final ComparisonState state, final ComparisonAttribute... attributes) {
         this(state, Arrays.asList(attributes));
      }

      public ComparisonResult(final ComparisonState state, final List<ComparisonAttribute> attributes) {
         this.state = state;
         this.attributes = new ArrayList<>(attributes);
      }

      public ComparisonResult(final ComparisonState state) {
         this(state, new ArrayList<>());
      }

      public boolean isEqual() {
         return state == ComparisonState.EQUAL;
      }

      public boolean isUnequal() {
         return !isEqual();
      }

      /**
       * Return the attribute string for the passed {@code IComparisonAttrID}
       */
      public String getAttributeString(final ComparisonAttrID id) {
         return attributes.stream().filter((attr) -> attr.id == id).map(ComparisonAttribute::toString).collect(Collectors.joining(" "));
      }

      public String getDescriptionString() {
         return getStateDescription() + " " + attributes.stream().filter((attr) -> !attr.id.expected && !attr.id.actual).map(
               ComparisonAttribute::toString).collect(Collectors.joining(" ")) + "\n";
      }

      public String getExpectedStrings() {
         return attributes.stream().filter((attr) -> attr.id.expected).map(ComparisonAttribute::toString).collect(Collectors.joining(
               "\n-------------\n"));
      }

      public String getActualStrings() {
         return attributes.stream().filter((attr) -> attr.id.actual).map(ComparisonAttribute::toString).collect(Collectors.joining(
               "\n-------------\n"));
      }

      public String getStateDescription() {
         return state.desc;
      }

      public void ifEqual(final Consumer<ComparisonResult> fun) {
         if (isEqual()) {
            fun.accept(this);
         }
      }

      public void ifUnequal(final Consumer<ComparisonResult> fun) {
         if (isUnequal()) {
            fun.accept(this);
         }
      }
   }

   public enum ComparisonArg {
      // @formatter:off
      /**
       * Enables comparison of include directives
       */
      COMPARE_INCLUDE_DIRECTIVES, 
      /**
       * Enables comparison of comments
       */
      COMPARE_COMMENTS,
      /**
       * Aborts if a ProblemNode or ProblemHolder is encountered
       */
      ABORT_ON_PROBLEM_NODE,
      /**
       * If COMPARE_INCLUDE_DIRECTIVES is passed, this ignores the order of the includes
       */
      IGNORE_INCLUDE_ORDER,
      /**
       * Compare the source. If source comparison is enabled following arguments are ignored:
       * COMPARE_INCLUDE_DIRECTIVES, COMPARE_COMMENTS, ABORT_ON_PROBLEM_NODE, PRINT_WHOLE_ASTS_ON_FAIL,
       * PRINT_CONTEXT_ON_FAIL, DO_NOT_PRINT_RELATIVE_LINE_NOS, DEBUG_COMPARE_SIGNATURE_FOR_DEFAULT_HANDLER 
       */
      USE_SOURCE_COMPARISON,
      /**
       * Additionally to the nodes signature, this prints the raw signature of the full AST if the comparison failed.
       */
      PRINT_WHOLE_ASTS_ON_FAIL,
      /**
       * Additionally to the nodes signature, this prints around 3 lines of context if the comparison failed.
       */
      PRINT_CONTEXT_ON_FAIL,
      /**
       * This avoids printing relative line numbers, if PRINT_WHOLE_ASTS_ON_FAIL or PRINT_CONTEXT_ON_FAIL is used.
       */
      DO_NOT_PRINT_RELATIVE_LINE_NOS,
      /*
       * If USE_SOURCE_COMPARISON is used, this skips formatting before comparing.
       */
      DEBUG_NO_FORMATTING,
      /**
       * This skips normalizing before comparing.
       */
      DEBUG_NO_NORMALIZING,
      /**
       * If this is enabled, Nodes which are not specifically handled, will not be skipped, but will compare the raw signatures.
       */
      DEBUG_COMPARE_SIGNATURE_FOR_DEFAULT_HANDLER;      
      // @formatter:on

      public static EnumSet<ComparisonArg> emptySet() {
         return EnumSet.noneOf(ComparisonArg.class);
      }
   }

}
