package ch.hsr.ifs.cdttesting.showoffset;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class ShowOffsetPlugin extends AbstractUIPlugin {

   public static final String PLUGIN_ID = "ch.hsr.ifs.cdttesting.showoffset";

   private static ShowOffsetPlugin plugin;

   public ShowOffsetPlugin() {
      plugin = this;
   }

   @Override
   public void start(BundleContext context) throws Exception {
      super.start(context);
   }

   @Override
   public void stop(BundleContext context) throws Exception {
      plugin = null;
      super.stop(context);
   }

   public static ShowOffsetPlugin getDefault() {
      return plugin;
   }

   public static ImageDescriptor getImageDescriptor(String path) {
      return imageDescriptorFromPlugin(PLUGIN_ID, path);
   }
}
