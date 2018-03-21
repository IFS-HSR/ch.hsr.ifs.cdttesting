package ch.hsr.ifs.cdttesting.cdttest.comparison;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.IToken;

import ch.hsr.ifs.iltis.core.exception.ILTISException;


/**
 * This is a marker node to mark the end of an AST
 * 
 * @author tstauber
 *
 */
public class EOTNode implements IASTNode {

   private IASTTranslationUnit ast;

   protected EOTNode(IASTTranslationUnit ast) {
      this.ast = ast;
   }

   private ILTISException makeMarkerException() {
      return new ILTISException("This node type does not provide any functionality. It is a pure marker node!");

   }

   @Override
   public IASTTranslationUnit getTranslationUnit() {
      return this.ast;
   }

   @Override
   public IASTNodeLocation[] getNodeLocations() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTFileLocation getFileLocation() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public String getContainingFilename() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public boolean isPartOfTranslationUnitFile() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTNode getParent() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTNode[] getChildren() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public void setParent(IASTNode node) {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public ASTNodeProperty getPropertyInParent() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public void setPropertyInParent(ASTNodeProperty property) {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public boolean accept(ASTVisitor visitor) {
      return false;
   }

   @Override
   public String getRawSignature() {
      return "-- END OF TREE --";
   }

   @Override
   public boolean contains(IASTNode node) {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public boolean isFrozen() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public boolean isActive() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTNode copy() {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTNode copy(CopyStyle style) {
      throw makeMarkerException().rethrowUnchecked();
   }

   @Override
   public IASTNode getOriginalNode() {
      throw makeMarkerException().rethrowUnchecked();
   }

}
