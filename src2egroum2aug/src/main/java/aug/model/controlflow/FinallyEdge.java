package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.ControlFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class FinallyEdge extends BaseEdge implements ControlFlowEdge {
    public FinallyEdge(Node source, Node target) {
        super(source, target, Type.FINALLY);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
