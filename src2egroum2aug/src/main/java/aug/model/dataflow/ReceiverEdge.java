package aug.model.dataflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class ReceiverEdge extends BaseEdge implements DataFlowEdge {
    public ReceiverEdge(Node source, Node target) {
        super(source, target, Type.RECEIVER);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
