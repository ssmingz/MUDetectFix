package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.ControlFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class OrderEdge extends BaseEdge implements ControlFlowEdge {
    public OrderEdge(Node source, Node target) {
        super(source, target, Type.ORDER);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
