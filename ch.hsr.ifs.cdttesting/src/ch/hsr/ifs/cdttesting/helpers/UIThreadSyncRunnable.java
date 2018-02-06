package ch.hsr.ifs.cdttesting.helpers;

import org.eclipse.ui.PlatformUI;


public abstract class UIThreadSyncRunnable implements Runnable {

   volatile private Exception e;

   protected abstract void runSave() throws Exception;

   @Override
   final public void run() {
      try {
         runSave();
      } catch (Exception e) {
         this.e = e;
      }
   }

   private void throwIfHasException() throws Exception {
      if (e != null) { throw e; }
   }

   final public void runSyncOnUIThread() throws Exception {
      PlatformUI.getWorkbench().getDisplay().syncExec(this);
      throwIfHasException();
   }
}
