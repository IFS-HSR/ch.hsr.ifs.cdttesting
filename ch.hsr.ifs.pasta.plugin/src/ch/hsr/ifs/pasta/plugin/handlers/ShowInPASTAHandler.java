package ch.hsr.ifs.pasta.plugin.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import ch.hsr.ifs.pasta.ASTView;
import ch.hsr.ifs.pasta.events.PastaEventConstants;


public class ShowInPASTAHandler extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final ISelection selection = getSelection();
      if (selection instanceof ITextSelection) {
         sendEvent(selection);
      }
      return null;

   }

   private ISelection getSelection() {
      return CUIPlugin.getActivePage().getActiveEditor().getEditorSite().getSelectionProvider().getSelection();
   }

   private void sendEvent(final Object data) {
      final Event selectionEvent = new Event(PastaEventConstants.SHOW_SELECTION, createMap(data));
      final BundleContext ctx = FrameworkUtil.getBundle(ASTView.class).getBundleContext();
      final ServiceReference<?> ref = ctx.getServiceReference(EventAdmin.class.getName());
      if (ref != null) {
         final EventAdmin admin = (EventAdmin) ctx.getService(ref);
         admin.sendEvent(selectionEvent);
         ctx.ungetService(ref);
      }
   }

   private Map<String, Object> createMap(final Object data) {
      final Map<String, Object> map = new HashMap<>();
      map.put(PastaEventConstants.SELECTION, data);
      return map;
   }

}
