package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class CatchNode extends BaseNode implements ActionNode {
    private final String exceptionType;

    public CatchNode(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public CatchNode(String exceptionType, int sourceLineNumber) {
        super(sourceLineNumber);
        this.exceptionType = exceptionType;
    }

    public CatchNode(String exceptionType, EGroumNode egroumNode) {
        this.exceptionType = exceptionType;
        this.egroumNode = egroumNode;
    }

    public CatchNode(String exceptionType, int sourceLineNumber, EGroumNode egroumNode) {
        super(sourceLineNumber, egroumNode);
        this.exceptionType = exceptionType;
    }

    public String getExceptionType() {
        return exceptionType;
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
