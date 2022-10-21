package xqd.compiler.core.tool;

import xqd.compiler.core.unit.RuleUnit;
import xqd.compiler.core.unit.TermUnit;
import xqd.compiler.core.unit.Unit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;

public class UnitTreeView {
    Map<Short, String> nodeNameMap;

    public UnitTreeView(Map<Short, String> nodeNameMap) {
        this.nodeNameMap = nodeNameMap;
    }

    public void showTree(RuleUnit unit ) {
        JDialog jDialog = new JDialog();
        jDialog.getContentPane().add(new JScrollPane(new JTree(buildTree(unit))));
        jDialog.setVisible(true);
        jDialog.setSize(500,500);
        jDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    DefaultMutableTreeNode buildTree(RuleUnit node ) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(nodeNameMap.get(node.id));
        for (Unit node1 : node.unitList) {
            if (node1 instanceof TermUnit) {
                treeNode.add(new DefaultMutableTreeNode(nodeNameMap.get(node1.id)+":"+((TermUnit) node1).text));
            }else{
                treeNode.add(buildTree((RuleUnit) node1) );
            }
        }
        return treeNode;
    }
}
