package ch.hsr.ifs.cdttesting.test.cdttest.tests;

import java.io.IOException;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.core.parser.tests.ast2.AST2TestBase;
import org.eclipse.cdt.core.testplugin.util.TestSourceReader;
import org.eclipse.cdt.internal.core.parser.ParserException;

import ch.hsr.ifs.cdttesting.cdttest.ASTComparison;
import ch.hsr.ifs.cdttesting.test.Activator;
import junit.framework.AssertionFailedError;


@SuppressWarnings("restriction")
public class ASTComparisonTest extends AST2TestBase {

   protected CharSequence[] getContents(int sections) throws IOException {
      Activator plugin = Activator.getDefault();
      if (plugin == null) throw new AssertionFailedError("This test must be run as a JUnit plugin test");
      return TestSourceReader.getContentsForTest(plugin.getBundle(), "src", getClass(), getName(), sections);
   }

   private void assertSameAST() throws IOException, ParserException {
      CharSequence[] sections = getContents(2);
      IASTTranslationUnit first = parse(sections[0].toString(), ParserLanguage.CPP, true);
      IASTTranslationUnit second = parse(sections[1].toString(), ParserLanguage.CPP, true);
      ASTComparison.assertEqualsAST(first, second, true);
   }

   // int i = 1 + 1;

   // int i = 1 * 1;
   public void testDifferentBinaryOperator() throws Exception {
      assertSameAST();
   }

   // int i = +1;

   // int i = -1;
   public void testDifferentUnaryOperator() throws Exception {
      assertSameAST();
   }

   // [[noreturn]]
   // void foo() {while(true);}

   // void foo() {while(true);}
   public void testMissingAttribute() throws Exception {
      assertSameAST();
   }

   // int i; //Razupaltuff

   // int i;
   public void testDifferentMissingComment() throws Exception {
      assertSameAST();
   }

   // template<int i>
   // struct Tpl{};
   // extern template class Tpl<5>;

   // template<int i>
   // struct Tpl{};
   // static template class Tpl<5>;
   public void testExplicitTemplate() throws Exception {
      assertSameAST();
   }

   // extern "C" {
   // int i;
   // }

   // extern "VisualBasic" {
   // int i;
   // }
   public void testDifferentLinkageSpecification() throws Exception {
      assertSameAST();
   }

   // inline namespace NS {
   // int i;
   // }

   // namespace NS {
   // int i;
   // }
   public void testInlineNamespaceDefinition() throws Exception {
      assertSameAST();
   }

   // export template<int i>
   // struct Tpl{};

   // template<int i>
   // struct Tpl{};
   public void testMissingExportTemplateDeclaration() throws Exception {
      assertSameAST();
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
      assertSameAST();
   }

   // struct S {};

   // class S {};
   public void testDifferentClassKeyword() throws Exception {
      assertSameAST();
   }

   // struct S;

   // class S;
   public void testDifferentKeywordForElaboratedTypeSpecifer() throws Exception {
      assertSameAST();
   }

   // enum E{};

   // enum class E{};
   public void testDifferentKindOfEnum() throws Exception {
      assertSameAST();
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
      assertSameAST();
   }

   // int i = static_cast<int>(1.0f);

   // int i = reinterpret_cast<int>(1.0f);
   public void testDifferentKindOfCastExpression() throws Exception {
      assertSameAST();
   }

   // void foo(int * i) {
   // delete i;
   // }

   // void foo(int * i) {
   // delete[] i;
   // }
   public void testVectoredDelete() throws Exception {
      assertSameAST();
   }

   // int * foo() {
   // return ::new int{};
   // }

   // int * foo() {
   // return new int{};
   // }
   public void testGlobalNew() throws Exception {
      assertSameAST();
   }

   // void foo() {
   // auto l = [&]{};
   // }

   // void foo() {
   // auto l = [=]{};
   // }
   public void testDifferentLambdaDefaultCapture() throws Exception {
      assertSameAST();
   }

   // template<typename...T>
   // void foo(A<T...>);

   // template<typename...T>
   // void foo(A<T>);
   public void testPackExpansionInType() throws Exception {
      assertSameAST();
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
      assertSameAST();
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
      assertSameAST();
   }
}