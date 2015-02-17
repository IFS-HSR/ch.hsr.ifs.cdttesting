package ch.hsr.ifs.pasta;

import org.eclipse.cdt.core.dom.ast.IASTNode;

public interface NodeSelectionListener {
    
    void nodeSelected(IASTNode node);
}
