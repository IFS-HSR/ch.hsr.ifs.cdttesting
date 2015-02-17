package ch.hsr.ifs.pasta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTImplicitNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNameBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

@SuppressWarnings("restriction")
public class NodeWidget extends Composite {

    private Tree tree;

    public NodeWidget(Composite parent) {
        super(parent, SWT.NONE);
        init();
    }

    private void init() {
        this.setLayout(new FillLayout());
        this.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        tree = new Tree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        tree.setLayout(new FillLayout());
        tree.setHeaderVisible(true);
        tree.setBounds(getParent().getClientArea());
        TreeColumn nameCol = new TreeColumn(tree, SWT.LEFT);
        nameCol.setText("Name");
        nameCol.setWidth(200);
        TreeColumn valueCol = new TreeColumn(tree, SWT.LEFT);
        valueCol.setText("Value");
        valueCol.setWidth(200);
        tree.setVisible(true);
    }

    public void displayNode(IASTNode node) {
        clearTree();
        displayName(node);
        displayBindings(node);
        displayImplicitNames(node);
        TreeItem typeHierarchy = createTreeItem(tree, "Type Hierarchy;");
        displayTypeHierarchy(typeHierarchy, node);
        TreeItem fields = createTreeItem(tree, "Fields;");
        displayFields(fields, node);
        TreeItem methods = createTreeItem(tree, "Methods;");
        displayMethods(methods, node);
        expandFirstLevel();
    }

    private void displayMethods(TreeItem parent, Object node) {
        collectMethods(parent, node.getClass());
    }

    private void displayName(IASTNode node) {
        createTreeItem(tree, node.getClass().getSimpleName() + ";");
    }

    private void clearTree() {
        for (TreeItem item : tree.getItems()) {
            item.dispose();
        }
    }

	private void displayFields(TreeItem parent, Object node) {
        List<Field> fields = new ArrayList<>(Arrays.asList(node.getClass().getFields()));
        fields.addAll(Arrays.asList(node.getClass().getDeclaredFields()));
        for (Field field : fields) {
            makeAccessible(field);
            Object fieldValue = getValue(field, node);
            if (!(fieldValue instanceof CPPASTNameBase)) { // workaround for CPPASTNameBase.toString() NPE
                createTreeItem(parent, field.getName() + ";" + getValue(field, node));
            }
        }
    }

    private void displayTypeHierarchy(TreeItem parent, Object o) {
        collectSuperclasses(parent, o.getClass());
        parent.setExpanded(true);
    }

    private void collectSuperclasses(TreeItem superClasses, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        TreeItem classItem = createTreeItem(superClasses, clazz.getSimpleName() + ";");
        displayInterfaceHierarchy(classItem, clazz);
        collectSuperclasses(classItem, clazz.getSuperclass());
        classItem.setExpanded(true);
    }

    private void displayInterfaceHierarchy(TreeItem classItem, Class<?> clazz) {
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            TreeItem interfaceItem = createTreeItem(classItem, interfaceClass.getSimpleName() + ";");
            displayInterfaceHierarchy(interfaceItem, interfaceClass);
        }
    }

    private void collectMethods(TreeItem parentItem, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        for (Method method : clazz.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                createTreeItem(parentItem, method.getName() + ";" + method.getReturnType().getSimpleName());
            }
        }
    }
    
    private void displayImplicitNames(IASTNode node) {
        if (node instanceof IASTImplicitNameOwner) {
            IASTImplicitName[] implicitNames = ((IASTImplicitNameOwner) node).getImplicitNames();
            TreeItem parent = createTreeItem(tree, "Implicit Names;"+implicitNames.length);
            for (IASTImplicitName implicitName : implicitNames) {
                createTreeItem(parent, implicitName.resolveBinding().getName()+";");
            }
        }
    }

    private void displayBindings(IASTNode node) {
        try {
            if (node instanceof IASTName) {
                IASTTranslationUnit ast = node.getTranslationUnit();
                TreeItem parent = createTreeItem(tree, "Bindings;");
                IBinding binding = ((IASTName) node).resolveBinding();
                IIndex index = node.getTranslationUnit().getIndex();
                for (IIndexName decl : index.findDeclarations(binding)) {
                    createTreeItem(parent, "declaration;" + decl.getEnclosingDefinition());
                }
                for (IIndexName def : index.findDefinitions(binding)) {
                    createTreeItem(parent, "definition;" + ast.getNodeSelector(null).findEnclosingNode(def.getNodeOffset(), def.getNodeLength()));
                }
                for (IIndexName ref : index.findReferences(binding)) {
                    createTreeItem(parent, "reference;" +  ast.getNodeSelector(null).findEnclosingNode(ref.getNodeOffset(), ref.getNodeLength()));
                }

                TreeItem typeHierarchy = createTreeItem(parent, "Type Hierarchy;");
                displayTypeHierarchy(typeHierarchy, binding);
                
                TreeItem fields = createTreeItem(parent, "Fields;");
                displayFields(fields, binding);
                
                TreeItem methods = createTreeItem(parent, "Methods;");
                displayMethods(methods, binding);
            }
        } catch (Exception e) {
            PastaPlugin.log(e);
        }
    }

    private void makeAccessible(Field field) {
        try {
            field.setAccessible(true);
            Field modifierField = Field.class.getDeclaredField("modifiers");
            modifierField.setAccessible(true);
            modifierField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception e) {
            PastaPlugin.log(e);
        }
    }

    private void expandFirstLevel() {
        for (TreeItem item : tree.getItems()) {
            item.setExpanded(true);
        }
    }

    private Object getValue(Field field, Object node) {
        try {
            return field.get(node);
        } catch (Throwable e) {
            PastaPlugin.log(e);
            return "error loading field value";
        }
    }

    private TreeItem createTreeItem(Tree parent, String string) {
        TreeItem treeItem = new TreeItem(parent, SWT.NONE);
        return configureTreeItem(string, treeItem);
    }

    private TreeItem createTreeItem(TreeItem parent, String content) {
        TreeItem treeItem = new TreeItem(parent, SWT.NONE);
        return configureTreeItem(content, treeItem);
    }

    private TreeItem configureTreeItem(String string, TreeItem treeItem) {
        treeItem.setText(string.split(";"));
        treeItem.setExpanded(true);
        return treeItem;
    }
}
