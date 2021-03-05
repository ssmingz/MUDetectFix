package edu.iastate.cs.egroum.aug;

import aug.builder.APIUsageExampleBuilder;
import aug.model.*;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.iastate.cs.egroum.aug.EGroumDataEdge.Type.QUALIFIER;

public class AUGBuilder {
    private static final Logger LOGGER = Logger.getLogger(AUGBuilder.class.getSimpleName());

    private static final Set<String> ASSIGNMENT_OPERATORS = new HashSet<>();
    private static final Set<String> UNARY_OPERATORS = new HashSet<>();
    private static final Set<Integer> LITERAL_AST_NODE_TYPES = new HashSet<>();

    static {
        ASSIGNMENT_OPERATORS.add("+");
        ASSIGNMENT_OPERATORS.add("-");
        ASSIGNMENT_OPERATORS.add("*");
        ASSIGNMENT_OPERATORS.add("/");
        ASSIGNMENT_OPERATORS.add("&");
        ASSIGNMENT_OPERATORS.add("|");
        ASSIGNMENT_OPERATORS.add("^");
        ASSIGNMENT_OPERATORS.add("%");
        ASSIGNMENT_OPERATORS.add(">>");
        ASSIGNMENT_OPERATORS.add("<<");
        ASSIGNMENT_OPERATORS.add(">>>");

        UNARY_OPERATORS.add("!");
        UNARY_OPERATORS.add("+");
        UNARY_OPERATORS.add("-");

        LITERAL_AST_NODE_TYPES.add(ASTNode.BOOLEAN_LITERAL);
        LITERAL_AST_NODE_TYPES.add(ASTNode.CHARACTER_LITERAL);
        LITERAL_AST_NODE_TYPES.add(ASTNode.NULL_LITERAL);
        LITERAL_AST_NODE_TYPES.add(ASTNode.NUMBER_LITERAL);
        LITERAL_AST_NODE_TYPES.add(ASTNode.STRING_LITERAL);
        LITERAL_AST_NODE_TYPES.add(ASTNode.TYPE_LITERAL);
    }

    private final AUGConfiguration configuration;

    public AUGBuilder(AUGConfiguration configuration) {
        this.configuration = configuration;
    }

    public Collection<APIUsageExample> build(String[] sourcePaths, String[] classpaths) {
        EGroumBuilder builder = new EGroumBuilder(configuration);
        return Arrays.stream(sourcePaths)
                .flatMap(sourcePath -> builder.buildBatch(sourcePath, classpaths).stream())
                .map(this::toAUG)
                .collect(Collectors.toList());
    }

	public Collection<APIUsageExample> build(String sourcePath, String[] classpaths) {
        return build(new String[] {sourcePath}, classpaths);
    }

    public Collection<APIUsageExample> build(String source, String basePath, String projectName, String[] classpath) {
        return new EGroumBuilder(configuration).buildGroums(source, basePath, projectName, classpath).stream()
                .map(this::toAUG).collect(Collectors.toList());
    }

    private APIUsageExample toAUG(EGroumGraph groum) {
        LOGGER.info("Converting to AUG: " + groum.getFilePath() + " " + groum.getName());
        APIUsageExampleBuilder builder = APIUsageExampleBuilder.buildAUG(
                new Location(groum.getProjectName(), groum.getFilePath(), getMethodSignature(groum)));
        for (EGroumNode node : groum.getNodes()) {
            addNode(builder, node);
        }
        for (EGroumNode node : groum.getNodes()) {
            for (EGroumEdge edge : node.getInEdges()) {
                addEdge(builder, edge);
            }
        }
        return builder.build(groum);
    }

    private void addEdge(APIUsageExampleBuilder builder, EGroumEdge edge) {
        Edge newEdge = convertDirectEdge(builder, edge);
        if (!edge.isDirect()) {
            // TODO the transitive edge should not wrap its 'newEdge', but the new edge for the corresponding direct
            // edge. But neither do we know this edge here, nor whether it has even been created yet. Therefore, we
            // cheat, which is currently functionally equivalent.
            // new TransitiveEdge(newEdge.getSource(), newEdge.getTarget(), newEdge);
        }
    }

    private Edge convertDirectEdge(APIUsageExampleBuilder builder, EGroumEdge edge) {
        String sourceNodeId = getNodeId(edge.getSource());
        String targetNodeId = getNodeId(edge.getTarget());
        if (edge instanceof EGroumDataEdge) {
            EGroumDataEdge.Type type = ((EGroumDataEdge) edge).getType();
            switch (type) {
                case RECEIVER:
                    builder.withReceiverEdge(sourceNodeId, targetNodeId);
                    break;
                case PARAMETER:
                    builder.withParameterEdge(sourceNodeId, targetNodeId);
                    break;
                case ORDER:
                    builder.withOrderEdge(sourceNodeId, targetNodeId);
                    break;
                case DEFINITION:
                    builder.withDefinitionEdge(sourceNodeId, targetNodeId);
                    break;
                case QUALIFIER:
                    if (configuration.encodeQualifiers) {
                        builder.withQualifierEdge(sourceNodeId, targetNodeId);
                    }
                    break;
                case CONDITION:
                    String label = edge.getLabel();
                    switch (label) {
                        // added by jaz
                        case "sel_T":
                            builder.withSelectionTrueEdge(sourceNodeId, targetNodeId);
                            break;
                        case "rep_T":
                            builder.withRepetitionTrueEdge(sourceNodeId, targetNodeId);
                            break;
                        case "sel_F":
                            builder.withSelectionFalseEdge(sourceNodeId, targetNodeId);
                            break;
                        case "rep_F":
                            builder.withRepetitionFalseEdge(sourceNodeId, targetNodeId);
                            break;
                        case "sel":
                            builder.withSelectionEdge(sourceNodeId, targetNodeId);
                            break;
                        case "rep":
                            builder.withRepetitionEdge(sourceNodeId, targetNodeId);
                            break;
                        case "syn":
                            builder.withSynchronizationEdge(sourceNodeId, targetNodeId);
                            break;
                        case "hdl":
                            builder.withExceptionHandlingEdge(sourceNodeId, targetNodeId);
                            break;
                        default:
                            if(label.startsWith("sel_"))
                                builder.withSelectionEdge(sourceNodeId, targetNodeId);
                            else if(label.startsWith("rep_"))
                                builder.withRepetitionEdge(sourceNodeId, targetNodeId);
                            else
                                throw new IllegalArgumentException("unsupported type of condition edge: " + label);
                    }
                    break;
                case THROW:
                    builder.withThrowEdge(sourceNodeId, targetNodeId);
                    break;
                case FINALLY:
                    builder.withFinallyEdge(sourceNodeId, targetNodeId);
                    break;
                case CONTAINS:
                    builder.withContainsEdge(sourceNodeId, targetNodeId);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported edge type: " + type);
            }
            return null;
        }
        throw new IllegalArgumentException("unsupported edge: " + edge.getLabel());
    }

    private void addNode(APIUsageExampleBuilder builder, EGroumNode node) {
        String nodeId = getNodeId(node);
        if (node instanceof EGroumDataNode) {
            // drop nodes that are only connected by qualifier edges, since we also drop qualifier edges and these nodes
            // would become disconnected.
            if (!configuration.encodeQualifiers && node.getInEdges().isEmpty()
                    && node.getOutEdges().stream().allMatch(AUGBuilder::isQualifierEdge)) {
                return;
            }

            EGroumDataNode dataNode = (EGroumDataNode) node;
            String dataType = node.getDataType();
            String dataName = dataNode.getDataName();
            String dataValue = dataNode.getDataValue();
            if (dataNode.isException() || dataType.endsWith("Exception") || dataType.endsWith("Error") || dataType.equals("Throwable")) {
                // TODO there's Exception nodes in catch blocks without incoming THROW edges
                builder.withException(node, nodeId, dataType, dataName);
                return;
            } else if (node.astNodeType == ASTNode.SIMPLE_NAME) {
                builder.withVariable(node, nodeId, dataType, dataName);
                return;
            } else if (node.astNodeType == ASTNode.FIELD_ACCESS) {
                builder.withConstant(node, nodeId, dataType, dataName, dataValue);
                return;
            } else if (LITERAL_AST_NODE_TYPES.contains(node.astNodeType)) {
                if (dataName != null) {
                    builder.withConstant(node, nodeId, dataType, dataName, dataValue);
                    return;
                } else {
                    builder.withLiteral(node, nodeId, dataType, dataValue);
                    return;
                }
            } else if (node.astNodeType == ASTNode.METHOD_DECLARATION) {
                // encoding of the methods of anonymous class instances
                String[] info = splitDeclaringTypeAndSignature(dataName);
                builder.withAnonymousClassMethod(node, nodeId, info[0], info[1]);
                return;
            } else {
                builder.withAnonymousObject(node, nodeId, dataType);
                return;
            }
        } else if (node instanceof EGroumActionNode) {
            int sourceLineNumber = node.getSourceLineNumber().orElse(-1);
            String label = node.getLabel();
            if (label.startsWith("{") && label.endsWith("}")) {
                builder.withArrayCreation(node, nodeId, label.substring(1, label.length() - 1), sourceLineNumber);
                return;
            } else if (label.endsWith(".arrayget()")) {
                String[] labelParts = splitDeclaringTypeAndSignature(label);
                builder.withArrayAccess(node, nodeId, labelParts[0], sourceLineNumber);
                return;
            } else if (label.endsWith(".arrayset()")) {
                String[] labelParts = splitDeclaringTypeAndSignature(label);
                builder.withArrayAssignment(node, nodeId, labelParts[0], sourceLineNumber);
                return;
            } else if (label.equals("<nullcheck>")) {
                builder.withNullCheck(node, nodeId, sourceLineNumber);
                return;
            } else if (label.endsWith(".<catch>")) {
                builder.withCatch(node, nodeId, label.substring(0, label.length() - 8), sourceLineNumber);
                return;
            } else if (node.astNodeType == ASTNode.METHOD_INVOCATION) {
                String[] labelParts = splitDeclaringTypeAndSignature(label);
                builder.withMethodCall(node, nodeId, labelParts[0], labelParts[1], sourceLineNumber);
                return;
            } else if (node.astNodeType == ASTNode.SUPER_METHOD_INVOCATION) {
                String[] labelParts = splitDeclaringTypeAndSignature(label);
                builder.withSuperMethodCall(node, nodeId, labelParts[0], labelParts[1], sourceLineNumber);
                return;
            } else if (node.astNodeType == ASTNode.CLASS_INSTANCE_CREATION) {
                String[] labelParts = splitDeclaringTypeAndSignature(label);
                builder.withConstructorCall(node, nodeId, labelParts[0], sourceLineNumber);
                return;
            } else if (node.astNodeType == ASTNode.CONSTRUCTOR_INVOCATION) {
                /* constructor name for "this()" calls look like "Type()" */
                String typeName = label.substring(0, label.length() - 2); // remove "()"
                builder.withConstructorCall(node, nodeId, typeName, sourceLineNumber);
                return;
            } else if (node.astNodeType == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
                String typeName = label.substring(0, label.length() - 2); // remove "()" from "Supertype()"
                builder.withSuperConstructorCall(node, nodeId, typeName, sourceLineNumber);
                return;
            } else if (label.endsWith("<cast>")) {
                String targetTypeName = splitDeclaringTypeAndSignature(label)[0];
                builder.withCast(node, nodeId, targetTypeName, sourceLineNumber);
                return;
            } else if (JavaASTUtil.infixExpressionLables.containsValue(label)) {
                // TODO capture non-generalized operator
                builder.withInfixOperator(node, nodeId, label, sourceLineNumber);
                return;
            } else if (ASSIGNMENT_OPERATORS.contains(label)) {
                // this happens because we transform operators such as += and -= into and = and the respective
                // operation, but do not apply the operator abstraction afterwards, i.e., this is actually a bug
                // in the transformation.
                // TODO ensure consistent handling of operators
                builder.withInfixOperator(node, nodeId, label, sourceLineNumber);
                return;
            } else if (UNARY_OPERATORS.contains(label)) {
                builder.withUnaryOperator(node, nodeId, label, sourceLineNumber);
                return;
            } else if (label.equals("=")) {
                builder.withAssignment(node, nodeId, sourceLineNumber);
                return;
            } else if (label.endsWith("<instanceof>")) {
                String checkTypeName = splitDeclaringTypeAndSignature(label)[0];
                builder.withTypeCheck(node, nodeId, checkTypeName, sourceLineNumber);
                return;
            } else if (label.equals("break")) {
                builder.withBreak(node, nodeId, sourceLineNumber);
                return;
            } else if (label.equals("continue")) {
                builder.withContinue(node, nodeId, sourceLineNumber);
                return;
            } else if (label.equals("return")) {
                builder.withReturn(node, nodeId, sourceLineNumber);
                return;
            } else if (label.equals("throw")) {
                builder.withThrow(node, nodeId, sourceLineNumber);
                return;
            }
        }
        throw new IllegalArgumentException("unsupported node: " + node);
    }

    private static boolean isQualifierEdge(EGroumEdge edge) {
        return edge instanceof EGroumDataEdge && ((EGroumDataEdge) edge).getType() == QUALIFIER;
    }

    private static String getNodeId(EGroumNode node) {
        return Integer.toString(node.getId());
    }

    private static String[] splitDeclaringTypeAndSignature(String declaringTypeAndSignatureLabel) {
        int endOfDeclaringTypeName = declaringTypeAndSignatureLabel.lastIndexOf('.');
        return new String[]{
                declaringTypeAndSignatureLabel.substring(0, endOfDeclaringTypeName),
                declaringTypeAndSignatureLabel.substring(endOfDeclaringTypeName + 1)
        };
    }

    public static String getMethodSignature(EGroumGraph graph) {
        // Examples of graph names are:
        // - C.noParamMethod#
        // - C.method#ParamType1#ParamType2#
        // - C.method#A.B#
        // - C.I.method#
        String[] parts = graph.getName().split("#", 2);
        return toMethodName(parts[0]) + toParameterList(parts[1]);
    }

    private static String toMethodName(String qualifiedMethodName) {
        return qualifiedMethodName.substring(qualifiedMethodName.lastIndexOf(".") + 1);
    }

    private static String toParameterList(String parameterList) {
        String[] parameters = parameterList.split("#");
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = toSimpleTypeName(parameters[i]);
        }
        return "(" + String.join(", ", (CharSequence[]) parameters) + ")";
    }

    private static String toSimpleTypeName(String typeName) {
        return typeName.substring(typeName.lastIndexOf(".") + 1);
    }
}
