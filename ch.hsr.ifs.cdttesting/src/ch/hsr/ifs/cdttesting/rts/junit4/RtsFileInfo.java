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
import java.util.Arrays;
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
      if (!initRtsFilePathWithAnnotation(testClass) && !initRtsFilePathWithName(testClass.getName())) { throw new FileNotFoundException(testClass
            .getSimpleName() + RTS_FILE_EXTENSION + " (This might happen, if the testplugin has no extension for extension-point \"ch.hsr.ifs.cdttesting.testingPlugin\")"); }
      initReader();
   }

   public RtsFileInfo(String rtsFileName) throws CoreException, FileNotFoundException {
      if (rtsFileName.endsWith(RTS_FILE_EXTENSION)) {
         rtsFileName = rtsFileName.substring(0, rtsFileName.length() - 4);
      }
      if (!initRtsFilePathWithName(rtsFileName)) { throw new FileNotFoundException(rtsFileName + RTS_FILE_EXTENSION +
                                                                                   " (This might happen, if the testplugin has no extension for extension-point \"ch.hsr.ifs.cdttesting.testingPlugin\")"); }
      initReader();
   }

   public void closeReaderStream() throws IOException {
      if (rtsFileReader != null) {
         rtsFileReader.close();
      }
   }

   private void initReader() {
      rtsFileReader = new BufferedReader(new InputStreamReader(getResourcesOfCurrentExtensionAsStream()));
   }

   private boolean initRtsFilePathWithName(String name) throws CoreException {
      for (IConfigurationElement curElement : getExtensions()) {
         activeExtension = curElement;

         String testResourcePrefix = activeExtension.getAttribute(XML_SOURCE_LOCATION);
         StringBuilder completeRTSPathBuilder = new StringBuilder(testResourcePrefix);
         /* The +1 removes the dot which allows to use testResourcePrefix ending in / */
         completeRTSPathBuilder.append(name.substring(getTestPackagePrefix().length() + 1).replace(".", "/")).append(RTS_FILE_EXTENSION);
         Path temporaryCompleteRTSPath = new Path(completeRTSPathBuilder.toString());

         /* Testing if file for this path exists in this bundle */
         if (doesPathExistInBundle(getBundleOfExtension(curElement), temporaryCompleteRTSPath)) {
            completeRTSPath = temporaryCompleteRTSPath;
            return true;
         }
      }
      return false;
   }

   private boolean doesPathExistInBundle(Bundle bundle, Path temporaryCompleteRTSPath) {
      return FileLocator.findEntries(bundle, temporaryCompleteRTSPath).length > 0;
   }

   private InputStream getResourcesOfCurrentExtensionAsStream() {
      Bundle bundle = getBundleOfActiveExtension();
      /* Conditional needed for performance (File IO / Stack-unwinding is expensive) */
      if(doesPathExistInBundle(bundle, completeRTSPath)) {
         try {
            return FileLocator.openStream(bundle, completeRTSPath, false);
         } catch (IOException ignored) {
            return null;
         }
      } else {
         return null;
      }

   }

   private String getTestPackagePrefix() throws CoreException {
      String packagePrefix = activeExtension.getAttribute(XML_TEST_PACKAGE);
      return packagePrefix != null ? packagePrefix : getBundleOfActiveExtension().getSymbolicName();
   }

   public Bundle getBundleOfActiveExtension() {
      return getBundleOfExtension(activeExtension);
   }

   private Bundle getBundleOfExtension(IConfigurationElement extension) {
      Bundle contributingBundle = Platform.getBundle(extension.getContributor().getName());
      Bundle[] fragments = Platform.getFragments(contributingBundle);
      if (fragments != null) {
         Optional<Bundle> testFragment = Arrays.stream(fragments).filter((bundle) -> bundle.getSymbolicName().equals(contributingBundle
               .getSymbolicName() + ".tests")).findFirst();
         return testFragment.orElse(contributingBundle);
      } else {
         return contributingBundle;
      }
   }

   private boolean initRtsFilePathWithAnnotation(Class<? extends CDTTestingTest> testClass) throws CoreException {
      RunFor runForAnnotation = testClass.getAnnotation(RunFor.class);
      if (runForAnnotation != null) {
         completeRTSPath = new Path(runForAnnotation.rtsFile());
         for (IConfigurationElement curElement : getExtensionsForBundle(FrameworkUtil.getBundle(testClass))) {
            activeExtension = curElement;
            if (getResourcesOfCurrentExtensionAsStream() != null) { return true; }
         }
      }
      return false;
   }

   public BufferedReader getRtsFileReader() {
      return rtsFileReader;
   }

   private IConfigurationElement[] getExtensionsForBundle(Bundle bundle) {
      return RegistryFactory.getRegistry().getConfigurationElementsFor(bundle.getSymbolicName(), TestingPlugin.XML_EXTENSION_POINT_ID);
   }

   private IConfigurationElement[] getExtensions() {
      return RegistryFactory.getRegistry().getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID);
   }

   public String getexternalTextResourcePath() {
      String result = activeExtension.getAttribute(XML_EXTERNAL_SOURCE_LOCATION);
      return result != null ? result : XML_EXTERNAL_SOURCE_LOCATION_DEFAULT;
   }
}
