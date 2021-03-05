package fix;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.model.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.HashMap;
import java.util.Map;

public class ASTNodeMapping {
    public Map<ASTNode, ASTNode> targetASTNodeByPatternASTNode = new HashMap<>();
    public Map<String, String> targetVarByPatternVar = new HashMap<>();
    private Map<Node, Node> targetNodeByPatternNode;

    public ASTNodeMapping(Map<Node, Node> targetByPattern) {
        this.targetNodeByPatternNode = targetByPattern;
    }

    public void findVarMappings() {
        for(Map.Entry<Node, Node> entry : targetNodeByPatternNode.entrySet()) {
            Node pNode = entry.getKey(), tNode = entry.getValue();
            // focus on action node, skip data node
            if(pNode instanceof ActionNode) {
                // action node pair
                ASTNode pASTNode = ((BaseNode) pNode).egroumNode.getAstNode();
                ASTNode tASTNode = ((BaseNode) tNode).egroumNode.getAstNode();
                // If the two nodes have the same type, then keep checking on their children. Otherwise, stop checking.
                // Checking process: If two data nodes mapped, then add them to the map. Otherwise, skip to the next.
                findChildVarMappings(pASTNode, tASTNode);
            }
        }
    }

    private void findChildVarMappings(ASTNode pASTNode, ASTNode tASTNode) {
        if(pASTNode == null || tASTNode == null) {
            return;
        }
        if(pASTNode.getNodeType() != tASTNode.getNodeType()) {
            return;
        }
        if(pASTNode instanceof MethodInvocation) {
            findVarMapping((MethodInvocation) pASTNode, (MethodInvocation) tASTNode);
        }

    }

    private void findVarMapping(MethodInvocation pASTNode, MethodInvocation tASTNode) {
        // receiver
        if(pASTNode.getExpression() instanceof SimpleName && tASTNode.getExpression() instanceof SimpleName) {
            SimpleName pVar = ((SimpleName) pASTNode.getExpression());
            SimpleName tVar = ((SimpleName) tASTNode.getExpression());
            addToVarMap(pVar, tVar);
        }
        // arguments
        int pArgSize = pASTNode.arguments().size(), tArgSize = tASTNode.arguments().size();
        if(pArgSize != 0 && tArgSize != 0 && pArgSize == tArgSize) {
            for(int i = 0; i < pASTNode.arguments().size(); i++) {
                if(pASTNode.arguments().get(i) instanceof SimpleName && tASTNode.arguments().get(i) instanceof SimpleName) {
                    SimpleName pVar = ((SimpleName) pASTNode.arguments().get(i));
                    SimpleName tVar = ((SimpleName) tASTNode.arguments().get(i));
                    addToVarMap(pVar, tVar);
                }
            }
        }
    }

    private void addToVarMap(SimpleName pVar, SimpleName tVar) {
        // avoid repetition
        for(String aVar : targetVarByPatternVar.keySet()) {
            if(aVar.equals(pVar.getIdentifier()))
                return;
        }
        targetVarByPatternVar.put(pVar.getIdentifier(), tVar.getIdentifier());
    }
}
