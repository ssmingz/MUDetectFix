package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class SuperConstructorCallNode extends ConstructorCallNode {
    public SuperConstructorCallNode(String superTypeName) {
        super(superTypeName);
    }

    public SuperConstructorCallNode(String superTypeName, int sourceLineNumber) {
        super(superTypeName, sourceLineNumber);
    }
    public SuperConstructorCallNode(String superTypeName, EGroumNode egroumNode) {
        super(superTypeName, egroumNode);
    }

    public SuperConstructorCallNode(String superTypeName, int sourceLineNumber, EGroumNode egroumNode) {
        super(superTypeName, sourceLineNumber, egroumNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
