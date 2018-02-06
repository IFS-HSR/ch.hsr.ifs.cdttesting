package ch.hsr.ifs.pasta.tree;

import javax.swing.JFrame;

public class TestApp {

    public static void main(String[] args) {

        Node<String> root = new Node<String>("root");
        JBaum.adjustTree(root, 1, 1);
        JFrame jFrame = new JFrame("JBaumDebug");
        jFrame.add(new JBaumDebugView(root));
        jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

    }
}
