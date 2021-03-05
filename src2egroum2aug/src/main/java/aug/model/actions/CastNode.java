package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

import java.util.Optional;

public class CastNode extends BaseNode implements ActionNode {
    private final String targetType;

    public CastNode(String targetType) {
        this.targetType = targetType;
    }

    public CastNode(String targetType, int sourceLineNumber) {
        super(sourceLineNumber);
        this.targetType = targetType;
    }

    public CastNode(String targetType, EGroumNode egroumNode) {
        this.targetType = targetType;
        this.egroumNode = egroumNode;
    }

    public CastNode(String targetType, int sourceLineNumber, EGroumNode egroumNode) {
        super(sourceLineNumber, egroumNode);
        this.targetType = targetType;
    }

    public String getTargetType() {
        return targetType;
    }

    @Override
    public Optional<String> getAPI() {
        return Optional.of(targetType);
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
