package aug.model.data;

import aug.model.BaseNode;
import aug.model.DataNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class AnonymousObjectNode extends BaseNode implements DataNode {
    private final String dataType;

    public AnonymousObjectNode(String dataType) {
        this.dataType = dataType;
    }

    public AnonymousObjectNode(String dataType, EGroumNode egroumNode) {
        this.dataType = dataType;
        this.egroumNode = egroumNode;
    }

    @Override
    public String getType() {
        return dataType;
    }

    @Override
    public String getName() {
        return "Some(" + dataType + ")";
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
