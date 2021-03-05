package aug.model.dataflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class ParameterEdge extends BaseEdge implements DataFlowEdge {
    public ParameterEdge(Node source, Node target) {
        super(source, target, Type.PARAMETER);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
