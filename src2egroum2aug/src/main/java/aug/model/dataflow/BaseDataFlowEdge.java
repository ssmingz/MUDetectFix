package aug.model.dataflow;

import aug.model.BaseEdge;
import aug.model.DataFlowEdge;
import aug.model.Node;

public abstract class BaseDataFlowEdge extends BaseEdge implements DataFlowEdge {
    public BaseDataFlowEdge(Node source, Node target, Type type) {
        super(source, target, type);
    }
}
