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
package ch.hsr.ifs.cdttesting.rts.junit4;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;


public class RtsFileInfo {

   private static final String   XML_TEST_PACKAGE                     = "testPackage";
   private static final String   XML_SOURCE_LOCATION                  = "sourceLocation";
   private static final String   RTS_FILE_EXTENSION                   = ".rts";
   private static final String   XML_EXTERNAL_SOURCE_LOCATION         = "externalSourceLocation";
   private static final String   XML_EXTERNAL_SOURCE_LOCATION_DEFAULT = "externalTestResource/";
   private Path                  completeRTSPath;
   private BufferedReader        rtsFileReader;
   private IConfigurationElement activeExtension;

   public RtsFileInfo(Class<? extends CDTTestingTest> testClass) throws CoreException, FileNotFoundException {
      /* Uses the name of the testClass as name */
      this(testClass, null);
   }

   public RtsFileInfo(Class<? extends CDTTestingTest> testClass, String rtsFileName) throws CoreException, FileNotFoundException {
      if (rtsFileName != null && rtsFileName.endsWith(RTS_FILE_EXTENSION)) {
         rtsFileName = rtsFileName.substring(0, rtsFileName.length() - 4);
      }

      init(testClass, Optional.ofNullable(rtsFileName));
      initReader();
   }

   public void closeReaderStream() throws IOException {
      if (rtsFileReader != null) {
         rtsFileReader.close();
      }
   }

   private void initReader() {
      Bundle bundle = Platform.getBundle(activeExtension.getContributor().getName());
      rtsFileReader = new BufferedReader(new InputStreamReader(getResourceAsStream(bundle, completeRTSPath)));
   }

   private boolean doesPathExistInBundle(Bundle bundle, Path path) {
      return FileLocator.findEntries(bundle, path).length > 0;
   }

   private static InputStream getResourceAsStream(Bundle bundle, Path path) {
      try {
         return FileLocator.openStream(bundle, path, false);
      } catch (IOException ignored) {
         return null;
      }
   }

   private boolean init(Class<? extends CDTTestingTest> clazz, Optional<String> rtsFileName) throws FileNotFoundException {
      Bundle bundle = FrameworkUtil.getBundle(clazz);
      Path temporaryCompleteRTSPath = null;
      for (IConfigurationElement curElement : getExtensionsContributedByBundle(bundle)) {
         temporaryCompleteRTSPath = generateRTSPathForClass(clazz, rtsFileName, curElement);
         if (doesPathExistInBundle(bundle, temporaryCompleteRTSPath)) {
            activeExtension = curElement;
            completeRTSPath = temporaryCompleteRTSPath;
            return true;
         }
      }
      throw new FileNotFoundException(String.valueOf(temporaryCompleteRTSPath) +
                                      " (This might happen, if the testplugin has non, or a fauly extension for extension-point \"" +
                                      TestingPlugin.XML_EXTENSION_POINT_ID + "\")");
   }

   private Path generateRTSPathForClass(Class<? extends CDTTestingTest> clazz, Optional<String> rtsFileName, IConfigurationElement curElement) {
      RunFor runForAnnotation = clazz.getAnnotation(RunFor.class);
      if (runForAnnotation == null) {
         return buildCompleteRTSPath(getPackagePrefix(clazz.getPackage(), curElement), rtsFileName.orElse(clazz.getSimpleName()),
               getTestResourcePrefix(curElement));
      } else {
         return new Path(runForAnnotation.rtsFile());
      }
   }

   private String getTestResourcePrefix(IConfigurationElement curElement) {
      return curElement.getAttribute(XML_SOURCE_LOCATION);
   }

   private static String getPackagePrefix(Package pkg, IConfigurationElement extension) {
      String packagePrefix = extension.getAttribute(XML_TEST_PACKAGE);
      return packagePrefix != null ? packagePrefix : pkg.getName();
   }

   private static Path buildCompleteRTSPath(String testPackagePrefix, String testFileName, String testResourcePrefix) {
      String collapsedFolderName = getStringBetweenLastTwoOccurencesOfString(testResourcePrefix, "/");
      return new Path(testResourcePrefix + testPackagePrefix.substring(Math.min(testPackagePrefix.length(), collapsedFolderName.length() + 1)).replace(".", "/") + "/" + testFileName +
                      RTS_FILE_EXTENSION);
   }

   private static String getStringBetweenLastTwoOccurencesOfString(String fullString, String marker) {
      String withoutEverythingAfterLastOccurence = fullString.substring(0, fullString.lastIndexOf(marker));
      return withoutEverythingAfterLastOccurence.substring(withoutEverythingAfterLastOccurence.lastIndexOf(marker) + 1,
            withoutEverythingAfterLastOccurence.length());
   }

   public BufferedReader getRtsFileReader() {
      return rtsFileReader;
   }

   private IConfigurationElement[] getExtensionsContributedByBundle(Bundle bundle) {
      return Arrays.stream(RegistryFactory.getRegistry().getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID)).filter((
            element) -> element.getContributor().getName().equals(bundle.getSymbolicName())).toArray(IConfigurationElement[]::new);
   }

   public String getExternalTextResourcePath() {
      String result = activeExtension.getAttribute(XML_EXTERNAL_SOURCE_LOCATION);
      return result != null ? result : XML_EXTERNAL_SOURCE_LOCATION_DEFAULT;
   }

   public Enumeration<URL> getExternalResourcesForActiveBundle() {
      return Platform.getBundle(activeExtension.getContributor().getName()).findEntries(getExternalTextResourcePath(), "*", true);
   }
}
