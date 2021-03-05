package aug.model.data;

import aug.model.BaseNode;
import aug.model.DataNode;
import aug.visitors.NodeVisitor;
import edu.iastate.cs.egroum.aug.EGroumNode;


public class VariableNode extends BaseNode implements DataNode {
    private final String variableType;
    private String variableName;

    public VariableNode(String variableType, String variableName) {
        this.variableType = variableType;
        this.variableName = variableName;
    }

    public VariableNode(String variableType, String variableName, EGroumNode egroumNode) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.egroumNode = egroumNode;
    }

    @Override
    public String getName() {
        return variableName;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getType() {
        return variableType;
    }

    @Override
    public void setName(String newName) {
        this.variableName = newName;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
