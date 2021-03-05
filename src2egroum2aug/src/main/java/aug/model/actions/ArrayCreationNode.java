package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ArrayCreationNode extends ConstructorCallNode {
    public ArrayCreationNode(String baseType) {
        super(baseType);
    }

    public ArrayCreationNode(String baseType, int sourceLineNumber) {
        super(baseType, sourceLineNumber);
    }
    public ArrayCreationNode(String baseType, EGroumNode egroumNode) {
        super(baseType, egroumNode);
    }

    public ArrayCreationNode(String baseType, int sourceLineNumber, EGroumNode egroumNode) {
        super(baseType, sourceLineNumber, egroumNode);
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
