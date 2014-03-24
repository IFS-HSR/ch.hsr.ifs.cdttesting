package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import name.graf.emanuel.testfileeditor.*;
import java.net.*;
import org.eclipse.jface.resource.*;

public class TestFileLabelProvider extends LabelProvider
{
    private static Image testFile;
    private static Image test;
    private static Image lang;
    private static Image exp;
    private static Image file;
    private static Image className;
    private static Image selection;
    
    static {
        URL testFileUrl = null;
        URL testUrl = null;
        URL langUrl = null;
        URL expUrl = null;
        URL fileUrl = null;
        URL classNameUrl = null;
        URL selectionUrl = null;
        try {
            testFileUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/test_file.gif");
            testUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/test.gif");
            langUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/lang.gif");
            expUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/exp.gif");
            fileUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/file.gif");
            classNameUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/classname.gif");
            selectionUrl = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/sel.gif");
        }
        catch (MalformedURLException ex) {}
        TestFileLabelProvider.testFile = ImageDescriptor.createFromURL(testFileUrl).createImage();
        TestFileLabelProvider.test = ImageDescriptor.createFromURL(testUrl).createImage();
        TestFileLabelProvider.lang = ImageDescriptor.createFromURL(langUrl).createImage();
        TestFileLabelProvider.exp = ImageDescriptor.createFromURL(expUrl).createImage();
        TestFileLabelProvider.file = ImageDescriptor.createFromURL(fileUrl).createImage();
        TestFileLabelProvider.className = ImageDescriptor.createFromURL(classNameUrl).createImage();
        TestFileLabelProvider.selection = ImageDescriptor.createFromURL(selectionUrl).createImage();
    }
    
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
