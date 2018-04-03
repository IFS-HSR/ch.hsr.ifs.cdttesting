package name.graf.emanuel.testfileeditor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

import name.graf.emanuel.testfileeditor.ui.support.editor.PartitionScanner;


public class Activator extends AbstractUIPlugin {

   public static final String    PLUGIN_ID              = "name.graf.emanuel.testfileeditor";
   public static final String    TEST_FILE_PARTITIONING = "__test_file_partitioning";
   private static Activator      plugin;
   private PartitionScanner      scanner;
   private ScopedPreferenceStore preferenceStore;

   public Activator() {
      super();
      Activator.plugin = this;
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
   }

   @Override
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

   public PartitionScanner getTestFilePartitionScanner() {
      if (this.scanner == null) {
         this.scanner = new PartitionScanner();
      }
      return this.scanner;
   }

   public static ImageDescriptor getImageDescriptor(final String path) {
      return imageDescriptorFromPlugin(PLUGIN_ID, path);
   }

   public static void log(final IStatus status) {
      getDefault().getLog().log(status);
   }

   public static void logError(final Throwable t, final int code) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, code, t.getMessage(), t));
   }

   @Override
   public IPreferenceStore getPreferenceStore() {
      if (preferenceStore == null) {
         preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
      }
      return preferenceStore;
   }
}
