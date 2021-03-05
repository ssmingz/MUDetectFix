package aug.model.data;

import aug.model.BaseNode;
import aug.model.DataNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class AnonymousClassMethodNode extends BaseNode implements DataNode {
    private final String baseType;
    private final String methodSignature;

    public AnonymousClassMethodNode(String baseType, String methodSignature) {
        this.baseType = baseType;
        this.methodSignature = methodSignature;
    }

    public AnonymousClassMethodNode(String baseType, String methodSignature, EGroumNode egroumNode) {
        this.baseType = baseType;
        this.methodSignature = methodSignature;
        this.egroumNode = egroumNode;
    }

    @Override
    public String getType() {
        return baseType;
    }

    @Override
    public String getName() {
        return methodSignature;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public void setName(String newName) { }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
