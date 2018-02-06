package ch.hsr.ifs.pasta;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;


public class NodeWidget extends Composite {

   private Tree tree;

   public NodeWidget(final Composite parent) {
      super(parent, SWT.NONE);
      init();
   }

   private void init() {
      setLayout(new FillLayout());
      setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
      tree = new Tree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
      tree.setLayout(new FillLayout());
      tree.setHeaderVisible(true);
      tree.setBounds(getParent().getClientArea());
      final TreeColumn nameCol = new TreeColumn(tree, SWT.LEFT);
      nameCol.setText("Name");
      nameCol.setWidth(200);
      final TreeColumn valueCol = new TreeColumn(tree, SWT.LEFT);
      valueCol.setText("Value");
      valueCol.setWidth(800);
      tree.setVisible(true);
   }

   public void displayNode(final IASTNode node) {
      try {
         node.getTranslationUnit().getIndex().acquireReadLock();
         clearTree();
         displayName(node);
         displayBindings(node);
         displayImplicitNames(node);
         final TreeItem typeHierarchy = createTreeItem(tree, "Type Hierarchy;");
         displayTypeHierarchy(typeHierarchy, node);
         final TreeItem fields = createTreeItem(tree, "Fields;@" + Integer.toString(node.hashCode(), 16));
         displayFields(fields, node);
         final TreeItem methods = createTreeItem(tree, "Methods;");
         displayMethods(methods, node);
         expandFirstLevel();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      } finally {
         node.getTranslationUnit().getIndex().releaseReadLock();
      }
   }

   private void displayTypeIfPresent(final TreeItem parent, final IBinding binding) {
      try {
         final Method getTypeMethod = getGetTypeMethod(binding);
         if (getTypeMethod != null) {
            final TreeItem parentOfType = createTreeItem(parent, "Type;");
            final Object type = getTypeMethod.invoke(binding);
            createTreeItem(parentOfType, "toString();" + type);
            final TreeItem typeHierarchy = createTreeItem(parentOfType, "Type Hierarchy;");
            displayTypeHierarchy(typeHierarchy, type);
         }
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         // PastaPlugin.log(e); // Uncomment for debugging
      }
   }

   private Method[] getMethods(final Object node) {
      if (node == null) { return new Method[0]; }
      final Class<?> clazz = node.getClass();
      if (clazz == null) { return new Method[0]; }
      return clazz.getMethods();
   }

   private Method getGetTypeMethod(final Object o) {
      final Method[] methods = getMethods(o);
      for (final Method method : methods) {
         if (Modifier.isPublic(method.getModifiers()) && method.getName().equals("getType")) { return method; }
      }
      return null;
   }

   private void displayMethods(final TreeItem parent, final Object node) {
      collectMethods(parent, node.getClass());
   }

   private void displayName(final IASTNode node) {
      createTreeItem(tree, node.getClass().getSimpleName() + ";" + safeToString(node));
   }

   private void clearTree() {
      for (final TreeItem item : tree.getItems()) {
         item.dispose();
      }
   }

   private void displayFields(final TreeItem parent, final Object node) {
      final Class<? extends Object> theClass = node.getClass();
      addFieldsOfClass(theClass, parent, node);
   }

   private void addFieldsOfClass(final Class<? extends Object> theClass, final TreeItem parent, final Object node) {
      final List<Field> fields = new ArrayList<>(Arrays.asList(theClass.getFields()));
      fields.addAll(Arrays.asList(theClass.getDeclaredFields()));
      for (final Field field : fields) {
         makeAccessible(field);
         if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) { // PS,
            // don't
            // want
            // static
            // fields
            createFieldValueEntries(parent, field.getName(), getValue(field, node));
         } // do not display static
      }
      final Class<?> superclass = theClass.getSuperclass();
      if (superclass != Object.class) {
         final TreeItem superfields = createTreeItem(parent, superclass.getSimpleName() + ";");
         addFieldsOfClass(theClass.getSuperclass(), superfields, node);
         superfields.setExpanded(true);
      }
   }

   public void createFieldValueEntries(final TreeItem parent, final String fieldName, final Object fieldValue) {
      if (fieldValue != null && fieldValue.getClass().isArray()) {
         final int len = Array.getLength(fieldValue);
         final TreeItem arrayitem = createTreeItem(parent, fieldName + "[" + len + "]" + ";" + fieldValue);
         for (int i = 0; i < len; ++i) {
            final Object entryvalue = Array.get(fieldValue, i);
            final String nameForArrayField = fieldName + "[" + i + "]";
            createFieldValueEntries(arrayitem, nameForArrayField, entryvalue);
         }
      } else {
         createFieldEntrySafe(parent, fieldName, fieldValue);
      }
   }

   public void createFieldEntrySafe(final TreeItem parent, final String nameForField, final Object fieldValue) {
      // workaround for CPPASTNameBase.toString() NPE
      String value = safeToString(fieldValue);
      if (fieldValue != null) {
         value += " : " + fieldValue.getClass().getSimpleName();
      }
      createTreeItem(parent, nameForField + ";" + value);
   }

   public String safeToString(final Object fieldValue) {
      String value = "null";
      if (fieldValue != null) {
         try {
            value = fieldValue.toString();
         } catch (final Throwable t) {
            PastaPlugin.log(t);
            value = "tostring throws: " + t;
         }
      }
      return value;
   }

   private void displayTypeHierarchy(final TreeItem parent, final Object o) {
      collectSuperclasses(parent, o.getClass());
      parent.setExpanded(true);
   }

   private void collectSuperclasses(final TreeItem superClasses, final Class<?> clazz) {
      if (clazz == null) { return; }
      final TreeItem classItem = createTreeItem(superClasses, clazz.getSimpleName() + ";");
      displayInterfaceHierarchy(classItem, clazz);
      collectSuperclasses(classItem, clazz.getSuperclass());
      classItem.setExpanded(true);
   }

   private void displayInterfaceHierarchy(final TreeItem classItem, final Class<?> clazz) {
      for (final Class<?> interfaceClass : clazz.getInterfaces()) {
         final TreeItem interfaceItem = createTreeItem(classItem, interfaceClass.getSimpleName() + ";");
         displayInterfaceHierarchy(interfaceItem, interfaceClass);
      }
   }

   private void collectMethods(final TreeItem parentItem, final Class<?> clazz) {
      if (clazz == null) { return; }
      for (final Method method : clazz.getMethods()) {
         if (Modifier.isPublic(method.getModifiers())) {
            createTreeItem(parentItem, method.getName() + ";" + method.getReturnType().getSimpleName());
         }
      }
      final Class<?> superclass = clazz.getSuperclass();
      if (superclass != Object.class) {
         final TreeItem supermethods = createTreeItem(parentItem, superclass.getSimpleName() + ";");
         collectMethods(supermethods, superclass);
         supermethods.setExpanded(true);
      }

   }

   private void displayImplicitNames(final IASTNode node) {
      if (node instanceof IASTImplicitNameOwner) {
         final IASTImplicitName[] implicitNames = ((IASTImplicitNameOwner) node).getImplicitNames();
         final TreeItem parent = createTreeItem(tree, "Implicit Names;" + implicitNames.length);
         for (final IASTImplicitName implicitName : implicitNames) {
            try {
               final TreeItem implicitNameItem = createTreeItem(parent, implicitName.resolveBinding().getName() + ";");
               final TreeItem bindingsParent = createTreeItem(implicitNameItem, "Bindings;");
               displayBindings(bindingsParent, node, implicitName.resolveBinding());
            } catch (final Exception e) {
               PastaPlugin.log(e);
            }
         }
      }
   }

   private void displayBindings(final IASTNode node) {
      try {
         if (node instanceof IASTName) {
            final IBinding binding = ((IASTName) node).resolveBinding();
            final TreeItem parent = createTreeItem(tree, "Bindings;");
            displayBindings(parent, node, binding);
         }
      } catch (final Exception e) {
         PastaPlugin.log(e);
      }
   }

   private void displayBindings(final TreeItem parent, final IASTNode node, final IBinding binding) throws CoreException {
      final IASTTranslationUnit ast = node.getTranslationUnit();
      for (final IASTName decl : ast.getDeclarationsInAST(binding)) {
         createTreeItem(parent, "declaration;" + decl);
      }
      for (final IASTName def : ast.getDefinitionsInAST(binding)) {
         createTreeItem(parent, "definition;" + def);
      }
      for (final IASTName ref : ast.getReferences(binding)) {
         createTreeItem(parent, "reference;" + ref);
      }
      displayTypeIfPresent(parent, binding);

      final TreeItem typeHierarchy = createTreeItem(parent, "Type Hierarchy;");
      displayTypeHierarchy(typeHierarchy, binding);

      final TreeItem fields = createTreeItem(parent, "Fields;");
      displayFields(fields, binding);

      final TreeItem methods = createTreeItem(parent, "Methods;");
      displayMethods(methods, binding);

   }

   private void makeAccessible(final Field field) {
      try {
         field.setAccessible(true);
         final Field modifierField = Field.class.getDeclaredField("modifiers");
         modifierField.setAccessible(true);
         modifierField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      } catch (final Exception e) {
         PastaPlugin.log(e);
      }
   }

   private void expandFirstLevel() {
      for (final TreeItem item : tree.getItems()) {
         item.setExpanded(true);
      }
   }

   private Object getValue(final Field field, final Object node) {
      try {
         return field.get(node);
      } catch (final Throwable e) {
         PastaPlugin.log(e);
         return "error loading field value";
      }
   }

   private TreeItem createTreeItem(final Tree parent, final String string) {
      final TreeItem treeItem = new TreeItem(parent, SWT.NONE);
      return configureTreeItem(string, treeItem);
   }

   private TreeItem createTreeItem(final TreeItem parent, final String content) {
      final TreeItem treeItem = new TreeItem(parent, SWT.NONE);
      return configureTreeItem(content, treeItem);
   }

   private TreeItem configureTreeItem(final String string, final TreeItem treeItem) {
      treeItem.setText(string.split(";"));
      treeItem.setExpanded(true);
      return treeItem;
   }
}
