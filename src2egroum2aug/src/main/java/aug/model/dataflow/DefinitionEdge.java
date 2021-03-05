package aug.model.dataflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

public class DefinitionEdge extends BaseEdge implements DataFlowEdge {
    public DefinitionEdge(Node source, Node target) {
        super(source, target, Type.DEFINITION);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
