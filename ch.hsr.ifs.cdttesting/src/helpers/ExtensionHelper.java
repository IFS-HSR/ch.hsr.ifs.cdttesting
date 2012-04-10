/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package helpers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ch.hsr.ifs.cdttesting.TestingPlugin;

public class ExtensionHelper {

	public static IConfigurationElement[] getExtensions() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] extensions = reg.getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID);
		return extensions;
	}

	public static String getTestPackagePrefix() {
		return getActivatorClass().getPackage().getName();
	}

	public static Class<? extends AbstractUIPlugin> getActivatorClass() {
		try {
			for (IConfigurationElement curElement : getExtensions()) {
				if (curElement.getName().equals(TestingPlugin.XML_ACTIVATOR_ELEMTENT_NAME)) {
					AbstractUIPlugin activator = (AbstractUIPlugin) curElement.createExecutableExtension("class");
					return activator.getClass();
				}
			}
		} catch (CoreException e) {
			// ignore
		}
		return null;
	}
}
