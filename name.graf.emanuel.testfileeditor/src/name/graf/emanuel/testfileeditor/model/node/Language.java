package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;


public class Language implements Node {

   private String   lang;
   private Position pos;
   private Test     test;

   public Language(final String lang, final Position pos, final Test test) {
      super();
      this.lang = lang;
      this.pos = pos;
      this.test = test;
   }

   public Node[] getChildren() {
      return null;
   }

   public Node getParent() {
      return this.test;
   }

   public Position getPosition() {
      return this.pos;
   }

   public boolean hasChildren() {
      return false;
   }

   public String toString() {
      return this.lang;
   }

   public boolean equals(final Object obj) {
      return this.hashCode() == obj.hashCode();
   }
}
