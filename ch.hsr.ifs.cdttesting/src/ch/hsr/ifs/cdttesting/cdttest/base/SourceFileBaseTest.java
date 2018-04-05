/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 *
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;

import ch.hsr.ifs.iltis.core.data.StringInputStream;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingConfigConstants;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.IProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


public abstract class SourceFileBaseTest extends ProjectHolderBaseTest {

   /**
    * Compares by using the AST, in addition this compares also comments and includes. On fail it prints the whole AST.
    */
   protected static final EnumSet<ComparisonArg> COMPARE_AST_AND_COMMENTS_AND_INCLUDES = EnumSet.of(ComparisonArg.COMPARE_COMMENTS,
         ComparisonArg.COMPARE_INCLUDE_DIRECTIVES, ComparisonArg.PRINT_WHOLE_ASTS_ON_FAIL);

   /**
    * Compares by using the AST, additionally this compares includes. On fail it prints the whole AST.
    */
   protected static final EnumSet<ComparisonArg> COMPARE_AST_AND_INCLUDES = EnumSet.of(ComparisonArg.COMPARE_INCLUDE_DIRECTIVES,
         ComparisonArg.PRINT_WHOLE_ASTS_ON_FAIL);

   /**
    * The name of the primary test source file. This can be set by adding primaryFile=foo.cpp to the config.
    */
   private String primaryTestSourceFileName;

   protected Language language;

   /**
    * Key: file name, value: test file
    */
   protected LinkedHashMap<String, TestSourceFile> testFiles = new LinkedHashMap<>();

   private boolean calledConfigureTest = false;

   /**
    * Loads config, loads selections, and populates the testFiles map form the passed files.
    * 
    * This method is called by the testing framework
    */
   public void initTestSourceFiles(final List<TestSourceFile> files) {
      TestSourceFile configFile = null;

      for (final TestSourceFile file : files) {
         if (isConfigFile(file)) {
            configFile = file;
         } else {
            if (primaryTestSourceFileName == null) primaryTestSourceFileName = file.getName();
            testFiles.put(file.getName(), file);
         }
      }
      initializeConfiguration(extractProperties(configFile));
   }

   private boolean isConfigFile(TestSourceFile file) {
      return file.getName().equals(CDTTestingConfigConstants.CONFIG_FILE_NAME);
   }

   public void setLanguage(Language language) {
      this.language = language;
   }

   private Properties extractProperties(final TestSourceFile configFile) {
      final Properties properties = new Properties();
      if (configFile != null) {
         try (InputStream is = new StringInputStream(configFile.getSource())) {
            properties.load(is);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
      return properties;
   }

   private void initializeConfiguration(final Properties properties) {
      initCommonFields(properties);
      configureTest(properties);
      assertAllSuperMethodsCalled(calledConfigureTest, "configureTest");
   }

   /**
    * Can be overloaded to use properties to configure the test.
    * <p>
    * Overriding methods should always call {@code super.configureTest(Properties)}
    * 
    * @param properties
    *        The properties from the TestSourceFile
    * 
    * @noreference Do not call this method directly
    */
   protected void configureTest(final Properties properties) {
      this.calledConfigureTest = true;
   };

   private void initCommonFields(final Properties properties) {
      Optional.ofNullable(properties.getProperty(CDTTestingConfigConstants.PRIMARY_FILE, null)).ifPresent(name -> primaryTestSourceFileName = name);
   }

   @Override
   protected void initProjectFiles() throws Exception {
      stageTestSourceFileForImportForBothProjects(testFiles.values());
      super.initProjectFiles();
   }

   private void formatDocumentForBothProjects(String relativePath) throws InterruptedException {
      scheduleAndJoinBoth(currentProjectHolder.formatFileAsync(currentProjectHolder.makeProjectAbsolutePath(relativePath)), expectedProjectHolder
            .formatFileAsync(currentProjectHolder.makeProjectAbsolutePath(relativePath)));

   }

   /**
    * @return The name of the primary test source file (the first file defined in this test-case)
    */
   protected String getNameOfPrimaryTestFile() {
      return primaryTestSourceFileName;
   }

   /**
    * Convenience method for {@code getCurrentCElement(getCurrentIFile(testSourceFileName))}
    * 
    * @throws IllegalArgumentException
    *         If the no file with this name exists.
    */
   protected Optional<ICElement> getCurrentCElement(String testSourceFileName) {
      return getCurrentCElement(getCurrentIFile(testSourceFileName));
   }

   /**
    * Convenience method to get ICElement represented by this path in expectedProjectHolder
    * 
    * @throws IllegalArgumentException
    *         If the no file with this name exists.
    */
   protected Optional<ICElement> getExpectedCElement(String testSourceFileName) {
      return getExpectedCElement(getExpectedIFile(testSourceFileName));
   }

   /**
    * Convenience method to get ICElement of the primary test source file
    * 
    * @throws IllegalArgumentException
    *         If the no primary file exists.
    */
   protected Optional<ICElement> getPrimaryCElementFromCurrentProject() {
      return getCurrentCElement(getNameOfPrimaryTestFile());
   }

   /**
    * Convenience method to get the text selection of the primary file
    */
   protected Optional<ITextSelection> getSelectionOfPrimaryTestFile() {
      return getSelection(getNameOfPrimaryTestFile());
   }

   /**
    * Convenience method to get the text selection from a file name
    */
   protected Optional<ITextSelection> getSelection(String testSourceFileName) {
      return Optional.ofNullable(testFiles.get(testSourceFileName)).flatMap(TestSourceFile::getSelection);
   }

   /**
    * Convenience method to get IFile of current project for the name of a test source file
    * 
    * @throws IllegalArgumentException
    *         If the no file with this name exists.
    */
   protected IFile getCurrentIFile(String testSourceFileName) {
      return getIFile(testSourceFileName, currentProjectHolder);
   }

   /**
    * Convenience method to get IFile of expected project for the name of a test source file
    * 
    * @throws IllegalArgumentException
    *         If the no file with this name exists.
    */
   protected IFile getExpectedIFile(String testSourceFileName) {
      return getIFile(testSourceFileName, expectedProjectHolder);
   }

   /**
    * @throws IllegalArgumentException
    *         If the no file with this name exist
    */
   private IFile getIFile(String testSourceFileName, IProjectHolder holder) {
      if (testFiles.containsKey(testSourceFileName)) {
         return getIFile(testFiles.get(testSourceFileName), holder);
      } else {
         throw new IllegalArgumentException("No such test file \"" + testSourceFileName + "\" found.");
      }
   }

   /**
    * Convenience method to get the {@code IFile} with was created from this test source file in the current project.
    */
   protected IFile getCurrentIFile(TestSourceFile testSourceFile) {
      return getIFile(testSourceFile, currentProjectHolder);
   }

   /**
    * Convenience method to get the {@code IFile} with was created from this test source file in the expected project.
    */
   protected IFile getExpectedIFile(TestSourceFile testSourceFile) {
      return getIFile(testSourceFile, expectedProjectHolder);
   }

   private IFile getIFile(TestSourceFile testSourceFile, IProjectHolder holder) {
      return holder.getFile(testSourceFile.getName());
   }

   /* --- Comparison --- */

   /**
    * This method uses {@code fastAssertEquals} on each pair of test source files.
    * 
    * @param args
    *        The comparison arguments
    */
   protected void assertAllSourceFilesEqual(EnumSet<ComparisonArg> args) {
      for (final TestSourceFile testFile : testFiles.values()) {
         fastAssertEquals(testFile.getName(), args);
      }
   }

   /**
    * This method compares the files with this name from the current project with the one from the expected project.
    * By default this method uses AST based comparison. This can be changed by passing an EnumSet containing the value
    * {@code ComparisonArg.USE_SOURCE_COMPARISON}
    * 
    * @param testSourceFileName
    *        The name of the test source file
    * @param args
    *        The comparison arguments
    * @throws IllegalArgumentException
    *         If no test source file with this name exists.
    */
   public void fastAssertEquals(final String testSourceFileName, EnumSet<ComparisonArg> args) {
      fastAssertEquals(testSourceFileName, args, ITranslationUnit.AST_SKIP_INDEXED_HEADERS | ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT |
                                                 ITranslationUnit.AST_SKIP_TRIVIAL_EXPRESSIONS_IN_AGGREGATE_INITIALIZERS);
   }

   /**
    * This method compares the files with this name from the current project with the one from the expected project.
    * By default this method uses AST based comparison. This can be changed by passing an EnumSet containing the value
    * {@code ComparisonArg.USE_SOURCE_COMPARISON}
    * 
    * @param testSourceFileName
    *        The name of the test source file
    * @param args
    *        The comparison arguments
    * @param astStyle
    *        The style of the AST to use (used to create the index)
    * @throws IllegalArgumentException
    *         If no test source file with this name exists.
    */
   public void fastAssertEquals(final String testSourceFileName, EnumSet<ComparisonArg> args, int astStyle) {
      if (testFiles.containsKey(testSourceFileName)) {
         if (args.contains(ComparisonArg.USE_SOURCE_COMPARISON)) {
            assertEqualsWithSource(testSourceFileName, args);
         } else {
            assertEqualsWithAST(testSourceFileName, args, astStyle);
         }
      } else {
         throw new IllegalArgumentException("No such test file \"" + testSourceFileName + "\" found.");
      }
   }

   private void assertEqualsWithSource(final String testSourceFileName, EnumSet<ComparisonArg> args) {
      String expectedSource;
      String currentSource;
      if (!args.contains(ComparisonArg.DEBUG_NO_FORMATTING)) {
         try {
            formatDocumentForBothProjects(testSourceFileName);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      expectedSource = getExpectedDocument(getExpectedIFile(testSourceFileName)).get();
      currentSource = getCurrentDocument(getCurrentIFile(testSourceFileName)).get();

      if (!args.contains(ComparisonArg.DEBUG_NO_NORMALIZING)) {
         expectedSource = ASTComparison.normalizeCPP(expectedSource);
         currentSource = ASTComparison.normalizeCPP(currentSource);
      }
      assertEquals("Textual comparison", expectedSource, currentSource);
   }

   private void assertEqualsWithAST(final String testSourceFileName, EnumSet<ComparisonArg> args, int astStyle) {
      IIndex expectedIndex = null;
      IIndex currentIndex = null;
      try {
         //         TODO parallelize creation of index and ast
         expectedIndex = CCorePlugin.getIndexManager().getIndex(getExpectedCProject(), IIndexManager.ADD_DEPENDENCIES & IIndexManager.ADD_DEPENDENT);
         currentIndex = CCorePlugin.getIndexManager().getIndex(getCurrentCProject(), IIndexManager.ADD_DEPENDENCIES & IIndexManager.ADD_DEPENDENT);
         expectedIndex.acquireReadLock();
         currentIndex.acquireReadLock();
         ITranslationUnit expectedTU = CoreModelUtil.findTranslationUnit(getExpectedIFile(testSourceFileName));
         ITranslationUnit currentTU = CoreModelUtil.findTranslationUnit(getCurrentIFile(testSourceFileName));
         //            ASTComparison.assertEqualsAST(expectedTU.getAST(), currentTU.getAST(), args);
         ASTComparison.assertEqualsAST(expectedTU.getAST(expectedIndex, astStyle), currentTU.getAST(currentIndex, astStyle), args);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (expectedIndex != null) expectedIndex.releaseReadLock();
         if (currentIndex != null) currentIndex.releaseReadLock();
      }
   }

}
