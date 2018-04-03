package name.graf.emanuel.testfileeditor.ui.support.outline;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.model.TestFile;
import name.graf.emanuel.testfileeditor.model.node.Class;
import name.graf.emanuel.testfileeditor.model.node.Expected;
import name.graf.emanuel.testfileeditor.model.node.File;
import name.graf.emanuel.testfileeditor.model.node.Language;
import name.graf.emanuel.testfileeditor.model.node.Selection;
import name.graf.emanuel.testfileeditor.model.node.Test;


public class TestFileLabelProvider extends LabelProvider {

   private static final Image TEST_FILE = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/test_file.gif"))
         .createImage();
   private static final Image TEST      = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/test.gif")).createImage();
   private static final Image LANGUAGE  = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/lang.gif")).createImage();
   private static final Image EXPECTED  = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/exp.gif")).createImage();
   private static final Image FILE      = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/file.gif")).createImage();
   private static final Image CLASS     = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/classname.gif"))
         .createImage();
   private static final Image SELECTION = ImageDescriptor.createFromURL(Activator.getDefault().getBundle().getEntry("icons/sel.gif")).createImage();

   @Override
   public Image getImage(final Object element) {
      if (element instanceof TestFile) { return TEST_FILE; }
      if (element instanceof Test) { return TEST; }
      if (element instanceof Language) { return LANGUAGE; }
      if (element instanceof Expected) { return EXPECTED; }
      if (element instanceof File) { return FILE; }
      if (element instanceof Class) { return CLASS; }
      if (element instanceof Selection) { return SELECTION; }
      return super.getImage(element);
   }
}
