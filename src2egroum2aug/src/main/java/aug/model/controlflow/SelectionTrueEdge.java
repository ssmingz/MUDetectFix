package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class SelectionTrueEdge extends SelectionEdge{
    public SelectionTrueEdge(Node source, Node target) {
        super(source, target);
        this.conditionType = ConditionType.SELECTION_T;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
