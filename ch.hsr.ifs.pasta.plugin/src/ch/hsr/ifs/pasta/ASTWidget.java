package ch.hsr.ifs.pasta;

import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import ch.hsr.ifs.pasta.plugin.preferences.PreferenceConstants;
import ch.hsr.ifs.pasta.tree.Node;
import ch.hsr.ifs.pasta.tree.NodeVisitor;

public class ASTWidget extends ScrolledComposite {

	private static final Color GOLDEN_YELLOW = new Color(Display.getCurrent(), 255, 168, 0);
	private static final Color WHITE = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

	private static final int DEFAULT_NODE_HEIGHT = 20;
	private static final float SIBLING_DISTANCE = 4;
	private static final float BRANCH_DISTANCE = 15;
	private static final int GAP_SIZE = 20;

	private final ASTScrolledCanvas canvas;
	private int nodeHeight = DEFAULT_NODE_HEIGHT;


	private NodeSelectionListener listener;
	private Node<Pair<Button, IASTNode>> root;
	private Node<Pair<Button, IASTNode>> lastControl = null;

	private final IPreferenceStore prefStore;

	public ASTWidget(final Composite parent) {
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

		prefStore = PastaPlugin.getDefault().getPreferenceStore();
		canvas = new ASTScrolledCanvas(this, SWT.BACKGROUND);

		setupScrolledComposite(parent);
		setupCanvas();
	}

	void showSelectedNode(final IASTNode sParent) {

		IASTNode parent = sParent;

		if (!root.isTreatedAsLeaf()) {
			buildChildrenAndRefresh(root);
		}

		final LinkedList<IASTNode> nodeList = new LinkedList<>();
		while (parent.getParent() != null) {
			nodeList.add(parent);
			parent = parent.getParent();
		}

		IASTNode listNode;
		Node<Pair<Button, IASTNode>> currentNode = root;
		while (!nodeList.isEmpty()) {
			listNode = nodeList.removeLast();
			buildChildrenAndRefresh(currentNode);
			for (final Node<Pair<Button, IASTNode>> child : currentNode.getChildren()) {
				if (child.data().getSecond().getRawSignature().equals(listNode.getRawSignature())) {
					currentNode = child;
					break;
				}
			}
		}
		buildChildrenAndRefresh(currentNode);
	}

	private void setupScrolledComposite(final Composite parent) {
		setAlwaysShowScrollBars(true);
		setBackground(WHITE);
		setContent(canvas);
		setExpandHorizontal(true);
		setExpandVertical(true);
	}

	private void setupCanvas() {
		resizeCanvas();
		canvas.setBackground(WHITE);
		canvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(final PaintEvent e) {
				if (lastControl != null) {
					adjustView(lastControl);
				}
				if (root != null) {
					root.visit(node -> {
						if (node.data().getFirst().isVisible()) {
							if (node.parent() != null) {
								drawArrowFromParent(e.gc, node);
							}
							return NodeVisitor.AfterVisitBehaviour.Continue;
						}
						return NodeVisitor.AfterVisitBehaviour.Abort;
					});
				}
			}

		});

	}

	protected void resizeCanvas() {
		final Point widgetSize = getSize();
		final Point computedSize = canvas.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		final Point newSize = new Point(Math.max(computedSize.x, widgetSize.x), Math.max(computedSize.y, widgetSize.y));
		setMinSize(newSize);
		canvas.setSize(newSize);
	}

	public void drawAST(final IASTTranslationUnit ast) {
		clear();
		root = constructTree(ast, canvas);
		root.adjust(ASTWidget.SIBLING_DISTANCE, ASTWidget.BRANCH_DISTANCE);
		setOrigin(0, 0);
		updateNodePositions(root);
		resizeCanvas();
		final Button rootButton = root.data().getFirst();
		rootButton.setLocation(new Point(getSize().x / 2 - rootButton.getBounds().x / 2, 0));
		rootButton.setVisible(true);
		refresh();
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

	private void drawArrowFromParent(final GC gc, final Node<?> node) {
		final int parentX = (int) (getXCoord(node.parent()) + ((node.parent().width()) / 2));
		final int parentY = getYCoord(node.parent()) + nodeHeight;
		final int nodeX = (int) (getXCoord(node) + ((node.width()) / 2));
		final int nodeY = getYCoord(node);
		drawArrow(gc, parentX, parentY, nodeX, nodeY);
	}

	public static void drawArrow(final GC gc, final int x1, final int y1, int x2, final int y2) {
		final double arrowAngle = Math.toRadians(30.0);
		final double arrowLength = 8.0;
		double theta = Math.atan2(y2 - y1, x2 - x1);

		/* Arrow line */
		if (Math.abs(x1 - x2) > GAP_SIZE) {
			final int centerY = (y1 + y2) / 2;
			if (x1 > x2) {
				gc.drawArc(x1 - 30, y1 - GAP_SIZE/2, 30, GAP_SIZE, 0, -90);
				gc.drawLine(x1 - 15, centerY, x2 + 15, centerY);
				gc.drawArc(x2, y1 + GAP_SIZE/2, 30, GAP_SIZE, 180, -90);
				theta = Math.toRadians(124);
				x2--;
			} else {
				gc.drawArc(x1, y1 - GAP_SIZE/2, 30, GAP_SIZE, 180, 90);
				gc.drawLine(x1 + 15, centerY, x2 - 15, centerY);
				gc.drawArc(x2 - 30, y1 + GAP_SIZE/2, 30, GAP_SIZE, 90, -90);
				theta = Math.toRadians(60);
				x2++;
			}
		} else {
			gc.drawLine(x1, y1, x2, y2);
		}
		/* Arrow head */
		gc.drawLine((int) (x2 - arrowLength * Math.cos(theta - arrowAngle)),
				(int) (y2 - arrowLength * Math.sin(theta - arrowAngle)), x2, y2);
		gc.drawLine((int) (x2 - arrowLength * Math.cos(theta + arrowAngle)),
				(int) (y2 - arrowLength * Math.sin(theta + arrowAngle)), x2, y2);
	}

	private void updateNodePositions(final Node<Pair<Button, IASTNode>> node) {
		if (node.parent() != null && !node.parent().data().getFirst().isVisible()) {
			node.data().getFirst().setVisible(false);
		}
		node.data().getFirst().setBounds(getXCoord(node), getYCoord(node), (int) (node.width()), nodeHeight);
		for (final Node<Pair<Button, IASTNode>> child : node.getChildren()) {
			updateNodePositions(child);
		}
	}

	private int getYCoord(final Node<?> node) {
		return (int) (node.y() * (nodeHeight + GAP_SIZE));
	}

	private int getXCoord(final Node<?> node) {
		return (int) node.x();
	}

	private void refresh() {
		canvas.redraw();
		canvas.update();
	}

	private void adjustView(final Node<Pair<Button, IASTNode>> node) {

		final Rectangle buttonBounds = node.data().getFirst().getBounds();

		final int leftmostIndex;
		final int bottommostIndex;
		if (node.leftMostChild() == null) {
			leftmostIndex = buttonBounds.x;
			bottommostIndex = buttonBounds.y + buttonBounds.height;
		} else {
			final Rectangle bounds = node.leftMostChild().data().getFirst().getBounds();
			leftmostIndex = Math.min(bounds.x, buttonBounds.x);
			bottommostIndex = bounds.y + bounds.height;
		}

		final int rightmostIndex;
		if (node.rightMostChild() == null) {
			rightmostIndex = buttonBounds.x + buttonBounds.width;
		} else {
			final Rectangle bounds = node.rightMostChild().data().getFirst().getBounds();
			rightmostIndex = Math.max(bounds.x + bounds.width, buttonBounds.x + buttonBounds.width);
		}

		int correctedX = getOrigin().x;
		int correctedY = getOrigin().y;
		lastControl = null;
		if (!(leftmostIndex > getOrigin().x && rightmostIndex < getOrigin().x + getBounds().width)) {
			correctedX = buttonBounds.x - (getBounds().width / 2 - buttonBounds.width / 2);
		}
		if (!(buttonBounds.y > getOrigin().y && bottommostIndex < getOrigin().y + getBounds().height)) {
			correctedY = buttonBounds.y - (getBounds().height / 2 - buttonBounds.height / 2);
		}
		this.setOrigin(correctedX, correctedY);
	}

	private Node<Pair<Button, IASTNode>> constructTree(final IASTNode astNode, final Composite parent) {
		final Button button = createButton(astNode.getClass().getSimpleName(), parent);
		final Node<Pair<Button, IASTNode>> node = createNode(button, astNode);

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
		button.getFont().getFontData()[0].setHeight(10);
		button.setText(text.replaceAll("\\{[\\S\\s]*\\}", "{ ... }"));
		button.setVisible(false);
		button.pack();
		button.setCursor(new Cursor(getDisplay(), SWT.CURSOR_ARROW));
		return button;
	}

	private Node<Pair<Button, IASTNode>> createNode(final Button button, final IASTNode astNode) {
		final Node<Pair<Button, IASTNode>> node = new Node<>(new Pair<>(button, astNode));

		final Point minButtonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		nodeHeight = Math.max(nodeHeight, minButtonSize.y);
		node.setWidth(minButtonSize.x);
		node.treatAsLeaf(true);

		if (astNode instanceof ICPPASTTranslationUnit) {
			button.setBackground(GOLDEN_YELLOW);
			button.getFont().getFontData()[0].setStyle(SWT.BOLD);
		}

		button.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				int selectKey = 0;

				if (prefStore.getString(PreferenceConstants.P_HOW_TO_SELECT)
						.equals(PreferenceConstants.P_SELECT_BY_RIGHT_CLICK)) {
					selectKey = 3;
				} else if (prefStore.getString(PreferenceConstants.P_HOW_TO_SELECT)
						.equals(PreferenceConstants.P_SELECT_BY_LEFT_CLICK)) {
					selectKey = 1;
				}

				if (e.button == selectKey) {
					setNodeInNodeView(astNode);
				}

				if (e.button == 1) {
					buildChildrenAndRefresh(node);
				}
			}

		});

		button.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseHover(final MouseEvent e) {
				if (prefStore.getString(PreferenceConstants.P_HOW_TO_SELECT)
						.equals(PreferenceConstants.P_SELECT_BY_MOUSE_OVER)) {
					setNodeInNodeView(astNode);
				}
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
			}

			@Override
			public void mouseExit(final MouseEvent e) {
			}

			@Override
			public void mouseEnter(final MouseEvent e) {
			}

		});

		return node;
	}

	private void setNodeInNodeView(final IASTNode astNode) {
		if (listener != null) {
			listener.nodeSelected(astNode);
		}
	}

	private void buildChildrenAndRefresh(final Node<Pair<Button, IASTNode>> node) {
		lastControl = node;
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
		if (!node.isTreatedAsLeaf()) {
			root.adjust(ASTWidget.SIBLING_DISTANCE, ASTWidget.BRANCH_DISTANCE);
		}
		updateNodePositions(root);
		resizeCanvas();
		refresh();
	}

}
