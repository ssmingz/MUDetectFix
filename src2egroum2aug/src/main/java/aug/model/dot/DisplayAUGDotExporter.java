package aug.model.dot;

import aug.visitors.BaseAUGLabelProvider;
import aug.visitors.WithSourceLineNumberLabelProvider;

public class DisplayAUGDotExporter extends AUGDotExporter {
    public DisplayAUGDotExporter() {
        super(new WithSourceLineNumberLabelProvider(new BaseAUGLabelProvider()),
                new AUGNodeAttributeProvider(),
                new AUGEdgeAttributeProvider());
    }
}
