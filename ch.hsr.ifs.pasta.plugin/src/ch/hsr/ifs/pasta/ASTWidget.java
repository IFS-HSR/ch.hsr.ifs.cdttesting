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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
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
	private int nodeHeight = 20;
	private NodeSelectionListener listener;

	public ASTWidget(final Composite parent) {
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		init();
	}

	private void init() {
		setBackground(getColorWhite());
		canvas = new Canvas(this, SWT.BACKGROUND);
		canvas.setBackground(getColorWhite());

		setContent(canvas);
		setExpandHorizontal(true);
		setExpandVertical(true);

		canvas.addPaintListener(e -> {
			ASTWidget.this.setMinWidth(treeWidth);
			ASTWidget.this.setMinHeight(treeHeight);
			if (root != null) {
				root.visit(node -> {
					if (node.data().getFirst().isVisible()) {
						if (node.parent() != null) {
							drawLineToParent(e, node);
						}
						return NodeVisitor.AfterVisitBehaviour.Continue;
					}
					return NodeVisitor.AfterVisitBehaviour.Abort;
				});
			}
		});
	}

	private Color getColorWhite() {
		return getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}

	public void drawAST(final IASTTranslationUnit ast) {
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

	public void setListener(final NodeSelectionListener listener) {
		this.listener = listener;
	}

	private void clear() {
		root = null;
		for (final Control child : canvas.getChildren()) {
			child.dispose();
		}
	}

	private void drawLineToParent(final PaintEvent e, final Node<?> node) {
		final int parentX = (int) (getXCoord(node.parent()) + ((node.parent().width()) / 2));
		final int parentY = getYCoord(node.parent()) + nodeHeight;
		final int nodeX = (int) (getXCoord(node) + ((node.width()) / 2));
		final int nodeY = getYCoord(node);
		e.gc.drawLine(nodeX, nodeY, parentX, parentY);
		drawArrowHead(e.gc, nodeX, nodeY, parentX, parentY);
	}

	private void drawArrowHead(final GC gc, final double tipX, final double tipY, final double tailX,
			final double tailY) {
		final double phi = Math.toRadians(20);
		final int barb = 10;
		final double dy = tipY - tailY;
		final double dx = tipX - tailX;
		final double theta = Math.atan2(dy, dx);
		double x, y, rho = theta + phi;
		for (int j = 0; j < 2; j++) {
			x = tipX - barb * Math.cos(rho);
			y = tipY - barb * Math.sin(rho);
			gc.drawLine((int) tipX, (int) tipY, (int) x, (int) y);
			rho = theta - phi;
		}
	}

	private void updateNodePositions(final Node<Pair<Button, IASTNode>> node) {
		if (node.parent() != null && !node.parent().data().getFirst().isVisible()) {
			node.data().getFirst().setVisible(false);
		}
		treeWidth = (int) (getXCoord(node) + node.width() > treeWidth ? getXCoord(node) + node.width() : treeWidth);
		treeHeight = (getYCoord(node) > treeHeight + nodeHeight ? getYCoord(node) + nodeHeight : treeHeight);
		node.data().getFirst().setBounds(getXCoord(node), getYCoord(node), (int) (node.width()), nodeHeight);
		for (final Node<Pair<Button, IASTNode>> child : node.getChildren()) {
			updateNodePositions(child);
		}
	}

	private int getYCoord(final Node<?> node) {
		return (int) (node.y() * 60f);
	}

	private int getXCoord(final Node<?> node) {
		return (int) node.x();
	}

	private Node<Pair<Button, IASTNode>> constructTree(final IASTNode astNode, final Composite parent) {
		final Button button = createButton(astNode.getClass().getSimpleName(), parent);
		final Node<Pair<Button, IASTNode>> node = createNode(button, astNode);
		button.addMouseMoveListener(e -> {

			IASTFileLocation fileLocation = astNode.getFileLocation();
			while (fileLocation.getContextInclusionStatement() != null) {
				final IASTPreprocessorIncludeStatement contextInclusionStatement = fileLocation
						.getContextInclusionStatement();
				fileLocation = contextInclusionStatement.getFileLocation();
			}
			final TextSelection textSelection = new TextSelection(fileLocation.getNodeOffset(),
					fileLocation.getNodeLength());
			CUIPlugin.getActivePage().getActiveEditor().getEditorSite().getSelectionProvider()
					.setSelection(textSelection);
		});

		button.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				if (listener != null) {
					listener.nodeSelected(astNode);
				}
			}
		});

		if (astNode.getChildren().length == 0) {
			final Button leafButton = createButton(astNode.getRawSignature(), parent);
			leafButton.setEnabled(false);
			final Node<Pair<Button, IASTNode>> leafNode = createNode(leafButton, astNode);
			node.addChild(leafNode);
		}
		return node;
	}

	private Button createButton(final String text, final Composite parent) {
		final Button button = new Button(parent, SWT.FLAT);
		final FontData fontData = button.getFont().getFontData()[0];
		fontData.setHeight(10);
		final Font font = new Font(parent.getDisplay(), fontData);

		button.setFont(font);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		button.setVisible(false);
		button.pack();
		return button;
	}

	private Node<Pair<Button, IASTNode>> createNode(final Button button, final IASTNode astNode) {
		final Node<Pair<Button, IASTNode>> node = new Node<>(new Pair<>(button, astNode));

		final Point minButtonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		nodeHeight = Math.max(nodeHeight, minButtonSize.y);
		node.setWidth(minButtonSize.x);

		node.treatAsLeaf(true);
		node.data().getFirst().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {

				node.treatAsLeaf(!node.isTreatedAsLeaf());
				if (!node.isTreatedAsLeaf() && node.getChildren().size() == 0) {
					for (final IASTNode child : node.data().getSecond().getChildren()) {
						node.addChild(constructTree(child, canvas));
					}
				}
				for (final Node<Pair<Button, IASTNode>> child : node.getChildren()) {
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
