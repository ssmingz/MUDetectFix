package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class RepetitionEdge extends ConditionEdge {
    public RepetitionEdge(Node source, Node target) {
        super(source, target, ConditionType.REPETITION);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
