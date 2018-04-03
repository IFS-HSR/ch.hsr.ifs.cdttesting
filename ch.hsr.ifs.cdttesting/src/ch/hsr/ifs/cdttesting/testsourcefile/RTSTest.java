package ch.hsr.ifs.cdttesting.testsourcefile;

import java.util.ArrayList;


public class RTSTest {

   private String                    testName;
   private Language                  language = Language.CPP;
   private ArrayList<TestSourceFile> testSourceFiles = new ArrayList<>();

   public RTSTest(String name) {
      this.testName = name;
   }

   public ArrayList<TestSourceFile> getTestSourceFiles() {
      return testSourceFiles;
   }

   public String getName() {
      return testName;
   }

   public void addFile(TestSourceFile file) {
      testSourceFiles.add(file);
   }

   public void setLanguage(final String language) {
      this.language = Language.valueOf(language);
   }

   public Language getLanguage() {
      return language;
   }

   public enum Language {
      CPP("CPP"), C("C");

      String lang;

      Language(String lang) {
         this.lang = lang;
      }

   }

}
