package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class SelectionEdge extends ConditionEdge {
    public SelectionEdge(Node source, Node target) {
        super(source, target, ConditionType.SELECTION);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
