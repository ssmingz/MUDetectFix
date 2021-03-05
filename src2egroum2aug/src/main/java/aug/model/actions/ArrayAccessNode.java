package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ArrayAccessNode extends MethodCallNode {
    public ArrayAccessNode(String arrayTypeName) {
        super(arrayTypeName, "arrayget()");
    }

    public ArrayAccessNode(String arrayTypeName, int sourceLineNumber) {
        super(arrayTypeName, "arrayget()", sourceLineNumber);
    }
    public ArrayAccessNode(String arrayTypeName, EGroumNode egroumNode) {
        super(arrayTypeName, "arrayget()", egroumNode);
    }

    public ArrayAccessNode(String arrayTypeName, int sourceLineNumber, EGroumNode egroumNode) {
        super(arrayTypeName, "arrayget()", sourceLineNumber, egroumNode);
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
