package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ConstructorCallNode extends MethodCallNode {
    public ConstructorCallNode(String typeName) {
        super(typeName, "<init>");
    }

    public ConstructorCallNode(String typeName, int sourceLineNumber) {
        super(typeName, "<init>", sourceLineNumber);
    }

    public ConstructorCallNode(String typeName, EGroumNode egroumNode) {
        super(typeName, "<init>", egroumNode);
    }

    public ConstructorCallNode(String typeName, int sourceLineNumber, EGroumNode egroumNode) {
        super(typeName, "<init>", sourceLineNumber, egroumNode);
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
