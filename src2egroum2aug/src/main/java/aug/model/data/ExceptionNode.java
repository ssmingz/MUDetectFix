package aug.model.data;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ExceptionNode extends VariableNode {
    public ExceptionNode(String exceptionType, String variableName) {
        super(exceptionType, variableName);
    }

    public ExceptionNode(String exceptionType, String variableName, EGroumNode egroumNode) {
        super(exceptionType, variableName, egroumNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
