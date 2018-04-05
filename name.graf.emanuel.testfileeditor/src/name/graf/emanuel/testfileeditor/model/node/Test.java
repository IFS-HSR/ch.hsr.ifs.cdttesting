package name.graf.emanuel.testfileeditor.model.node;

import java.util.Vector;

import org.eclipse.jface.text.Position;

import name.graf.emanuel.testfileeditor.model.TestFile;


public class Test implements Node {

   private final String       name;
   private final Position     pos;
   private final Position     head;
   private final TestFile     parent;
   private Language           lang;
   private Expected           exp;
   private Class              className;
   private final Vector<File> fileDefs;

   public Test(final String name, final Position pos, final Position head, final TestFile file) {
      super();
      lang = null;
      exp = null;
      className = null;
      fileDefs = new Vector<>();
      this.name = name;
      this.pos = pos;
      this.head = head;
      parent = file;
   }

   @Override
   public Position getPosition() {
      return pos;
   }

   public Position getHeadPosition() {
      return head;
   }

   public boolean containsOffset(final int offset) {
      return pos.overlapsWith(offset, 1);
   }

   public boolean containsFile(final String name) {
      return getFile(name) != null;
   }

   public File getFile(final String name) {
      for (final File file : fileDefs) {
         if (file.toString().equals(name)) { return file; }
      }
      return null;
   }

   public String getName() {
      return name;
   }

   @Override
   public String toString() {
      return name;
   }

   @Override
   public int hashCode() {
      return name.hashCode();
   }

   @Override
   public Node[] getChildren() {
      final int length = howManyChildren();
      final Node[] children = new Node[length];
      int index = 0;
      if (className != null) {
         children[index++] = className;
      }
      if (lang != null) {
         children[index++] = lang;
      }
      if (exp != null) {
         children[index++] = exp;
      }
      for (final Node node : fileDefs) {
         children[index++] = node;
      }
      return children;
   }

   private int howManyChildren() {
      int length = 0;
      if (lang != null) {
         ++length;
      }
      if (exp != null) {
         ++length;
      }
      if (className != null) {
         ++length;
      }
      length += fileDefs.size();
      return length;
   }

   @Override
   public Object getParent() {
      return parent;
   }

   @Override
   public boolean hasChildren() {
      return howManyChildren() > 0;
   }

   public void setLang(final Language lang) {
      this.lang = lang;
   }

   public void setExpected(final Expected exp) {
      this.exp = exp;
   }

   public void setClassname(final Class className) {
      this.className = className;
   }

   public void addFile(final File file) {
      fileDefs.add(file);
   }

   @Override
   public boolean equals(final Object obj) {
      return hashCode() == obj.hashCode();
   }
}
