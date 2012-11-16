/*******************************************************************************
 * Copyright (c) 2012 Institute for Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.rts.junit4;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import ch.hsr.ifs.cdttesting.TestingPlugin;

public class RtsFileInfo {

	private static final String XML_ACTIVATOR_CLASS = "activatorClass";
	private static final String XML_SOURCE_LOCATION = "sourceLocation";
	private static final String RTS_FILE_EXTENSION = ".rts";
	private String completeRTSPath;
	private BufferedReader rtsFileReader;
	private IConfigurationElement activeExtension;

	public RtsFileInfo(Class<? extends CDTProjectJUnit4RtsTest> testClass) throws FileNotFoundException, CoreException {
		if (!initRtsFilePathWithAnnotation(testClass) && !initRtsFilePathWithName(testClass.getName())) {
			throw new FileNotFoundException(testClass.getSimpleName() + RTS_FILE_EXTENSION);
		}
		initReader();
	}

	public RtsFileInfo(String rtsFileName) throws CoreException, FileNotFoundException {
		if (rtsFileName.endsWith(RTS_FILE_EXTENSION)) {
			rtsFileName = rtsFileName.substring(0, rtsFileName.length() - 4);
		}
		if (!initRtsFilePathWithName(rtsFileName)) {
			throw new FileNotFoundException(rtsFileName + RTS_FILE_EXTENSION);
		}
		initReader();
	}

	public void closeReaderStream() throws IOException {
		if (rtsFileReader != null) {
			rtsFileReader.close();
		}
	}

	private void initReader() throws CoreException {
		InputStream resourceAsStream = getActivatorClass().getResourceAsStream(completeRTSPath);
		rtsFileReader = new BufferedReader(new InputStreamReader(resourceAsStream));
	}

	private boolean initRtsFilePathWithName(String name) throws CoreException {
		for (IConfigurationElement curElement : getExtensions()) {
			activeExtension = curElement;
			String testResourcePrefix = curElement.getAttribute(XML_SOURCE_LOCATION);
			StringBuilder completeRTSPathBuilder = new StringBuilder(testResourcePrefix);
			completeRTSPathBuilder.append(name.substring(getTestPackagePrefix().length()).replace(".", "/")).append(RTS_FILE_EXTENSION);
			InputStream resourceAsStream = getActivatorClass().getResourceAsStream(completeRTSPathBuilder.toString());
			if (resourceAsStream != null) {
				completeRTSPath = completeRTSPathBuilder.toString();
				return true;
			}
		}
		return false;
	}

	private String getTestPackagePrefix() throws CoreException {
		return getActivatorClass().getPackage().getName();
	}

	private Class<? extends AbstractUIPlugin> getActivatorClass() throws CoreException {
		AbstractUIPlugin activator = (AbstractUIPlugin) activeExtension.createExecutableExtension(XML_ACTIVATOR_CLASS);
		return activator.getClass();
	}

	private boolean initRtsFilePathWithAnnotation(Class<? extends CDTProjectJUnit4RtsTest> testClass) {
		RunFor runForAnnotation = testClass.getAnnotation(RunFor.class);
		boolean hasAnnotation = runForAnnotation != null;
		if (hasAnnotation) {
			completeRTSPath = runForAnnotation.rtsFile();
		}
		return hasAnnotation;
	}

	public BufferedReader getRtsFileReader() {
		return rtsFileReader;
	}

	private IConfigurationElement[] getExtensions() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] extensions = reg.getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID);
		return extensions;
	}
	
	public Bundle getBundle() {
		return Platform.getBundle(activeExtension.getContributor().getName());
	}
}
