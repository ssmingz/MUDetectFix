package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class SelectionFalseEdge extends SelectionEdge{
    public SelectionFalseEdge(Node source, Node target) {
        super(source, target);
        this.conditionType = ConditionType.SELECTION_F;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
