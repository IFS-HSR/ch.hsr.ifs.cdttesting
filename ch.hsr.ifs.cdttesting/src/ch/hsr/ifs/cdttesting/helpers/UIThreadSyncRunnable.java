package ch.hsr.ifs.cdttesting.helpers;

import org.eclipse.ui.PlatformUI;

import ch.hsr.ifs.iltis.core.exception.ILTISException;


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

   private void throwIfHasException() throws RuntimeException {
      if (e != null) ILTISException.wrap(e).rethrowUnchecked();
   }

   final public void runSyncOnUIThread() throws RuntimeException {
      PlatformUI.getWorkbench().getDisplay().syncExec(this);
      throwIfHasException();
   }
}
