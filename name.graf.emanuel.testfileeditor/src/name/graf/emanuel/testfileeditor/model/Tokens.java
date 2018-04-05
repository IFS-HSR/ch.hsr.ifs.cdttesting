package name.graf.emanuel.testfileeditor.model;

public interface Tokens {

   public static final int TOKENLENGTH = 3;

   public static final String CLASS           = "//#";
   public static final String COMMENT_OPEN    = "/*";
   public static final String COMMENT_CLOSE   = "*/";
   public static final String EXPECTED        = "//=";
   public static final String FILE            = "//@";
   public static final String LANGUAGE        = "//%";
   public static final String TEST            = "//!";
   public static final String SELECTION_OPEN  = "/*$";
   public static final String SELECTION_CLOSE = "$*/";

}
