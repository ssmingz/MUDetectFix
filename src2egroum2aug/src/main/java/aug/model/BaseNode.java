package aug.model;

import aug.visitors.BaseAUGLabelProvider;
import edu.iastate.cs.egroum.aug.EGroumNode;

import java.util.Optional;

public abstract class BaseNode implements Node {
    private static int nextNodeId = 0;

    private final int id;
    private final int sourceLineNumber;
    private APIUsageGraph aug;

    // to build {AUGNode:EGroumNode} mapping, added by jaz
    public EGroumNode egroumNode = null;

    protected BaseNode() { this(-1); }

    protected BaseNode(int sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
        this.id = nextNodeId++;
    }

    protected BaseNode(int sourceLineNumber, EGroumNode egroumNode) {
        this.sourceLineNumber = sourceLineNumber;
        this.id = nextNodeId++;
        this.egroumNode = egroumNode;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setGraph(APIUsageGraph aug) {
        this.aug = aug;
    }

    @Override
    public APIUsageGraph getGraph() {
        return aug;
    }

    public Optional<Integer> getSourceLineNumber() {
        return sourceLineNumber > -1 ? Optional.of(sourceLineNumber) : Optional.empty();
    }

    @Override
    public Node clone() {
        try {
            return (Node) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("All nodes must be cloneable", e);
        }
    }

    @Override
    public String toString() {
        String type = getClass().getSimpleName();
        if (type.endsWith("Node")) {
            type = type.substring(0, type.length() - 4);
        }
        return type + ":" + new BaseAUGLabelProvider().getLabel(this);
    }
}
