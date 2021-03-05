package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class UnaryOperatorNode extends OperatorNode {
    public UnaryOperatorNode(String operator) {
        super(operator);
    }

    public UnaryOperatorNode(String operator, int sourceLineNumber) {
        super(operator, sourceLineNumber);
    }

    public UnaryOperatorNode(String operator, EGroumNode egroumNode) {
        super(operator, egroumNode);
    }

    public UnaryOperatorNode(String operator, int sourceLineNumber, EGroumNode egroumNode) {
        super(operator, sourceLineNumber, egroumNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
