package ch.hsr.ifs.pasta.tree;

public interface NodeVisitor<T> {

   public enum AfterVisitBehaviour {
      Continue, Abort

   }

   AfterVisitBehaviour visit(Node<T> node);

}
