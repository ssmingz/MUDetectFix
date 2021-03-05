package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;
import aug.model.dataflow.DefinitionEdge;
import aug.visitors.EdgeVisitor;

/**
 * Throw edges connect from the throwing action to the data node representing the exception, e.g.,
 * <code>inputStream.read() -(throw)-> IOException</code>. In a sense, they are comparable to
 * {@link DefinitionEdge}s.
 */
public class ThrowEdge extends BaseEdge implements DataFlowEdge {
    public ThrowEdge(Node source, Node target) {
        super(source, target, Type.THROW);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
