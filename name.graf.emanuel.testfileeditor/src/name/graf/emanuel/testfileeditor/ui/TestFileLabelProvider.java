package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import name.graf.emanuel.testfileeditor.Activator;

public class TestFileLabelProvider extends LabelProvider {
    private static final Image testFile = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/test_file.gif")).createImage();
    private static final Image test = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/test.gif")).createImage();
    private static final Image lang = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/lang.gif")).createImage();
    private static final Image exp = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/exp.gif")).createImage();
    private static final Image file = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/file.gif")).createImage();
    private static final Image className = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/classname.gif")).createImage();
    private static final Image selection = ImageDescriptor
            .createFromURL(Activator.getDefault().getBundle().getEntry("icons/sel.gif")).createImage();

    public Image getImage(final Object element) {
        if (element instanceof TestFile) {
            return TestFileLabelProvider.testFile;
        }
        if (element instanceof Test) {
            return TestFileLabelProvider.test;
        }
        if (element instanceof LanguageDef) {
            return TestFileLabelProvider.lang;
        }
        if (element instanceof ExpectedNode) {
            return TestFileLabelProvider.exp;
        }
        if (element instanceof FileDefNode) {
            return TestFileLabelProvider.file;
        }
        if (element instanceof ClassNameNode) {
            return TestFileLabelProvider.className;
        }
        if (element instanceof SelectionNode) {
            return TestFileLabelProvider.selection;
        }
        return super.getImage(element);
    }
}
