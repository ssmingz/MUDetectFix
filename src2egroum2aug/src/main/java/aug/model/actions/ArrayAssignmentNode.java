package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ArrayAssignmentNode extends MethodCallNode {
    public ArrayAssignmentNode(String arrayTypeName) {
        super(arrayTypeName, "arrayset()");
    }

    public ArrayAssignmentNode(String arrayTypeName, int sourceLineNumber) {
        super(arrayTypeName, "arrayset()", sourceLineNumber);
    }
    public ArrayAssignmentNode(String arrayTypeName, EGroumNode egroumNode) {
        super(arrayTypeName, "arrayset()", egroumNode);
    }

    public ArrayAssignmentNode(String arrayTypeName, int sourceLineNumber, EGroumNode egroumNode) {
        super(arrayTypeName, "arrayset()", sourceLineNumber, egroumNode);
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
