package ch.hsr.ifs.cdttesting.test.cdttest.tests;

import java.io.IOException;
import java.util.EnumSet;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.core.parser.tests.ast2.AST2TestBase;
import org.eclipse.cdt.core.testplugin.util.TestSourceReader;
import org.eclipse.cdt.internal.core.parser.ParserException;
import org.osgi.framework.FrameworkUtil;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;


@SuppressWarnings("restriction")
public class ASTComparisonTest extends AST2TestBase {

   protected CharSequence[] getContents(int sections) throws IOException {
      return TestSourceReader.getContentsForTest(FrameworkUtil.getBundle(getClass()), "src", getClass(), getName(), sections);
   }

   private void assertDifferentAST() throws IOException, ParserException {
      CharSequence[] sections = getContents(2);
      IASTTranslationUnit first = parse(sections[0].toString(), ParserLanguage.CPP, true);
      IASTTranslationUnit second = parse(sections[1].toString(), ParserLanguage.CPP, true);
      ComparisonResult result = ASTComparison.equalsAST(first, second, EnumSet.of(ComparisonArg.COMPARE_COMMENTS,
            ComparisonArg.COMPARE_INCLUDE_DIRECTIVES));
      assertTrue(result.state != ComparisonState.EQUAL);
   }

   // int i = 1 + 1;

   // int i = 1 * 1;
   public void testDifferentBinaryOperator() throws Exception {
      assertDifferentAST();
   }

   // int i = +1;

   // int i = -1;
   public void testDifferentUnaryOperator() throws Exception {
      assertDifferentAST();
   }

   // [[noreturn]]
   // void foo() {while(true);}

   // void foo() {while(true);}
   public void testMissingAttribute() throws Exception {
      assertDifferentAST();
   }

   // int i; //Razupaltuff

   // int i;
   public void testDifferentMissingComment() throws Exception {
      assertDifferentAST();
   }

   // template<int i>
   // struct Tpl{};
   // extern template class Tpl<5>;

   // template<int i>
   // struct Tpl{};
   // static template class Tpl<5>;
   public void testExplicitTemplate() throws Exception {
      assertDifferentAST();
   }

   // extern "C" {
   // int i;
   // }

   // extern "VisualBasic" {
   // int i;
   // }
   public void testDifferentLinkageSpecification() throws Exception {
      assertDifferentAST();
   }

   // inline namespace NS {
   // int i;
   // }

   // namespace NS {
   // int i;
   // }
   public void testInlineNamespaceDefinition() throws Exception {
      assertDifferentAST();
   }

   // export template<int i>
   // struct Tpl{};

   // template<int i>
   // struct Tpl{};
   public void testMissingExportTemplateDeclaration() throws Exception {
      assertDifferentAST();
   }

   // template<typename T>
   // struct Tpl {
   // using typename T::Inner;
   // };

   // template<typename T>
   // struct Tpl {
   // using T::Inner;
   // };
   public void testMissingTypenameSpecifier() throws Exception {
      assertDifferentAST();
   }

   // struct S {};

   // class S {};
   public void testDifferentClassKeyword() throws Exception {
      assertDifferentAST();
   }

   // struct S;

   // class S;
   public void testDifferentKeywordForElaboratedTypeSpecifer() throws Exception {
      assertDifferentAST();
   }

   // enum E{};

   // enum class E{};
   public void testDifferentKindOfEnum() throws Exception {
      assertDifferentAST();
   }

   // template<typename T>
   // struct Tpl {
   // typename T::Inner t;
   // };

   // template<typename T>
   // struct Tpl {
   // T::Inner t;
   // };
   public void testTypenameSpecifierLosingTypename() throws Exception {
      assertDifferentAST();
   }

   // int i = static_cast<int>(1.0f);

   // int i = reinterpret_cast<int>(1.0f);
   public void testDifferentKindOfCastExpression() throws Exception {
      assertDifferentAST();
   }

   // void foo(int * i) {
   // delete i;
   // }

   // void foo(int * i) {
   // delete[] i;
   // }
   public void testVectoredDelete() throws Exception {
      assertDifferentAST();
   }

   // int * foo() {
   // return ::new int{};
   // }

   // int * foo() {
   // return new int{};
   // }
   public void testGlobalNew() throws Exception {
      assertDifferentAST();
   }

   // void foo() {
   // auto l = [&]{};
   // }

   // void foo() {
   // auto l = [=]{};
   // }
   public void testDifferentLambdaDefaultCapture() throws Exception {
      assertDifferentAST();
   }

   // template<typename...T>
   // void foo(A<T...>);

   // template<typename...T>
   // void foo(A<T>);
   public void testPackExpansionInType() throws Exception {
      assertDifferentAST();
   }

   // template<typename...B>
   // struct Tpl : public B... {
   // Tpl(B const & ... b) : B(b)... { }
   // };

   // template<typename...B>
   // struct Tpl : public B {
   // Tpl(B const & ... b) : B(b) { }
   // };
   public void testBasePackExpansion() throws Exception {
      assertDifferentAST();
   }

   // template<typename T>
   // struct Tpl {
   // int j = ::Tpl::i;
   // };

   // template<typename T>
   // struct Tpl {
   // int j = Tpl::i;
   // };
   public void testFullyQualifiedName() throws Exception {
      assertDifferentAST();
   }
}
