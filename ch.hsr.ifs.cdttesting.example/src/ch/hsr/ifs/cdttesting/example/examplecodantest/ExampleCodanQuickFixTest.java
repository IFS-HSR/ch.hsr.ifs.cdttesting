package ch.hsr.ifs.cdttesting.example.examplecodantest;

import java.util.EnumSet;

import org.eclipse.ui.IMarkerResolution;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingQuickfixTest;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.example.examplecodantest.MyCodanChecker.MyProblemId;
import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


public class ExampleCodanQuickFixTest extends CDTTestingQuickfixTest {

   @Override
   protected IProblemId getProblemId() {
      return MyProblemId.EXAMPLE_ID;
   }

   @Test
   public void runTest() throws Throwable {
      runQuickfixForAllMarkersAndAssertAllEqual();
   }
   
   @Override
   protected EnumSet<ComparisonArg> makeComparisonArguments() {
      return EnumSet.of(ComparisonArg.USE_SOURCE_COMPARISON);
   }
   
   @Override
   protected IMarkerResolution createMarkerResolution() {
      return new MyQuickFix();
   }
}
