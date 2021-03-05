package aug.builder;

import aug.model.APIUsageExample;
import aug.model.Edge;
import aug.model.Location;
import aug.model.Node;
import aug.model.actions.*;
import aug.model.controlflow.*;
import aug.model.data.*;
import aug.model.dataflow.DefinitionEdge;
import aug.model.dataflow.ParameterEdge;
import aug.model.dataflow.QualifierEdge;
import aug.model.dataflow.ReceiverEdge;
import edu.iastate.cs.egroum.aug.EGroumGraph;
import edu.iastate.cs.egroum.aug.EGroumNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class APIUsageExampleBuilder {
    private final Map<String, Node> nodeMap = new HashMap<>();
    private final Set<Edge> edges = new HashSet<>();
    private final Location location;

    public static APIUsageExampleBuilder buildAUG(Location location) {
        return new APIUsageExampleBuilder(location);
    }

    private APIUsageExampleBuilder(Location location) {
        this.location = location;
    }

    // Action Nodes

    public APIUsageExampleBuilder withArrayAccess(EGroumNode egroumNode, String nodeId, String arrayTypeName, int sourceLineNumber) {
        return withNode(nodeId, new ArrayAccessNode(arrayTypeName, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withArrayAssignment(EGroumNode egroumNode, String nodeId, String baseType, int sourceLineNumber) {
        return withNode(nodeId, new ArrayAssignmentNode(baseType, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withArrayCreation(EGroumNode egroumNode, String nodeId, String baseType, int sourceLineNumber) {
        return withNode(nodeId, new ArrayCreationNode(baseType, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withAssignment(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new AssignmentNode(sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withBreak(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new BreakNode(sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withCast(EGroumNode egroumNode, String nodeId, String targetType, int sourceLineNumber) {
        return withNode(nodeId, new CastNode(targetType, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withConstructorCall(EGroumNode egroumNode, String nodeId, String typeName, int sourceLineNumber) {
        return withNode(nodeId, new ConstructorCallNode(typeName, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withContinue(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ContinueNode(sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withInfixOperator(EGroumNode egroumNode, String nodeId, String operator, int sourceLineNumber) {
        return withNode(nodeId, new InfixOperatorNode(operator, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withMethodCall(EGroumNode egroumNode, String nodeId, String declaringTypeName, String methodSignature, int sourceLineNumber) {
        return withNode(nodeId, new MethodCallNode(declaringTypeName, methodSignature, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withNullCheck(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new NullCheckNode(sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withReturn(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ReturnNode(sourceLineNumber,egroumNode));
    }

    public APIUsageExampleBuilder withSuperConstructorCall(EGroumNode egroumNode, String nodeId, String superTypeName, int sourceLineNumber) {
        return withNode(nodeId, new SuperConstructorCallNode(superTypeName, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withSuperMethodCall(EGroumNode egroumNode, String nodeId, String declaringTypeName, String methodSignature, int sourceLineNumber) {
        return withNode(nodeId, new SuperMethodCallNode(declaringTypeName, methodSignature, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withThrow(EGroumNode egroumNode, String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ThrowNode(sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withCatch(EGroumNode egroumNode, String nodeId, String exceptionType, int sourceLineNumber) {
        return withNode(nodeId, new CatchNode(exceptionType, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withTypeCheck(EGroumNode egroumNode, String nodeId, String targetTypeName, int sourceLineNumber) {
        return withNode(nodeId, new TypeCheckNode(targetTypeName, sourceLineNumber, egroumNode));
    }

    public APIUsageExampleBuilder withUnaryOperator(EGroumNode egroumNode, String nodeId, String operator, int sourceLineNumber) {
        return withNode(nodeId, new UnaryOperatorNode(operator, sourceLineNumber, egroumNode));
    }

    // Data Nodes

    public APIUsageExampleBuilder withAnonymousClassMethod(EGroumNode egroumNode, String nodeId, String baseType, String methodSignature) {
        return withNode(nodeId, new AnonymousClassMethodNode(baseType, methodSignature, egroumNode));
    }

    public APIUsageExampleBuilder withAnonymousObject(EGroumNode egroumNode, String nodeId, String typeName) {
        return withNode(nodeId, new AnonymousObjectNode(typeName, egroumNode));
    }

    public APIUsageExampleBuilder withException(EGroumNode egroumNode, String nodeId, String typeName, String variableName) {
        return withNode(nodeId, new ExceptionNode(typeName, variableName, egroumNode));
    }

    public APIUsageExampleBuilder withLiteral(EGroumNode egroumNode, String nodeId, String typeName, String value) {
        return withNode(nodeId, new LiteralNode(typeName, value, egroumNode));
    }

    public APIUsageExampleBuilder withVariable(EGroumNode egroumNode, String nodeId, String dataTypeName, String variableName) {
        return withNode(nodeId, new VariableNode(dataTypeName, variableName, egroumNode));
    }

    public APIUsageExampleBuilder withConstant(EGroumNode egroumNode, String nodeId, String dataType, String dataName, String dataValue) {
        return withNode(nodeId, new ConstantNode(dataType, dataName, dataValue, egroumNode));
    }

    // Data-Flow Edges

    public APIUsageExampleBuilder withDefinitionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new DefinitionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withParameterEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ParameterEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withQualifierEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new QualifierEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withReceiverEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ReceiverEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    // Control-Flow Edges

    public APIUsageExampleBuilder withContainsEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ContainsEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withExceptionHandlingEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ExceptionHandlingEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withFinallyEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new FinallyEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withOrderEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new OrderEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withRepetitionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new RepetitionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSelectionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SelectionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSynchronizationEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SynchronizationEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withThrowEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ThrowEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    // added by jaz
    public APIUsageExampleBuilder withRepetitionTrueEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new RepetitionTrueEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSelectionTrueEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SelectionTrueEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withRepetitionFalseEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new RepetitionFalseEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSelectionFalseEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SelectionFalseEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    // helpers

    private APIUsageExampleBuilder withNode(String nodeId, Node node) {
        nodeMap.put(nodeId, node);
        return this;
    }

    private APIUsageExampleBuilder withEdge(Edge edge) {
        edges.add(edge);
        return this;
    }

    private Node getNode(String nodeId) {
        if (!nodeMap.containsKey(nodeId)) {
            throw new IllegalArgumentException("node with id '" + nodeId + "' does not exist");
        }
        return nodeMap.get(nodeId);
    }

    public APIUsageExample build(EGroumGraph egroum) {
        APIUsageExample aug = new APIUsageExample(location);
        aug.egroum = egroum;
        for (Node node : nodeMap.values()) {
            aug.addVertex(node);
            node.setGraph(aug);
        }
        for (Edge edge : edges) {
            aug.addEdge(edge.getSource(), edge.getTarget(), edge);
        }
        return aug;
    }

    // get {nodeId:Node} mapping of this APIUsageExample, added by jaz
    public Map<String, Node> getNodeMap() {
        return this.nodeMap;
    }
}
