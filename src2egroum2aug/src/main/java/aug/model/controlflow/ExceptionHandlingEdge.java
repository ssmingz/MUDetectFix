package aug.model.controlflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;
import aug.visitors.EdgeVisitor;

/**
 * A handling edge represents data flow in the sense that the type information of the exception flows into the handling
 * code.
 */
public class ExceptionHandlingEdge extends BaseEdge implements DataFlowEdge {
    public ExceptionHandlingEdge(Node source, Node target) {
        super(source, target, Type.EXCEPTION_HANDLING);
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
