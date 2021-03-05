package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

import java.util.Optional;

public class MethodCallNode extends BaseNode implements ActionNode {
    private final String declaringTypeName;
    private final String methodSignature;

    public MethodCallNode(String declaringTypeName, String methodSignature) {
        this.declaringTypeName = declaringTypeName;
        this.methodSignature = methodSignature;
    }

    public MethodCallNode(String declaringTypeName, String methodSignature, int sourceLineNumber) {
        super(sourceLineNumber);
        this.declaringTypeName = declaringTypeName;
        this.methodSignature = methodSignature;
    }

    public MethodCallNode(String declaringTypeName, String methodSignature, EGroumNode egroumNode) {
        this.declaringTypeName = declaringTypeName;
        this.methodSignature = methodSignature;
        this.egroumNode = egroumNode;
    }

    public MethodCallNode(String declaringTypeName, String methodSignature, int sourceLineNumber, EGroumNode egroumNode) {
        super(sourceLineNumber, egroumNode);
        this.declaringTypeName = declaringTypeName;
        this.methodSignature = methodSignature;
    }

    @Override
    public boolean isCoreAction() {
        return !getMethodSignature().startsWith("get");
    }

    @Override
    public Optional<String> getAPI() {
        String declaringType = getDeclaringTypeName();
        if (!declaringType.isEmpty() && !declaringType.endsWith("[]"))
            return Optional.of(declaringType);
        else
            return Optional.empty();
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getDeclaringTypeName() {
        return declaringTypeName;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
