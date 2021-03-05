package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ReturnNode extends BaseNode implements ActionNode {
    public ReturnNode() {}

    public ReturnNode(int sourceLineNumber) {
        super(sourceLineNumber);
    }
    public ReturnNode(int sourceLineNumber, EGroumNode egroumNode) {
        super(sourceLineNumber, egroumNode);
    }

    @Override
    public boolean isCoreAction() {
        return true;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
