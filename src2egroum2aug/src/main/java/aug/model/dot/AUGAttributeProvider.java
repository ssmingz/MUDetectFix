package aug.model.dot;

import aug.model.APIUsageGraph;

import java.util.Map;

public interface AUGAttributeProvider<G extends APIUsageGraph> {
    Map<String, String> getAUGAttributes(G aug);
}
