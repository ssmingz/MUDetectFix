package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class NullCheckNode extends InfixOperatorNode {
    public NullCheckNode() {
        super("<nullcheck>");
    }

    public NullCheckNode(int sourceLineNumber) {
        super("<nullcheck>", sourceLineNumber);
    }

    public NullCheckNode(EGroumNode egroumNode) {
        super("<nullcheck>", egroumNode);
    }

    public NullCheckNode(int sourceLineNumber, EGroumNode egroumNode) {
        super("<nullcheck>", sourceLineNumber, egroumNode);
    }

    @Override
    public boolean isCoreAction() {
        return false;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
