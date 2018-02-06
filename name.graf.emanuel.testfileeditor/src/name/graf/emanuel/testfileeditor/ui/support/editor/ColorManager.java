package name.graf.emanuel.testfileeditor.ui.support.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;


public class ColorManager {

   protected Map<RGB, Color> fColorTable;

   public ColorManager() {
      super();
      this.fColorTable = new HashMap<RGB, Color>(10);
   }

   public void dispose() {
      final Iterator<Color> e = this.fColorTable.values().iterator();
      while (e.hasNext()) {
         e.next().dispose();
      }
   }

   public Color getColor(final RGB rgb) {
      Color color = this.fColorTable.get(rgb);
      if (color == null) {
         color = new Color(Display.getCurrent(), rgb);
         this.fColorTable.put(rgb, color);
      }
      return color;
   }
}
