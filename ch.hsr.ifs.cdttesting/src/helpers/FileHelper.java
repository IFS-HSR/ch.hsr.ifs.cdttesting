/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package helpers;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class FileHelper {

	public static final String NL = System.getProperty("line.separator");
	public static final int NL_LENGTH = NL.length();
	public static final char PATH_SEGMENT_SEPARATOR = File.separatorChar;

	private static Map<IFile, IDocumentProvider> connectedDocuments = new LinkedHashMap<IFile, IDocumentProvider>();

	public static IPath uriToPath(URI fileUri) {
		return new Path(fileUri.getPath());
	}

	public static IPath stringToPath(String strPath) {
		return new Path(strPath);
	}

	public static URI stringToUri(String fileString) {
		return new File(fileString).toURI();
	}

	public static String uriToStringPath(URI fileUri) {
		return pathToStringPath(uriToPath(fileUri));
	}

	public static String pathToStringPath(IPath filePath) {
		return filePath.toOSString();
	}

	public static URI pathToUri(IPath path) {
		return stringToUri(pathToStringPath(path));
	}

	public static IFile getIFile(URI fileURI) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IFile[] files = root.findFilesForLocationURI(fileURI);
		if (files.length == 1) {
			return files[0];
		}
		for (IFile curFile : files) {
			if (fileURI.getPath().endsWith(curFile.getFullPath().toString())) {
				return curFile;
			}
		}
		return null;
	}

	public static String makeProjectRelativePath(String absolutePathStr, ICProject project) {
		IPath projectPath = project.getProject().getLocation();
		IPath absolutePath = new Path(absolutePathStr);
		return absolutePath.makeRelativeTo(projectPath).toOSString();
	}

	public static IIndexFile getIndexFile(IIndexFileLocation fileLocation, IIndex index) throws CoreException {
		IIndexFile[] files = index.getFiles(fileLocation);
		return (files.length != 0) ? files[0] : null;
	}

	public static IASTFileLocation getNodeFileLocation(IASTNode node) {
		return (node.getFileLocation() != null) ? node.getFileLocation() : getNodeFileLocation(node.getParent());
	}

	/**
	 * Replaces all occurrences of '\\' and '/' in a given path with the separator for path segments of the underlying system.
	 * 
	 * @param path
	 * @return Path with replaced separator characters.
	 */
	public static String useSystemSeparators(String path) {
		char systemSeparator = File.separatorChar;
		return path.replace('\\', systemSeparator).replace('/', systemSeparator);
	}

	public static String getSmartFilePath(String absolutePath, ICProject project) {
		String relativePath = makeProjectRelativePath(absolutePath, project);
		return getSmartFilePath(absolutePath, relativePath);
	}

	public static String getSmartFilePath(String absolutePath, String relativePath) {
		return relativePath.startsWith(".." + FileHelper.PATH_SEGMENT_SEPARATOR + "..") ? absolutePath : relativePath;
	}

	public static IDocument getDocument(IFile file) {
		if (connectedDocuments.containsKey(file)) {
			return connectedDocuments.get(file).getDocument(file);
		}
		try {
			IDocumentProvider provider = new TextFileDocumentProvider();
			if (file == null) {
				return null;
			}
			provider.connect(file);
			connectedDocuments.put(file, provider);
			return provider.getDocument(file);
		} catch (CoreException e) {
			return null;
		}
	}

	public static IDocument getDocument(URI fileUri) {
		IFile file = FileHelper.getIFile(fileUri);
		return getDocument(file);
	}

	public static void clean() {
		for (Entry<IFile, IDocumentProvider> curEntry : connectedDocuments.entrySet()) {
			curEntry.getValue().disconnect(curEntry.getKey());
		}
		connectedDocuments.clear();
	}
}
