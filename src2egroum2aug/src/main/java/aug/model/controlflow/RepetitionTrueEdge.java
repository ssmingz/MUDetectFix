package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class RepetitionTrueEdge extends RepetitionEdge{
    public RepetitionTrueEdge(Node source, Node target) {
        super(source, target);
        this.conditionType = ConditionType.REPETITION_T;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
