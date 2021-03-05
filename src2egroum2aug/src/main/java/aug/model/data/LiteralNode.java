package aug.model.data;

import aug.model.BaseNode;
import aug.model.DataNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class LiteralNode extends BaseNode implements DataNode {
    private final String dataType;
    private final String dataValue;

    public LiteralNode(String dataType, String dataValue) {
        this.dataType = dataType;
        this.dataValue = dataValue;
    }

    public LiteralNode(String dataType, String dataValue, EGroumNode egroumNode) {
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.egroumNode = egroumNode;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return dataValue;
    }

    @Override
    public String getType() {
        return dataType;
    }

    @Override
    public void setName(String newName) { }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
