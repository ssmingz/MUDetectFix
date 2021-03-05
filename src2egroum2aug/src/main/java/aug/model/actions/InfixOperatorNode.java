package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class InfixOperatorNode extends OperatorNode {
    public InfixOperatorNode(String operator) {
        super(operator);
    }

    public InfixOperatorNode(String operator, int sourceLineNumber) {
        super(operator, sourceLineNumber);
    }
    public InfixOperatorNode(String operator, EGroumNode egroumNode) {
        super(operator, egroumNode);
    }

    public InfixOperatorNode(String operator, int sourceLineNumber, EGroumNode egroumNode) {
        super(operator, sourceLineNumber, egroumNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
