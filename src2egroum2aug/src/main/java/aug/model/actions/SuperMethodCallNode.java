package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class SuperMethodCallNode extends MethodCallNode {
    public SuperMethodCallNode(String declaringTypeName, String methodSignature, EGroumNode egroumNode) {
        super(declaringTypeName, methodSignature, egroumNode);
    }

    public SuperMethodCallNode(String declaringTypeName, String methodSignature, int sourceLineNumber, EGroumNode egroumNode) {
        super(declaringTypeName, methodSignature, sourceLineNumber, egroumNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
