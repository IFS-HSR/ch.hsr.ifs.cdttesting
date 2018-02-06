package ch.hsr.ifs.pasta;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class PastaPlugin extends AbstractUIPlugin {

   // The shared instance
   private static PastaPlugin plugin;

   // The plug-in ID
   public static String PLUGIN_ID;

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
    * BundleContext )
    */
   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      plugin = this;
      PLUGIN_ID = getBundle().getSymbolicName();
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
    * BundleContext )
    */
   @Override
   public void stop(final BundleContext context) throws Exception {
      plugin = null;
      super.stop(context);
   }

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static PastaPlugin getDefault() {
      return plugin;
   }

   /**
    * Logs the specified status with this plug-in's log.
    *
    * @param status
    *        status to log
    */
   public static void log(final IStatus status) {
      getDefault().getLog().log(status);
   }

   /**
    * Logs an internal error with the specified throwable
    *
    * @param e
    *        the exception to be logged
    */
   public static void log(final Throwable e) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, 1, "Internal Error", e));
   }

   /**
    * Logs an error with the specified throwable and message
    *
    * @param e
    *        the exception to be logged
    * @param message
    *        additional message
    */
   public static void log(final Throwable e, final String message) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, e));
   }

   /**
    * Logs an internal error with the specified message.
    *
    * @param message
    *        the error message to log
    */
   public static void log(final String message) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, null));
   }
}
