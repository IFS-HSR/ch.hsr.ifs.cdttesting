package ch.hsr.ifs.pasta.tree;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.hsr.ifs.pasta.tree.NodeVisitor.AfterVisitBehaviour;

public class Node<T>  {

    private final T data;
    private final List<Node<T>> children;

    private Node<T> parent;
    private Node<T> ancestor;
    private Node<T> thread;
    private float x;
    private float y;
    private float mod;
    private int number;
    private float change;
    private float shift;
    private float width;
    private boolean treatAsLeaf;

    public Node(T data) {
        this(new ArrayList<Node<T>>(), data);
    }

    public Node(List<Node<T>> children, T data) {
        this.parent = null;
        this.thread = null;
        this.data = data;
        this.children = children;
        this.x = 0;
        this.y = 0;
        this.mod = 0;
        this.ancestor = this;
        this.number = 1;
        this.width = 1;
        this.treatAsLeaf = false;
    }

    public List<Node<T>> children() {
        return hasChildren() ? children : Collections.<Node<T>>emptyList();
    }
    
    public List<Node<T>> getChildren() {
        return children;
    }

    public Node<T> parent() {
        return parent;
    }
    
    /**
     * Traverse the tree post order (children first).
     * @param visitor
     */
    public void visit(NodeVisitor<T> visitor) {

        AfterVisitBehaviour visit = visitor.visit(this);
        if (visit == AfterVisitBehaviour.Abort) return;
    	for (Node<T> child : children) {
            child.visit(visitor);
        }
    }

    public Node<T> leftMostSibling() {
        return (parent.children().get(0) != this) ? parent.children().get(0) : null;
    }

    public Node<T> leftMostChild() {
        if (thread != null) {
            return thread;
        }
        return hasChildren() ? children.get(0) : null;
    }

    private boolean hasChildren() {
        return !(children.isEmpty()  || treatAsLeaf);
    }

    public Node<T> rightMostChild() {
        if (thread != null) {
            return thread;
        }
        return hasChildren() ? children.get(children.size() - 1) : null;
    }

    public Node<T> leftSibling() {
        return hasLeftSibling() ? parent.children().get(this.number() - 2) : null;
    }

    public Node<T> rightSibling() {
        return hasRightSibling() ? parent.children().get(this.number()) : null;
    }

    public boolean hasRightSibling() {
        return (parent != null && (parent.children().size() > this.number()));
    }

    public boolean hasLeftSibling() {
        return (parent != null && this.number > 1);
    }
    
    public void addChild(Node<T> child) {
        children.add(child);
        child.setNumber(children.size());
        child.setY(this.y() + 1);
        child.setParent(this);
    }

    public float x() {
        return x;
    }

    protected void setX(float x) {
        this.x = x;
    }

    public float y() {
        return y;
    }

    protected void setY(float y) {
        this.y = y;
    }
    
    public float width() { 
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        
    }

    public T data() {
        return data;
    }
    
    public void treatAsLeaf(boolean isLeaf) {
        this.treatAsLeaf = isLeaf;
    }
    
    public boolean isTreatedAsLeaf() {
        return treatAsLeaf;
    }

    protected Node<T> ancestor() {
        return ancestor;
    }

    protected void setAncestor(Node<T> ancestor) {
        this.ancestor = ancestor;
    }

    protected float mod() {
        return mod;
    }

    protected void setMod(float mod) {
        this.mod = mod;
    }

    protected Node<T> thread() {
        return thread;
    }

    public void setThread(Node<T> thread) {
        this.thread = thread;
    }

    protected void setNumber(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    protected float change() {
        return change;
    }

    protected void setChange(float change) {
        this.change = change;

    }

    protected float shift() {
        return shift;
    }

    protected void setShift(float shift) {
        this.shift = shift;
    }

    protected void setParent(Node<T> parent) {
        this.parent = parent;
    }
 
    @Override
    public String toString() {
        return data + ": number:" + number + " x:" + x + " y:" + y;
    }
    
    public void adjust() {
        JBaum.reset(this);
        JBaum.adjustTree(this , 1f, 1f);
    }
    
    public void adjust(float siblingDistance, float branchDistance) {
        JBaum.reset(this);
        JBaum.adjustTree(this, siblingDistance, branchDistance);
    }
}
