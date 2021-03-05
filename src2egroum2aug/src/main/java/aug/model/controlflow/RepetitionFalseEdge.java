package aug.model.controlflow;

import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class RepetitionFalseEdge extends RepetitionEdge{
    public RepetitionFalseEdge(Node source, Node target) {
        super(source, target);
        this.conditionType = ConditionType.REPETITION_F;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
