package name.graf.emanuel.testfileeditor;

import org.eclipse.ui.plugin.*;
import name.graf.emanuel.testfileeditor.editors.*;
import org.osgi.framework.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.runtime.*;

public class Activator extends AbstractUIPlugin
{
    public static final String PLUGIN_ID = "name.graf.emanue.cdttestfileeditor";
    public static final String TEST_FILE_PARTITIONING = "__test_file_partitioning";
    private static Activator plugin;
    private TestFilePartitionScanner scanner;
    
    public Activator() {
        super();
        Activator.plugin = this;
    }
    
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }
    
    public void stop(final BundleContext context) throws Exception {
        Activator.plugin = null;
        super.stop(context);
    }
    
    public static Activator getDefault() {
        return Activator.plugin;
    }
    
    public static Display getStandardDisplay() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }
    
    public TestFilePartitionScanner getTestFilePartitionScanner() {
        if (this.scanner == null) {
            this.scanner = new TestFilePartitionScanner();
        }
        return this.scanner;
    }
    
    public static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin("name.graf.emanue.cdttestfileeditor", path);
    }
    
    public static void log(final IStatus status) {
        getDefault().getLog().log(status);
    }
    
    public static void logError(final Throwable t, final int code) {
        log((IStatus)new Status(4, "name.graf.emanue.cdttestfileeditor", code, t.getMessage(), t));
    }
}
