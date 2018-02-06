package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;


public interface Node {

   Position getPosition();

   boolean hasChildren();

   Node[] getChildren();

   Object getParent();
}
