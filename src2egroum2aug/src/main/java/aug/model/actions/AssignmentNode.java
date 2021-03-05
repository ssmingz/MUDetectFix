package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class AssignmentNode extends BaseNode implements ActionNode {
    public AssignmentNode() {}

    public AssignmentNode(int sourceLineNumber) {
        super(sourceLineNumber);
    }

    public AssignmentNode(int sourceLineNumber, EGroumNode egroumNode) {
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
