/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.cdttest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

import ch.hsr.ifs.iltis.core.resources.FileUtil;


public class FileCache {

   private static Map<IFile, IDocumentProvider> connectedDocuments = new LinkedHashMap<IFile, IDocumentProvider>();

   public static IDocument getDocument(IFile file) {
      if (connectedDocuments.containsKey(file)) { return connectedDocuments.get(file).getDocument(file); }
      try {
         IDocumentProvider provider = new TextFileDocumentProvider();
         if (file == null) { return null; }
         provider.connect(file);
         connectedDocuments.put(file, provider);
         return provider.getDocument(file);
      } catch (CoreException e) {
         return null;
      }
   }

   public static IDocument getDocument(URI fileUri) {
      IFile file = FileUtil.toIFile(fileUri);
      return getDocument(file);
   }

   public static void clean() {
      for (Entry<IFile, IDocumentProvider> curEntry : connectedDocuments.entrySet()) {
         curEntry.getValue().disconnect(curEntry.getKey());
      }
      connectedDocuments.clear();
   }

}
