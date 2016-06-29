package ch.hsr.ifs.pasta;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ch.hsr.ifs.pasta.tree.Node;
import ch.hsr.ifs.pasta.tree.NodeVisitor;


public class ASTWidget extends ScrolledComposite {

    private Canvas canvas;
    private Node<Pair<Button, IASTNode>> root;
    private int treeHeight;
    private int treeWidth;
    private final int NODE_HEIGHT = 20;
    private NodeSelectionListener listener;

    public ASTWidget(Composite parent) {
        super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        init();
    }

    private void init() {
        this.setBackground(getColorWhite());
        canvas = new Canvas(this, SWT.BACKGROUND);
        canvas.setBackground(getColorWhite());

        this.setContent(canvas);
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);

        canvas.addPaintListener(new PaintListener() {

            @Override
            public void paintControl(final PaintEvent e) {
                ASTWidget.this.setMinWidth(treeWidth);
                ASTWidget.this.setMinHeight(treeHeight);
                if (root != null) {
                    root.visit(new NodeVisitor<Pair<Button, IASTNode>>() {
                        @Override
                        public AfterVisitBehaviour visit(Node<Pair<Button, IASTNode>> node) {
                            if (node.data().getFirst().isVisible()) {
                                if (node.parent() != null) {
                                    drawLineToParent(e, node); 
                                }
                                return AfterVisitBehaviour.Continue;
                            }
                            return AfterVisitBehaviour.Abort;
                        }
                    });
                }
            }
        });
    }

    private Color getColorWhite() {
        return getDisplay().getSystemColor(SWT.COLOR_WHITE);
    }

    public void drawAST(IASTTranslationUnit ast) {
        clear();
        root = constructTree(ast, canvas);
        root.adjust(1f, 20f);
        setOrigin(0, 0);
        treeWidth = 0;
        treeHeight = 0;
        updateNodePositions(root);
        root.data().getFirst().setVisible(true);
        canvas.redraw();
        canvas.update();
    }
    
    public void setListener(NodeSelectionListener listener) {
        this.listener = listener;
    }

    private void clear() {
    	root = null;
        for (Control child : canvas.getChildren()) {
            child.dispose();
        }
    }

    private void drawLineToParent(PaintEvent e, Node<?> node) {
        int parentX = (int) (getXCoord(node.parent()) + ((node.parent().width()) / 2));
        int parentY = getYCoord(node.parent()) + NODE_HEIGHT;
        int nodeX = (int) (getXCoord(node) + ((node.width()) / 2));
        int nodeY = getYCoord(node);
        e.gc.drawLine(nodeX, nodeY, parentX, parentY);
        drawArrowHead(e.gc, nodeX, nodeY, parentX, parentY);
    }
    
    private void drawArrowHead(GC gc, double tipX, double tipY, double tailX, double tailY)  
    {  
        double phi = Math.toRadians(20);  
        int barb = 10;  
        double dy = tipY - tailY;  
        double dx = tipX - tailX;  
        double theta = Math.atan2(dy, dx);   
        double x, y, rho = theta + phi;  
        for(int j = 0; j < 2; j++)  
        {  
            x = tipX - barb * Math.cos(rho);  
            y = tipY - barb * Math.sin(rho);  
            gc.drawLine((int)tipX, (int)tipY,(int) x,(int) y);  
            rho = theta - phi;  
        }  
    }  

    private void updateNodePositions(final Node<Pair<Button, IASTNode>> node) {
        if (node.parent() != null && !node.parent().data().getFirst().isVisible()) {
            node.data().getFirst().setVisible(false);
        }
        treeWidth = (int) (getXCoord(node) + node.width() > treeWidth ? getXCoord(node) + node.width() : treeWidth);
        treeHeight = (getYCoord(node) > treeHeight + NODE_HEIGHT ? getYCoord(node) + NODE_HEIGHT : treeHeight);
        node.data().getFirst().setBounds(getXCoord(node), getYCoord(node), (int) (node.width()), NODE_HEIGHT);
        for (Node<Pair<Button, IASTNode>> child : node.getChildren()) {
            updateNodePositions(child);
        }
    }

    private int getYCoord(Node<?> node) {
        return (int) (node.y() * 60f);
    }

    private int getXCoord(Node<?> node) {
        return (int) node.x();
    }

    private Node<Pair<Button, IASTNode>> constructTree(final IASTNode astNode, Composite parent) {
        Button button = createButton(astNode.getClass().getSimpleName(), parent);
        final Node<Pair<Button, IASTNode>> node = createNode(button, astNode);
        button.addMouseMoveListener(new MouseMoveListener() {

            @Override
            public void mouseMove(MouseEvent e) {
            	
            	IASTFileLocation fileLocation = astNode.getFileLocation();
            	while (fileLocation.getContextInclusionStatement() != null) {
            		IASTPreprocessorIncludeStatement contextInclusionStatement = fileLocation.getContextInclusionStatement();
            		fileLocation = contextInclusionStatement.getFileLocation();
               	}
            	TextSelection textSelection = new TextSelection(fileLocation.getNodeOffset(), fileLocation.getNodeLength());
                CUIPlugin.getActivePage().getActiveEditor().getEditorSite().getSelectionProvider().setSelection(textSelection);
            }
        });
        
        button.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseDown(MouseEvent e) {
                if (listener != null) {
                    listener.nodeSelected(astNode);
                }
            }
        });
        
        if (astNode.getChildren().length == 0) {
            Button leafButton = createButton(astNode.getRawSignature(), parent);
            leafButton.setEnabled(false);
            final Node<Pair<Button, IASTNode>> leafNode = createNode(leafButton, astNode);
            node.addChild(leafNode);
        }
        return node;
    }

    private Button createButton(String text, Composite parent) {
        final Button button = new Button(parent, SWT.FLAT);
        button.setText(text);
        FontData fontData = button.getFont().getFontData()[0];
        fontData.setHeight(10);
        button.setFont(new Font(parent.getDisplay(), fontData));
        button.setVisible(false);
        button.pack();
        return button;
    }

    private Node<Pair<Button, IASTNode>> createNode(Button button, IASTNode astNode) {
        final Node<Pair<Button, IASTNode>> node = new Node<>(new Pair<>(button, astNode));
        node.setWidth(button.getBounds().width);
        node.treatAsLeaf(true);
        node.data().getFirst().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                
                node.treatAsLeaf(!node.isTreatedAsLeaf());
                if (!node.isTreatedAsLeaf() && node.getChildren().size() == 0) {
                	for (IASTNode child : node.data().getSecond().getChildren()) {
                        node.addChild(constructTree(child, canvas));
                    }
                }
                for (Node<Pair<Button, IASTNode>> child : node.getChildren()) {
                    if (!node.isTreatedAsLeaf()) {
                        child.treatAsLeaf(true);
                    }
                    child.data().getFirst().setVisible(!node.isTreatedAsLeaf());
                }
                treeWidth = 0;
                treeHeight = 0;
                if (!node.isTreatedAsLeaf()) {
                    root.adjust(1f, 20f);
                }
                updateNodePositions(root);
                canvas.redraw();
                canvas.update();
            }
        });
        return node;
    }
}
