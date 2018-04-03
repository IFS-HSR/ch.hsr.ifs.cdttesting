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
   public static void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit actualAST, EnumSet<ComparisonArg> args) {
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
   public static ComparisonResult equalsAST(final IASTTranslationUnit expected, final IASTTranslationUnit actual, EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.COMPARE_INCLUDE_DIRECTIVES)) {
         ComparisonResult result = equalsIncludes(expected, actual, args);
         if (result.isUnequal()) return addTuLevelComparisonAttributes(result, expected, actual, args);
      }
      NodeComparisonVisitor comparisonVisitor = new NodeComparisonVisitor(expected, actual, args);
      return addTuLevelComparisonAttributes(comparisonVisitor.compare(), expected, actual, args);
   }

   private static ComparisonResult addTuLevelComparisonAttributes(ComparisonResult result, IASTTranslationUnit expected, IASTTranslationUnit actual,
         EnumSet<ComparisonArg> args) {
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
         EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.IGNORE_INCLUDE_ORDER)) {
         return equalsIncludesUnordered(expectedTu, actualTu);
      } else {
         return equalsIncludesOrdered(expectedTu, actualTu);
      }
   }

   private static ComparisonResult equalsIncludesOrdered(IASTTranslationUnit expected, IASTTranslationUnit actual) {
      String expectedStmt = collectToString(getFilteredIncludeStmts(expected));
      String actualStmt = collectToString(getFilteredIncludeStmts(actual));

      String lineNo = getLineNo(zip(getFilteredIncludeStmts(expected), getFilteredIncludeStmts(actual)).filter(AbstractPair::notAllElementEquals).map(
            StreamPair::first).findFirst());

      if (expectedStmt.equals(actualStmt)) {
         /* All includes are present */
         return new ComparisonResult(ComparisonState.EQUAL);
      } else {
         /* Include order differs */
         return new ComparisonResult(ComparisonState.INCLUDE_ORDER, generateBasicIncludeComparisonAttributes(lineNo, expectedStmt, actualStmt));
      }
   }

   private static ComparisonResult equalsIncludesUnordered(IASTTranslationUnit expected, IASTTranslationUnit actual) {
      Set<IASTPreprocessorIncludeStatement> actualIncludes = getFilteredIncludeStmts(actual).collect(Collectors.toSet());
      Set<IASTPreprocessorIncludeStatement> expectedIncludes = getFilteredIncludeStmts(expected).collect(Collectors.toSet());

      if (CollectionUtil.haveSameElements(actualIncludes, expectedIncludes, ASTComparison::equalsRaw)) return new ComparisonResult(
            ComparisonState.EQUAL);

      Set<IASTPreprocessorIncludeStatement> onlyInActual = new HashSet<>();
      Set<IASTPreprocessorIncludeStatement> onlyInExpected = new HashSet<>();

      moveToElseTo(StreamFactory.stream(actualIncludes.stream(), expectedIncludes.stream()), stmt -> CollectionUtil.firstMatch(expectedIncludes,
            ASTComparison::equalsRaw, stmt).isPresent(), onlyInActual, stmt -> CollectionUtil.firstMatch(actualIncludes, ASTComparison::equalsRaw,
                  stmt).isPresent(), onlyInExpected);

      List<ComparisonAttribute> attributes = generateBasicIncludeComparisonAttributes(getLineNo(onlyInExpected, 0), collectToString(onlyInExpected
            .stream()), collectToString(onlyInActual.stream()));
      if (!onlyInActual.isEmpty()) {
         return new ComparisonResult(ComparisonState.ADDITIONAL_INCLUDE, attributes);
      } else {
         return new ComparisonResult(ComparisonState.MISSING_INCLUDE, attributes);
      }
   }

   private static String getLineNo(Collection<IASTPreprocessorIncludeStatement> nodes, int index) {
      return nodes.size() > index ? getLineNo(nodes.stream().skip(index - 1).findFirst()) : "?";
   }

   private static String getLineNo(Optional<? extends IASTNode> node) {
      return getLineNo(node.orElse(null));
   }

   private static String getLineNo(IASTNode node) {
      if (node == null || node.getFileLocation() == null) return "?";
      return String.valueOf(node.getFileLocation().getStartingLineNumber());
   }

   private static String collectToString(Stream<IASTPreprocessorIncludeStatement> nodes) {
      return nodes.map(ASTComparison::getRawSignatureOrMissing).collect(Collectors.joining("\n"));
   }

   private static List<ComparisonAttribute> generateBasicIncludeComparisonAttributes(String firstMismatchLineNo, String expected, String actual) {
      return Arrays.asList(new ComparisonAttribute(ComparisonAttrID.LINE_NO, firstMismatchLineNo), new ComparisonAttribute(
            ComparisonAttrID.EXPECTED_INCLUDES, expected), new ComparisonAttribute(ComparisonAttrID.ACTUAL_INCLUDES, actual));
   }

   private static Stream<IASTPreprocessorIncludeStatement> getFilteredIncludeStmts(IASTTranslationUnit ast) {
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
   public static ComparisonResult equals(final IASTNode expected, final IASTNode actual, EnumSet<ComparisonArg> args) {
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

      List<IASTNode> expectedChildren = getFilteredChildren(expected).collect(Collectors.toList());
      List<IASTNode> actualChildren = getFilteredChildren(actual).collect(Collectors.toList());
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

   private static Stream<IASTNode> getFilteredChildren(IASTNode node) {
      return Stream.of(node.getChildren()).filter(IASTNode::isPartOfTranslationUnitFile);
   }

   private static <T extends IASTNode> boolean typeSensitiveNodeEquals(T expected, T actual, EnumSet<ComparisonArg> args) {
      if (!expected.getClass().equals(actual.getClass())) { return false; }

      if (expected instanceof IASTDeclaration) {
         /* Declaration */

         /* Terminal checks */
         if (expected instanceof ICPPASTFunctionDefinition) {
            ICPPASTFunctionDefinition et = as(expected);
            ICPPASTFunctionDefinition at = as(actual);
            return et.isDefaulted() == at.isDefaulted() && et.isDeleted() == at.isDeleted();
         } else if (expected instanceof ICPPASTTemplateDeclaration) {
            ICPPASTTemplateDeclaration et = as(expected);
            ICPPASTTemplateDeclaration at = as(actual);
            return et.isExported() == at.isExported();
         } else if (expected instanceof ICPPASTNamespaceDefinition) {
            ICPPASTNamespaceDefinition et = as(expected);
            ICPPASTNamespaceDefinition at = as(actual);
            return et.isInline() == at.isInline();
         } else if (expected instanceof ICPPASTVisibilityLabel) {
            ICPPASTVisibilityLabel et = as(expected);
            ICPPASTVisibilityLabel at = as(actual);
            return et.getVisibility() == at.getVisibility();
         } else if (expected instanceof ICPPASTLinkageSpecification) {
            ICPPASTLinkageSpecification et = as(expected);
            ICPPASTLinkageSpecification at = as(actual);
            return et.getLiteral().equals(at.getLiteral());
         } else if (expected instanceof ICPPASTUsingDeclaration) {
            ICPPASTUsingDeclaration et = as(expected);
            ICPPASTUsingDeclaration at = as(actual);
            return et.isTypename() == at.isTypename();
         } else if (expected instanceof IASTSimpleDeclaration || expected instanceof ICPPASTAliasDeclaration ||
                    expected instanceof ICPPASTUsingDirective) {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof ICPPASTTemplateParameter) {
         /* Template Parameter */

         ICPPASTTemplateParameter e = as(expected);
         ICPPASTTemplateParameter a = as(actual);
         if (e.isParameterPack() != a.isParameterPack()) return false;
         if (expected instanceof ICPPASTSimpleTypeTemplateParameter) {
            ICPPASTSimpleTypeTemplateParameter et = as(expected);
            ICPPASTSimpleTypeTemplateParameter at = as(actual);
            return et.getParameterType() == at.getParameterType();
         } else if (expected instanceof ICPPASTParameterDeclaration || expected instanceof ICPPASTTemplatedTypeTemplateParameter) {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         }

      } else if (expected instanceof ICPPASTDeclarator) {
         /* Declarator */

         /* Terminal checks */
         if (expected instanceof ICPPASTFunctionDeclarator) {
            ICPPASTFunctionDeclarator e = as(expected);
            ICPPASTFunctionDeclarator a = as(actual);
            return e.isConst() == a.isConst() && e.isFinal() == a.isFinal() && e.isPureVirtual() == a.isPureVirtual() && e.isVolatile() == a
                  .isVolatile();
         } else {
            /* All tokens are stored in the child-nodes */
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof ICPPASTDeclSpecifier) {
         /* DeclSpecifier */

         ICPPASTDeclSpecifier e = as(expected);
         ICPPASTDeclSpecifier a = as(actual);
         if (e.isConst() != a.isConst() || e.isVirtual() != a.isVirtual() || e.isVolatile() != a.isVolatile() || e.isConstexpr() != a.isConstexpr() ||
             e.isExplicit() != a.isExplicit() || e.isFriend() != a.isFriend() || e.isRestrict() != a.isRestrict() || e.isThreadLocal() != a
                   .isThreadLocal() || e.getStorageClass() != a.getStorageClass()) return false;

         /* Terminal checks */
         if (expected instanceof ICPPASTNamedTypeSpecifier) {
            ICPPASTNamedTypeSpecifier et = as(expected);
            ICPPASTNamedTypeSpecifier at = as(actual);
            return et.isTypename() == at.isTypename();
         } else if (expected instanceof ICPPASTCompositeTypeSpecifier) {
            ICPPASTCompositeTypeSpecifier et = as(expected);
            ICPPASTCompositeTypeSpecifier at = as(actual);
            return et.isFinal() == at.isFinal() && et.isVirtual() == at.isVirtual() && et.getKey() == at.getKey();
         } else if (expected instanceof ICPPASTElaboratedTypeSpecifier) {
            ICPPASTElaboratedTypeSpecifier et = as(expected);
            ICPPASTElaboratedTypeSpecifier at = as(actual);
            return et.getKind() == at.getKind();
         } else if (expected instanceof ICPPASTEnumerationSpecifier) {
            ICPPASTEnumerationSpecifier et = as(expected);
            ICPPASTEnumerationSpecifier at = as(actual);
            return et.isOpaque() == at.isOpaque() && et.isScoped() == at.isScoped();
         } else {
            return defaultHandler(expected, actual, args);
         } /* Continue comparing raw-signature */
      } else if (expected instanceof IASTExpression) {
         /* Expressions */

         if (expected instanceof ICPPASTUnaryExpression) {
            ICPPASTUnaryExpression et = as(expected);
            ICPPASTUnaryExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTBinaryExpression) {
            ICPPASTBinaryExpression et = as(expected);
            ICPPASTBinaryExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTLiteralExpression) {
            ICPPASTLiteralExpression et = as(expected);
            ICPPASTLiteralExpression at = as(actual);
            return et.getKind() == at.getKind() && equalsRaw(et, at, args);
         } else if (expected instanceof ICPPASTCastExpression) {
            ICPPASTCastExpression et = as(expected);
            ICPPASTCastExpression at = as(actual);
            return et.getOperator() == at.getOperator();
         } else if (expected instanceof ICPPASTNewExpression) {
            ICPPASTNewExpression et = as(expected);
            ICPPASTNewExpression at = as(actual);
            return et.isArrayAllocation() == at.isArrayAllocation() && et.isGlobal() == at.isGlobal() && et.isNewTypeId() == at.isNewTypeId();
         } else if (expected instanceof ICPPASTDeleteExpression) {
            ICPPASTDeleteExpression et = as(expected);
            ICPPASTDeleteExpression at = as(actual);
            return et.isVectored() == at.isVectored() && et.isGlobal() == at.isGlobal();
         } else if (expected instanceof ICPPASTFieldReference) {
            ICPPASTFieldReference et = as(expected);
            ICPPASTFieldReference at = as(actual);
            return et.isPointerDereference() == at.isPointerDereference() && et.isTemplate() == at.isTemplate();
         } else if (expected instanceof ICPPASTLambdaExpression) {
            ICPPASTLambdaExpression et = as(expected);
            ICPPASTLambdaExpression at = as(actual);
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

         ICPPASTReferenceOperator et = as(expected);
         ICPPASTReferenceOperator at = as(actual);
         return et.isRValueReference() == at.isRValueReference();
      } else if (expected instanceof IASTPointer) {
         /* Pointers */

         IASTPointer et = as(expected);
         IASTPointer at = as(actual);
         return et.isConst() == at.isConst() && et.isRestrict() == at.isRestrict() && et.isVolatile() == at.isVolatile();
      } else if (expected instanceof IASTStatement) {
         /* Statements */

         return defaultHandler(expected, actual, args);
      } else if (expected instanceof ICPPASTPackExpandable) {
         /* ICPPASTPackExpandable */

         ICPPASTPackExpandable e = as(expected);
         ICPPASTPackExpandable a = as(actual);
         if (e.isPackExpansion() != a.isPackExpansion()) return false;

         if (expected instanceof ICPPASTInitializerList) {
            ICPPASTInitializerList et = as(expected);
            ICPPASTInitializerList at = as(actual);
            return et.getSize() == at.getSize();
         } else if (expected instanceof ICPPASTBaseSpecifier) {
            ICPPASTBaseSpecifier et = as(expected);
            ICPPASTBaseSpecifier at = as(actual);
            return et.isVirtual() == at.isVirtual() && et.getVisibility() == at.getVisibility();
         } else if (expected instanceof ICPPASTCapture) {
            ICPPASTCapture et = as(expected);
            ICPPASTCapture at = as(actual);
            return et.capturesThisPointer() == at.capturesThisPointer() && et.isByReference() == at.isByReference();
         } else if (expected instanceof ICPPASTTypeId) {
            return true;
         } else {
            return defaultHandler(expected, actual, args);
         }

      } else if (expected instanceof ICPPASTName) {
         /* Names */

         ICPPASTName e = as(expected);
         ICPPASTName a = as(actual);
         if (e.isQualified() != a.isQualified()) return false;

         if (expected instanceof ICPPASTTemplateId) {
            ICPPASTTemplateId et = as(expected);
            ICPPASTTemplateId at = as(actual);
            return et.isDeclaration() == at.isDeclaration();
         } else if (expected instanceof ICPPASTQualifiedName) {
            ICPPASTQualifiedName et = as(expected);
            ICPPASTQualifiedName at = as(actual);
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

   private static boolean defaultHandler(IASTNode expected, IASTNode actual, EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.DEBUG_COMPARE_SIGNATURE_FOR_DEFAULT_HANDLER)) return equalsRaw(expected, actual, args);
      return true;
   }

   private static boolean equalsRaw(IASTNode expected, IASTNode actual, EnumSet<ComparisonArg> args) {
      if (args.contains(ComparisonArg.DEBUG_NO_NORMALIZING)) {
         return equalsRaw(expected, actual);
      } else {
         return equalsNormalizedRaw(expected, actual);
      }
   }

   protected static <T extends IASTNode> boolean equalsRaw(T left, T right) {
      return left.getRawSignature().equals(right.getRawSignature());
   }

   protected static <T extends IASTNode> boolean equalsNormalizedRaw(T left, T right) {
      return normalizeCPP(left.getRawSignature()).equals(normalizeCPP(right.getRawSignature()));
   }

   protected static String getSignature(IASTNode node) {
      String signature = node.getRawSignature();
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
      List<ComparisonAttribute> attributes = new ArrayList<>();

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
      if (node == null) return "NULL";
      return node.getClass().getSimpleName();
   }

   protected static String getRawSignatureOrMissing(IASTNode node) {
      return node == null ? NODE_MISSING : getSignature(node);
   }

   protected static String getRawSignatureContextOrMissing(IASTNode node, EnumSet<ComparisonArg> args) {
      if (node == null) return NODE_MISSING;
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
      String signature = parent.getRawSignature();;
      if (args.contains(ComparisonArg.DO_NOT_PRINT_RELATIVE_LINE_NOS)) {
         return signature;
      } else {
         String[] lines = signature.split(WorkspaceUtil.getWorkspaceLineSeparator());
         int beginningLineNo = loc.getStartingLineNumber();
         for (int i = 0; i < lines.length; i++) {
            lines[i] = String.format("%02d| ", beginningLineNo + i) + lines[i];
         }
         return String.join(WorkspaceUtil.getWorkspaceLineSeparator(), lines);
      }
   }

   protected static String getTURawSignatureOrMissing(IASTNode node, EnumSet<ComparisonArg> args) {
      if (node == null || node.getTranslationUnit() == null) {
         return NODE_MISSING;
      } else {
         String signature = node.getTranslationUnit().getRawSignature();
         if (args.contains(ComparisonArg.DO_NOT_PRINT_RELATIVE_LINE_NOS)) {
            return signature;
         } else {
            String[] lines = signature.split(WorkspaceUtil.getWorkspaceLineSeparator());
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
      INTERRUPTED("The comparison encountered an InterruptedException and is therefore invalid!");
      //@formatter:on

      String desc;

      ComparisonState(String desc) {
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

      ComparisonAttrID(boolean expected, boolean actual, String prefix) {
         this.prefix = prefix;
         this.expected = expected;
         this.actual = actual;
      }

   }

   public static class ComparisonAttribute {

      public ComparisonAttrID id;
      public String           info;
      public String           value;

      public ComparisonAttribute(final ComparisonAttrID id, String value) {
         this(id, "", value);
      }

      /**
       * Formatted in the style "{id.prefix} {info}:\n{value}"
       */
      public ComparisonAttribute(final ComparisonAttrID id, String info, String value) {
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
      public String getAttributeString(ComparisonAttrID id) {
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

      public void ifEqual(Consumer<ComparisonResult> fun) {
         if (isEqual()) {
            fun.accept(this);
         }
      }

      public void ifUnequal(Consumer<ComparisonResult> fun) {
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
