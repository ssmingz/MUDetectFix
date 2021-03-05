package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.ControlFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class SynchronizationEdge extends BaseEdge implements ControlFlowEdge {
    public SynchronizationEdge(Node source, Node target) {
        super(source, target, Type.SYNCHRONIZE);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
