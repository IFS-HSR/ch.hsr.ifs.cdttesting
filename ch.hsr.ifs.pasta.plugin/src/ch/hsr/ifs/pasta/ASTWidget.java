package ch.hsr.ifs.pasta;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeSelector;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ch.hsr.ifs.pasta.events.PastaEventConstants;
import ch.hsr.ifs.pasta.plugin.preferences.PreferenceConstants;
import ch.hsr.ifs.pasta.tree.Node;
import ch.hsr.ifs.pasta.tree.NodeVisitor;

public class ASTWidget extends ScrolledComposite {

	private static final int DEFAULT_NODE_HEIGHT = 20;
	private static final float SIBLING_DISTANCE = 4;
	private static final float BRANCH_DISTANCE = 15;
	private static final int CURSOR_SIZE = 38;
	private static final int GAP_SIZE = 20;

	private final Canvas canvas;
	private int treeHeight;
	private int treeWidth;
	private int nodeHeight = DEFAULT_NODE_HEIGHT;
	private Point dragSource = null;
	private boolean dragFlag = false;

	private NodeSelectionListener listener;
	private Node<Pair<Button, IASTNode>> root;
	private Node<Pair<Button, IASTNode>> lastControl = null;

	private final IPreferenceStore prefStore;
	private IASTTranslationUnit localASTCopy;

	public ASTWidget(final Composite parent) {
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);

		prefStore = PastaPlugin.getDefault().getPreferenceStore();
		canvas = new Canvas(this, SWT.BACKGROUND);

		setupScrolledComposite(parent);

		setupCanvas();

		registerEventHandler(PastaEventConstants.SHOW_SELECTION, new EventHandler() {

			@Override
			public void handleEvent(final Event event) {

				final ISelection selection = (ISelection) event.getProperty(PastaEventConstants.SELECTION);
				if (selection instanceof ITextSelection) {
					showSelectedNode((ITextSelection) selection);
				}

			}

			private void showSelectedNode(final ITextSelection selection) {

				if (!root.isTreatedAsLeaf()) {
					buildChildrenAndRefresh(root);
				}

				final IASTNodeSelector selector = localASTCopy.getNodeSelector(localASTCopy.getFilePath());
				final LinkedList<IASTNode> nodeList = new LinkedList<>();
				IASTNode parent = selector.findEnclosingNode(selection.getOffset(), selection.getLength());
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
						if (child.data().getSecond().equals(listNode)) {
							currentNode = child;
							break;
						}
					}
				}
				buildChildrenAndRefresh(currentNode);

			}
		});
	}

	private void setupScrolledComposite(final Composite parent) {
		setAlwaysShowScrollBars(true);
		parent.setBackground(getColorWhite());
		setBackground(getColorWhite());
		setContent(canvas);
		setExpandHorizontal(true);
		setExpandVertical(true);
	}

	private void setupCanvas() {
		final ImageData grabImage = new ImageData(ASTWidget.class.getResourceAsStream("/icons/closedhand.gif"));
		final Cursor grabCursor = new Cursor(getDisplay(),
				grabImage.scaledTo(ASTWidget.CURSOR_SIZE, ASTWidget.CURSOR_SIZE), ASTWidget.CURSOR_SIZE / 2,
				ASTWidget.CURSOR_SIZE / 4);

		final ImageData openImage = new ImageData(ASTWidget.class.getResourceAsStream("/icons/openhand.gif"));
		final Cursor openCursor = new Cursor(getDisplay(),
				openImage.scaledTo(ASTWidget.CURSOR_SIZE, ASTWidget.CURSOR_SIZE), ASTWidget.CURSOR_SIZE / 2,
				ASTWidget.CURSOR_SIZE / 4);

		canvas.setCursor(openCursor);
		canvas.setBackground(getColorWhite());
		canvas.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				dragSource = new Point(e.x, e.y);
				canvas.setCursor(grabCursor);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				dragFlag = false;
				dragSource = null;
				canvas.setCursor(openCursor);
			}

		});

		canvas.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(final MouseEvent e) {
				if (dragFlag) {
					ASTWidget.this.setOrigin(ASTWidget.this.getOrigin().x + (dragSource.x - e.x),
							ASTWidget.this.getOrigin().y + (dragSource.y - e.y));
				}
			}

		});

		canvas.addDragDetectListener(new DragDetectListener() {

			@Override
			public void dragDetected(final DragDetectEvent e) {
				dragFlag = true;
			}

		});

		canvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(final PaintEvent e) {
				ASTWidget.this.setMinWidth(treeWidth);
				ASTWidget.this.setMinHeight(treeHeight);
				if (lastControl != null) {
					adjustView(lastControl);
				}
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
			}

		});

	}

	private Color getColorWhite() {
		return Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	}

	private void setMinTreeSize() {
		treeWidth = getBounds().width;
		treeHeight = getBounds().height;
	}

	public void drawAST(@NonNull final IASTTranslationUnit ast) {
		clear();
		localASTCopy = ast;
		root = constructTree(localASTCopy, canvas);
		root.adjust(ASTWidget.SIBLING_DISTANCE, ASTWidget.BRANCH_DISTANCE);
		setOrigin(0, 0);
		setMinTreeSize();
		updateNodePositions(root);
		root.data().getFirst().setVisible(true);
		refresh();
	}

	public void setListener(@NonNull final NodeSelectionListener listener) {
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

		if (getXCoord(node) + node.width() > treeWidth) {
			treeWidth = (int) (getXCoord(node) + node.width());
		}
		if (getYCoord(node) + nodeHeight >= treeHeight) {
			treeHeight = getYCoord(node) + nodeHeight;
		}
		node.data().getFirst().setBounds(getXCoord(node), getYCoord(node), (int) (node.width()), nodeHeight);
		for (final Node<Pair<Button, IASTNode>> child : node.getChildren()) {
			updateNodePositions(child);
		}
	}

	private void refresh() {
		canvas.redraw();
		canvas.update();
	}

	private int getYCoord(final Node<?> node) {
		return (int) (node.y() * (nodeHeight + GAP_SIZE));
	}

	private int getXCoord(final Node<?> node) {
		return (int) node.x();
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
			button.setBackground(new Color(Display.getCurrent(), 255, 168, 0));
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
			}

			@Override
			public void mouseExit(final MouseEvent e) {
			}

			@Override
			public void mouseEnter(final MouseEvent e) {
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
		refresh();
	}

	private void registerEventHandler(final String topic, final EventHandler handler) {
		final BundleContext ctx = FrameworkUtil.getBundle(ASTView.class).getBundleContext();
		final Dictionary<String, String> props = new Hashtable<>();
		props.put(EventConstants.EVENT_TOPIC, topic);
		ctx.registerService(EventHandler.class.getName(), handler, props);
	}
}
