package aug.visitors;

import aug.model.Edge;
import aug.model.Node;

public interface AUGLabelProvider extends AUGElementVisitor<String> {
    default String getLabel(Node node) {
        return node.apply(this);
    }

    default String getLabel(Edge edge) {
        return edge.apply(this);
    }
}
