package aug.model.actions;

import aug.model.ActionNode;
import aug.model.BaseNode;
import edu.iastate.cs.egroum.aug.EGroumNode;

public abstract class OperatorNode extends BaseNode implements ActionNode {
    private final String operator;

    OperatorNode(String operator) {
        this.operator = operator;
    }

    OperatorNode(String operator, int sourceLineNumber) {
        super(sourceLineNumber);
        this.operator = operator;
    }

    OperatorNode(String operator, EGroumNode egroumNode) {
        this.operator = operator;
        this.egroumNode = egroumNode;
    }

    OperatorNode(String operator, int sourceLineNumber, EGroumNode egroumNode) {
        super(sourceLineNumber, egroumNode);
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public boolean isCoreAction() {
        return false;
    }
}
