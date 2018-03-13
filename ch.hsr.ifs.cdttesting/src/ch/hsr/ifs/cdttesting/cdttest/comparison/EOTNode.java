package ch.hsr.ifs.cdttesting.cdttest.comparison;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.IToken;

/**
 * This is a marker node to mark the end of an AST
 * @author tstauber
 *
 */
public class EOTNode implements IASTNode {

   @Override
   public IASTTranslationUnit getTranslationUnit() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IASTNodeLocation[] getNodeLocations() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IASTFileLocation getFileLocation() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getContainingFilename() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isPartOfTranslationUnitFile() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public IASTNode getParent() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IASTNode[] getChildren() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setParent(IASTNode node) {
      // TODO Auto-generated method stub

   }

   @Override
   public ASTNodeProperty getPropertyInParent() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setPropertyInParent(ASTNodeProperty property) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean accept(ASTVisitor visitor) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String getRawSignature() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean contains(IASTNode node) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isFrozen() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isActive() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public IASTNode copy() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IASTNode copy(CopyStyle style) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public IASTNode getOriginalNode() {
      // TODO Auto-generated method stub
      return null;
   }

}
