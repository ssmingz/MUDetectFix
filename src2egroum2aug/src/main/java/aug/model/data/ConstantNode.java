package aug.model.data;

import aug.model.BaseNode;
import aug.model.DataNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;

public class ConstantNode extends BaseNode implements DataNode {
    private final String dataType;
    private final String dataName;
    private final String dataValue;

    public ConstantNode(String dataType, String dataName, String dataValue) {
        this.dataType = dataType;
        this.dataName = dataName;
        this.dataValue = dataValue;
    }

    public ConstantNode(String dataType, String dataName, String dataValue, EGroumNode egroumNode) {
        this.dataType = dataType;
        this.dataName = dataName;
        this.dataValue = dataValue;
        this.egroumNode = egroumNode;
    }

    @Override
    public String getName() {
        return dataName;
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
