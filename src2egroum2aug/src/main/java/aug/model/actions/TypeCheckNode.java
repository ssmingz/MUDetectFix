package aug.model.actions;

import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class TypeCheckNode extends OperatorNode {
    private final String targetTypeName;

    public TypeCheckNode(String targetTypeName) {
        super("<instanceof>");
        this.targetTypeName = targetTypeName;
    }

    public TypeCheckNode(String targetTypeName, int sourceLineNumber) {
        super("<instanceof>", sourceLineNumber);
        this.targetTypeName = targetTypeName;
    }

    public TypeCheckNode(String targetTypeName, EGroumNode egroumNode) {
        super("<instanceof>", egroumNode);
        this.targetTypeName = targetTypeName;
    }

    public TypeCheckNode(String targetTypeName, int sourceLineNumber, EGroumNode egroumNode) {
        super("<instanceof>", sourceLineNumber, egroumNode);
        this.targetTypeName = targetTypeName;
    }

    public String getTargetTypeName() {
        return targetTypeName;
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
