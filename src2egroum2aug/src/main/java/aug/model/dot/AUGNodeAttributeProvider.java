package aug.model.dot;

import aug.model.ActionNode;
import aug.model.DataNode;
import aug.model.Node;
import org.jgrapht.ext.ComponentAttributeProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class AUGNodeAttributeProvider implements ComponentAttributeProvider<Node> {
    @Override
    public Map<String, String> getComponentAttributes(Node node) {
        final LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        if (node instanceof ActionNode) {
            attributes.put("shape", "box");
        } else if (node instanceof DataNode) {
            attributes.put("shape", "ellipse");
        }
        return attributes;
    }
}
