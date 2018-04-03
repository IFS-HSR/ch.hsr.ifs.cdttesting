/*******************************************************************************
 * Copyright (c) 2012 Institute for Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.helpers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import ch.hsr.ifs.cdttesting.cdttest.base.RTSSourceFileTest;
import ch.hsr.ifs.cdttesting.rts.junit4.RunFor;


public class ExtensionPointEvaluator {

   private static final String XML_SOURCE_LOCATION                  = "sourceLocation";
   private static final String RTS_FILE_EXTENSION                   = ".rts";
   private static final String XML_EXTERNAL_SOURCE_LOCATION         = "externalSourceLocation";
   private static final IPath  XML_EXTERNAL_SOURCE_LOCATION_DEFAULT = Path.fromOSString("externalTestResource/");

   private IConfigurationElement              extension;
   private Bundle                             bundle;
   private Class<? extends RTSSourceFileTest> testClass;

   public ExtensionPointEvaluator(Class<? extends RTSSourceFileTest> testClass) {
      this.testClass = testClass;
      init(testClass);
   }

   /**
    * Returns a reader for the rts file with the same name as the test class which was used to create this instance of ExtensionPointEvaluator, or if
    * the test class used had an RunFor annotation present, the file declared in the annotation will be used. The
    * caller is responsible to close the reader.
    */
   public BufferedReader getRtsFileReader() throws FileNotFoundException {
      return getRtsFileReader(testClass.getSimpleName());
   }

   /**
    * Returns a reader for the rts file with the name provided. The
    * caller is responsible to close the reader.
    */
   public BufferedReader getRtsFileReader(String rtsFileName) throws FileNotFoundException {
      if (rtsFileName != null && rtsFileName.endsWith(RTS_FILE_EXTENSION)) {
         rtsFileName = rtsFileName.substring(0, rtsFileName.length() - 4);
      }

      Path rtsPath = generateRTSPathForClass(testClass, rtsFileName, extension);

      if (doesPathExistInBundle(bundle, rtsPath)) {
         return new BufferedReader(new InputStreamReader(getResourceAsStream(bundle, rtsPath)));
      } else {
         throw new FileNotFoundException(String.valueOf(rtsPath) +
                                         " (This might happen, if the testplugin has non, or a fauly extension for extension-point \"" +
                                         TestingPlugin.XML_EXTENSION_POINT_ID + "\")");
      }
   }

   private static boolean doesPathExistInBundle(Bundle bundle, Path path) {
      return FileLocator.findEntries(bundle, path).length > 0;
   }

   private static InputStream getResourceAsStream(Bundle bundle, Path path) {
      try {
         return FileLocator.openStream(bundle, path, false);
      } catch (IOException ignored) {
         return null;
      }
   }

   private boolean init(Class<? extends RTSSourceFileTest> clazz) {
      Bundle bundle = FrameworkUtil.getBundle(clazz);
      for (IConfigurationElement currentExtension : getExtensionsContributedByBundle(bundle)) {
         this.extension = currentExtension;
         this.bundle = bundle;
         return true;
      }
      return false;
   }

   private static Path generateRTSPathForClass(Class<? extends RTSSourceFileTest> clazz, String rtsFileName, IConfigurationElement curElement) {
      RunFor runForAnnotation = clazz.getAnnotation(RunFor.class);
      if (runForAnnotation == null) {
         return buildCompleteRTSPath(clazz.getPackage().getName(), rtsFileName, getTestResourcePrefix(curElement));
      } else {
         return new Path(runForAnnotation.rtsFile());
      }
   }

   private static String getTestResourcePrefix(IConfigurationElement curElement) {
      return curElement.getAttribute(XML_SOURCE_LOCATION);
   }

   private static Path buildCompleteRTSPath(String testPackagePrefix, String testFileName, String testResourcePrefix) {
      String collapsedFolderName = getStringBetweenLastTwoOccurencesOfString(testResourcePrefix, "/");
      return new Path(testResourcePrefix + testPackagePrefix.substring(Math.min(testPackagePrefix.length(), collapsedFolderName.length() + 1))
            .replace(".", "/") + "/" + testFileName + RTS_FILE_EXTENSION);
   }

   private static String getStringBetweenLastTwoOccurencesOfString(String fullString, String marker) {
      String withoutEverythingAfterLastOccurence = fullString.substring(0, fullString.lastIndexOf(marker));
      return withoutEverythingAfterLastOccurence.substring(withoutEverythingAfterLastOccurence.lastIndexOf(marker) + 1,
            withoutEverythingAfterLastOccurence.length());
   }

   private static IConfigurationElement[] getExtensionsContributedByBundle(Bundle bundle) {
      return Stream.of(RegistryFactory.getRegistry().getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID)).filter((element) -> element
            .getContributor().getName().equals(bundle.getSymbolicName())).toArray(IConfigurationElement[]::new);
   }

   public IPath getExternalTestResourcePath() {
      String result = extension.getAttribute(XML_EXTERNAL_SOURCE_LOCATION);
      return result != null ? Path.fromOSString(result) : XML_EXTERNAL_SOURCE_LOCATION_DEFAULT;
   }

   public Enumeration<URL> getExternalResourcesForActiveBundle() {
      return bundle.findEntries(getExternalTestResourcePath().toOSString(), "*", true);
   }

}
