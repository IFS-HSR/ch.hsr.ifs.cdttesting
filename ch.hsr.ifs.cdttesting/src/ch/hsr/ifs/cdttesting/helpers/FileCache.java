/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.helpers;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

import ch.hsr.ifs.iltis.core.resources.FileUtil;


public class FileCache {

   private Map<URI, IFile>               mappedURIs         = new HashMap<>();
   private Map<IFile, IDocumentProvider> connectedDocuments = new HashMap<IFile, IDocumentProvider>();
   private IDocumentProvider             provider           = new TextFileDocumentProvider();

   /**
    * Do not call this method before before performing any changes
    **/
   public IDocument getDocument(IFile file) {
      if (connectedDocuments.containsKey(file)) {
         /* load from cache */
         return connectedDocuments.get(file).getDocument(file);
      }
      try {
         if (file == null) { return null; }
         file.getProject();
         provider.connect(file);
         connectedDocuments.put(file, provider);
         return provider.getDocument(file);
      } catch (CoreException e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Do not call this method before before performing any changes
    **/
   public IDocument getDocument(URI fileUri) {
      if (mappedURIs.containsKey(fileUri)) return getDocument(mappedURIs.get(fileUri));
      IFile file = FileUtil.toIFile(fileUri);

      mappedURIs.put(fileUri, file);
      return getDocument(file);
   }

   public void clean() {
      for (Entry<IFile, IDocumentProvider> curEntry : connectedDocuments.entrySet()) {
         curEntry.getValue().disconnect(curEntry.getKey());
      }
      mappedURIs.clear();
      connectedDocuments.clear();
   }

}
