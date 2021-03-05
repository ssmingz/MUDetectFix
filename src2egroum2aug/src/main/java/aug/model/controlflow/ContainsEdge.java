package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class ContainsEdge extends BaseEdge {
    public ContainsEdge(Node source, Node target) {
        super(source, target, Type.CONTAINS);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
