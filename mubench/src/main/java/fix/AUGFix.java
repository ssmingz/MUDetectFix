package fix;

import aug.model.*;
import aug.model.actions.InfixOperatorNode;
import aug.model.actions.MethodCallNode;
import aug.model.actions.NullCheckNode;
import aug.model.controlflow.*;
import aug.model.data.LiteralNode;
import aug.model.dataflow.DefinitionEdge;
import aug.model.dataflow.ParameterEdge;
import aug.model.dataflow.ReceiverEdge;
import aug.model.patterns.APIUsagePattern;
import aug.model.patterns.AggregateDataNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.tu_darmstadt.stg.mudetect.model.Overlap;
import de.tu_darmstadt.stg.mudetect.model.Violation;
import edu.iastate.cs.egroum.aug.*;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AUGFix {
    private static final Logger LOGGER = Logger.getLogger(AUGFix.class.getSimpleName());
    private static final int MAX_FIX = 500;

    private final List<Violation> violations = new ArrayList<>();
    private List<CompilationUnit> fixedTargets = new ArrayList<>();

    private Map<String, String> targetVarByPattern = new HashMap<>();
    private AST tAST;

    private Overlap anOverlap;
    private APIUsagePattern aPattern;
    private APIUsageExample aTarget;
    private CompilationUnit fixedTarget, targetRefer;
    private MethodDeclaration md = null;
    private List<Node> missingNodes;
    private BiMap<ASTNode, Node> addedTargetNodes = HashBiMap.create(), addedTargetNodes_ref = HashBiMap.create();
    private Set<ASTNode> addedNodesToBeDeleted = new HashSet<>();

    private Map<Statement, DefUseInfo> defUseInfoMap = new LinkedHashMap<>();

    private int violationId = 0;
    private int newVarId, mistakeId;
    private int failed = 0, successed = 0;

    private String OUTPUT_PREFIX = "./fixedFile/";

    public AUGFix(List<Violation> violations) {
        this.violations.addAll(violations);
    }

    public List<CompilationUnit> findFixes() {
        for(int i=0; i<this.violations.size() && i<MAX_FIX; i++) {
            String methodSig = this.violations.get(i).getOverlap().getTarget().getLocation().getMethodSignature();
            String methodName = methodSig.substring(0, methodSig.indexOf('('));
            /*
            if(!methodName.equals("PdfPKCS7")
                    && !methodName.equals("drawImage")
                    && !methodName.equals("writeCrossReferenceTable")
                    && !methodName.equals("addChild")
                    && !methodName.equals("makeBookmarkParam")) {
                continue;
            }
            if(methodName.equals("makeBookmarkParam")) {
                int j=0;
            }
            */

            if(!methodName.equals("PdfPKCS7")
                    && !methodName.equals("drawImage")
                    && !methodName.equals("writeCrossReferenceTable")
                    && !methodName.equals("addChild")
                    && !methodName.equals("makeBookmarkParam")) {
                continue;
            }

            Violation aViolation = this.violations.get(i);
            CompilationUnit fixedTarget = findFix(aViolation);
            this.fixedTargets.add(fixedTarget);
            this.violationId++;
        }
        LOGGER.info("Successful fix: " + this.successed + " Failed fix: " + this.failed);
        return this.fixedTargets;
    }

    private CompilationUnit findFix(Violation violation) {
        this.anOverlap = violation.getOverlap();
        this.aTarget = this.anOverlap.getTarget();
        this.aPattern = this.anOverlap.getPattern();
        this.newVarId = 0;
        this.mistakeId = -1;
        initialReferenceTarget();

        // Step0 : Get def-use info for each statement in target
        this.defUseInfoMap.clear();
        String methodSig = this.aTarget.getLocation().getMethodSignature();
        String methodName = methodSig.substring(0, methodSig.indexOf('('));
        for(Node tNode : this.anOverlap.getMappedTargetNodes()) {
            ASTNode tASTNode = ((BaseNode) tNode).egroumNode.getAstNode();
            if(tASTNode != null) {
                while(!(tASTNode instanceof MethodDeclaration)) {
                    tASTNode = tASTNode.getParent();
                }
                this.md = (MethodDeclaration) tASTNode;
                break;
            }
        }
        for(Object stmt : this.md.getBody().statements()) {
            this.defUseInfoMap.putAll(findDefUseInfo((Statement) stmt, this.md)); //findDefUseInfo((Statement) stmt, this.md);
        }

        // Step1 : Find variable ASTNode mappings between target and pattern
        this.targetVarByPattern = findVarMappings(this.anOverlap.getTargetNodeByPatternNode());

        // Step2 : Create all single missing nodes without setting parent
        // filter out missing data nodes
        this.missingNodes = this.anOverlap.getMissingNodes().stream().filter(p -> p instanceof ActionNode).collect(Collectors.toList());
        // adjust missingNodes
        // 第一种情况：pattern中经过函数调用得到某个值，但在target中这个值直接被定义
        // 解决：如果missingNode的def边指向的DataNode不是missing的，那么将该ActionNode从missingNodes中删除
        Set<Node> removeList = new HashSet<>();
        for(Node node : this.missingNodes) {
            for(Edge edge : this.aPattern.outgoingEdgesOf(node)) {
                if(edge instanceof DefinitionEdge && !this.anOverlap.getMissingNodes().contains(this.aPattern.getEdgeTarget(edge))) {
                    removeList.add(node);
                }
            }
        }
        this.missingNodes.removeAll(removeList);
        removeList.clear();
        // 第二种情况：target中含有不只一个pattern类似实例，导致每次匹配时可能会交叉匹配
        // 解决：把missingEdge.source加进missingNodes
        for(Edge edge : this.anOverlap.getMissingEdges()) {
            // 只有在（1）missingEdge是order类型
            if(edge instanceof OrderEdge) {
                BaseNode ps = (BaseNode) edge.getSource(), pt = (BaseNode) edge.getTarget();
                if(!this.missingNodes.contains(ps) && !this.missingNodes.contains(pt)) {
                    // （2）且source.sourceLineNumber>target.sourceLineNumber时(否则可能只是匹配上两个pattern类似实例)
                    BaseNode ts = (BaseNode) this.anOverlap.getMappedTargetNode(ps);
                    BaseNode tt = (BaseNode) this.anOverlap.getMappedTargetNode(pt);
                    if(ts != null && tt != null && ts.getSourceLineNumber().get().intValue() > tt.getSourceLineNumber().get().intValue()) {
                        this.missingNodes.add(ps);
                        // 如果是MethodCallNode，还需检查其receiver和args是否也是MethodCall
                        if(ps instanceof MethodCallNode) {
                            for(Edge iedge : this.aPattern.incomingEdgesOf(ps)) {
                                if(iedge instanceof ReceiverEdge || iedge instanceof ParameterEdge) {
                                    Node iedgeSource = iedge.getSource();
                                    if(this.aPattern.incomingNodesOf(iedgeSource).size() > 0)
                                        this.missingNodes.addAll(this.aPattern.incomingNodesOf(iedgeSource));
                                }
                            }
                        }
                   }
                }
            }
            // 第三种情况：缺少包含对应参数的操作
            // 解决：把missingEdge.target加进missingNodes
            if(edge instanceof ParameterEdge) {
                BaseNode ps = (BaseNode) edge.getSource(), pt = (BaseNode) edge.getTarget();
                if(!this.missingNodes.contains(ps) && !missingNodes.contains(pt))
                    this.missingNodes.add(edge.getTarget());
            }
            // 重新再检查一遍
            for(Node node : this.missingNodes) {
                for(Edge edge2 : this.aPattern.outgoingEdgesOf(node)) {
                    if(edge2 instanceof DefinitionEdge && !this.anOverlap.getMissingNodes().contains(this.aPattern.getEdgeTarget(edge2))) {
                        removeList.add(node);
                    }
                }
            }
            this.missingNodes.removeAll(removeList);
            removeList.clear();
        }
        // before traversing, sort nodes by id from largest to smallest (from bottom to up in AUG)
        this.missingNodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                // 如果o1-o2返回负数，则把o1放在o2前面，即升序
                return getEGroumId(o2) - getEGroumId(o1);
            }
        });
        // traverse and build ASTNode
        // recorded in map { astNode (to be added) : patternAUGNode (used to get position) }
        this.addedTargetNodes.clear();
        this.addedTargetNodes_ref.clear();
        this.addedNodesToBeDeleted.clear();
        for(Node mNode : this.missingNodes) {
            this.addedTargetNodes.put(buildTargetNode(mNode), mNode);
        }
        String logPrefix = "";
        // 没有missing的ActionNode
        if(this.addedTargetNodes.isEmpty()) {
            logPrefix += "Skip fix for no missing ActionNode in Violation#";
            LOGGER.info(logPrefix + this.violationId
                    + " in [target] " + this.aTarget.getLocation().getFilePath() + " "
                    + this.aTarget.getLocation().getMethodSignature());
            return fixedTarget;
        }
        // Step3 : Insert new astNodes to fixedTarget
        Set<Edge> missingEdges = this.anOverlap.getMissingEdges();
        this.addedTargetNodes_ref.putAll(this.addedTargetNodes); // copy to reference
        Iterator nodeIt = this.addedTargetNodes.keySet().iterator();
        while(nodeIt.hasNext()) {
            ASTNode iNode = (ASTNode) nodeIt.next();
            if(this.addedNodesToBeDeleted.contains(iNode)) continue;
            insertNewASTNode(iNode);
            ASTNode cursor = iNode;
            while(!(cursor instanceof Statement) && cursor != null) {
                cursor = cursor.getParent();
            }
            if(cursor == null) continue;
            findDefUseInfo((Statement) cursor, md);
            if(!this.addedNodesToBeDeleted.contains(iNode))
                nodeIt.remove();
        }
        Set<ASTNode> restNodes = this.addedTargetNodes.keySet().stream()
                .filter(p -> !this.addedNodesToBeDeleted.contains(p)).collect(Collectors.toSet());
        nodeIt = restNodes.iterator();
        while(nodeIt.hasNext()) {
            ASTNode iNode = (ASTNode) nodeIt.next();
            insertNewASTNode(iNode);
            ASTNode cursor = iNode;
            while(!(cursor instanceof Statement) && cursor != null) {
                cursor = cursor.getParent();
            }
            if(cursor == null) continue;
            findDefUseInfo((Statement) cursor, md);
        }
        if(mistakeId == -1) {
            logPrefix += "Successfully fix in Violation#";
            generateFixedFile(fixedTarget);
            this.successed++;
        }
        else {
            logPrefix += "Failed fix in Violation#";
            this.failed++;
        }
        LOGGER.info(logPrefix + this.violationId
                + " in [target] " + this.aTarget.getLocation().getFilePath() + " "
                + this.aTarget.getLocation().getMethodSignature());
        return fixedTarget;
    }

    private void generateFixedFile(CompilationUnit fix) {
        String outputPath = this.OUTPUT_PREFIX + this.violationId + "_"
                + this.aTarget.getLocation().getMethodSignature().substring(0, this.aTarget.getLocation().getMethodSignature().indexOf('('))
                + ".java";
        File outputFile = new File(outputPath);
        try (FileOutputStream fop = new FileOutputStream(outputFile)) {
            outputFile.createNewFile();
            byte[] contentInBytes = fix.toString().getBytes(); // get the content in bytes
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
            System.out.println("Generate fix " + outputPath + " done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialReferenceTarget() {
        // 创建一个新的空的AST
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource("".toCharArray());
        CompilationUnit newCU = (CompilationUnit) parser.createAST(null);
        newCU.recordModifications();
        // copy target ASTNodes to newCU
        for(Node aNode : this.aTarget.vertexSet()) {
            if(aNode instanceof ActionNode && ((BaseNode) aNode).egroumNode.getAstNode() != null) {
                CompilationUnit originCU = (CompilationUnit) ((BaseNode) aNode).egroumNode.getAstNode().getRoot();
                this.tAST = originCU.getAST();
                newCU = (CompilationUnit) ASTNode.copySubtree(newCU.getAST(), originCU);
                this.fixedTarget = originCU;
                this.targetRefer = newCU;
                return ;
            }
        }
    }

    private Map<String, String> findVarMappings(Map<Node, Node> targetNodeByPatternNode) {
        ASTNodeMapping astNodeMap = new ASTNodeMapping(targetNodeByPatternNode, this.anOverlap);
        astNodeMap.findVarMappings();
        return astNodeMap.targetVarByPatternVar;
    }

    private void insertNewASTNode(ASTNode tNode) {
        // 第一种情况：在某种结构内缺少语句
        Iterator iedgeIt = this.aPattern.incomingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(iedgeIt.hasNext()) {
            Edge iedge = (Edge) iedgeIt.next();
            // in a finally block
            if(iedge instanceof FinallyEdge) {
                insertAtFinally(tNode);
                this.addedNodesToBeDeleted.add(tNode);
                return ;
            }
            // in a catch block
            else if(iedge instanceof ExceptionHandlingEdge) {
                insertAtCatch(tNode);
                this.addedNodesToBeDeleted.add(tNode);
                return ;
            }
        }
        // replace NOTEXIST declaration
        if(tNode instanceof Assignment && ((Assignment) tNode).getLeftHandSide().toString().equals("NOTEXIST")) {
            for(ASTNode astnode : this.addedTargetNodes.keySet()) {
                replaceNOTEXIST(astnode, (Assignment) tNode);
            }
            for(ASTNode astnode : this.addedNodesToBeDeleted) {
                replaceNOTEXIST(astnode, (Assignment) tNode);
            }
            return;
        }
        // 第二种情况：缺少判断条件
        Iterator oedgeIt = this.aPattern.outgoingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(oedgeIt.hasNext()) {
            Edge oedge = (Edge) oedgeIt.next();
            // in for/while stmt
            if(oedge instanceof RepetitionEdge && isDirectCond(tNode, oedge.getTarget(), this.aPattern)) {
                insertAsLoop(tNode);
                return ;
            }
            // in if stmt
            else if(oedge instanceof SelectionEdge && isDirectCond(tNode, oedge.getTarget(), this.aPattern)) {
                insertAsIf(tNode);
                return ;
            }
        }
        // 第三种情况：单纯地缺少语句
        insertSimpleStmt(tNode);
        //this.addedNodesToBeDeleted.add(tNode); // move this step into insertSimpleStmt() method
    }

    private void replaceNOTEXIST(ASTNode target, Assignment neAssign) {
        if(target instanceof InfixExpression) {
            // replace left
            if(((InfixExpression) target).getLeftOperand().toString().equals("NOTEXIST")) {
                ((InfixExpression) target).setLeftOperand((Expression) ASTNode.copySubtree(this.tAST, neAssign.getRightHandSide()));
            }
            // or replace right
            else if(((InfixExpression) target).getRightOperand().toString().equals("NOTEXIST")) {
                ((InfixExpression) target).setRightOperand((Expression) ASTNode.copySubtree(this.tAST, neAssign.getRightHandSide()));
            }
        }
    }

    private void insertSimpleStmt(ASTNode tNode) {
        // 构造语句节点
        Statement newStmt = createStatementNode(this.addedTargetNodes.get(tNode), tNode);
        // 直接找到最近一个匹配的节点然后插入
        Node pNode = this.addedTargetNodes.get(tNode);
        Node mappedNode = findNearestMappedNode(pNode);
        // 默认如果未找到匹配节点直接跳过
        if(mappedNode == null) {
            return;
        }
        // 找到已匹配节点所在block
        ASTNode originBlock = null, mappedStmt = null;
        if(!this.missingNodes.contains(mappedNode)) {
            originBlock = ((BaseNode) mappedNode).egroumNode.getAstNode();
            mappedStmt = ((BaseNode) mappedNode).egroumNode.getAstNode();
        }
        else {
            originBlock = this.addedTargetNodes_ref.inverse().get(mappedNode);
            mappedStmt = this.addedTargetNodes_ref.inverse().get(mappedNode);
        }
        while(!(mappedStmt instanceof Statement)) {
            if(mappedStmt==null) {
                int i=1;
            }
            mappedStmt = mappedStmt.getParent();
        }
        originBlock = mappedStmt.getParent();
        // 如果then/else/for内只有一行语句且没被括住，那么originBlock为IfStmt节点
        if(!(originBlock instanceof Block)) {
            Block newBlock = this.tAST.newBlock();
            if(originBlock instanceof IfStatement) {
                if(isInThenStmt((IfStatement) originBlock, mappedStmt))
                    ((IfStatement) originBlock).setThenStatement(newBlock);
                else
                    ((IfStatement) originBlock).setElseStatement(newBlock);
                newBlock.statements().add(mappedStmt);
                originBlock = newBlock;
            }
            else if(originBlock instanceof ForStatement) {
                ((ForStatement) originBlock).setBody(newBlock);
                newBlock.statements().add(mappedStmt);
                originBlock = newBlock;
            }
        }
        int mappedIndex;
        List<ASTNode> statements;
        if(originBlock instanceof SwitchStatement) {
            statements = ((SwitchStatement) originBlock).statements();
        }
        else {
            statements = ((Block) originBlock).statements();
        }
        mappedIndex = statements.indexOf(mappedStmt);
        // 该已匹配节点在待插入节点的前面，则在其之后插入
        if(getEGroumId(mappedNode) < getEGroumId(pNode)) {
            // filter part
            Map<Statement, DefUseInfo> newInsert = findDefUseInfo(newStmt, this.md);
            Set<String> newDef = newInsert.get(newStmt).def;
            boolean isMeaningful = false, findMapped = false;
            for(Map.Entry<Statement, DefUseInfo> entry : this.defUseInfoMap.entrySet()) {
                if(entry.getKey() == statements.get(mappedIndex+1)) {
                    findMapped = true;
                }
                if(findMapped) {
                    Set<String> originUse = entry.getValue().use;
                    for(String def : newDef) {
                        if(originUse.contains(def)) {
                            isMeaningful = true;
                            break;
                        }
                    }
                }
                if(isMeaningful) {
                    break;
                }
            }
            if(isMeaningful) {
                statements.add(mappedIndex + 1, newStmt);
                this.addedNodesToBeDeleted.add(tNode);
            }
        }
        // 在后面，则在其之前插入
        else {
            // filter part
            Map<Statement, DefUseInfo> newInsert = findDefUseInfo(newStmt, this.md);
            Set<String> newDef = newInsert.get(newStmt).def;
            boolean isMeaningful = false, findMapped = false;
            for(Map.Entry<Statement, DefUseInfo> entry : this.defUseInfoMap.entrySet()) {
                if(entry.getKey() == statements.get(mappedIndex)) {
                    findMapped = true;
                }
                if(findMapped) {
                    Set<String> originUse = entry.getValue().use;
                    for(String def : newDef) {
                        if(originUse.contains(def)) {
                            isMeaningful = true;
                            break;
                        }
                    }
                }
                if(isMeaningful) {
                    break;
                }
            }
            if(isMeaningful) {
                statements.add(mappedIndex, newStmt);
                this.addedNodesToBeDeleted.add(tNode);
            }
        }
    }

    /*
    * Return the nearest mapped AUGNode in target
    * */
    private Node findNearestMappedNode(Node pNode) {
        Node mappedNode1 = null, mappedNode2 = null;
        int minDist1 = Integer.MAX_VALUE, minDist2 = Integer.MAX_VALUE;
        // 检查原有的
        for(Node node : this.anOverlap.getTargetNodeByPatternNode().keySet()) { // traverse mapped pattern nodes
            if(node instanceof ActionNode && !this.missingNodes.contains(node)) {
                int dist = Math.abs(getEGroumId(pNode) - getEGroumId(node));
                if(dist<minDist1) {
                    minDist1 = dist;
                    mappedNode1 = node;
                }
            }
        }
        // 检查新创建的
        for(ASTNode astNode : this.addedNodesToBeDeleted) {
            Node node = this.addedTargetNodes.get(astNode);
            int dist = Math.abs(getEGroumId(pNode) - getEGroumId(node));
            if(dist<minDist2) {
                minDist2 = dist;
                mappedNode2 = node;
            }
        }
        if(minDist1<=minDist2)
            return this.anOverlap.getMappedTargetNode(mappedNode1);
        else
            return mappedNode2;
    }

    private void insertAsIf(ASTNode tNode) {
        Edge cedge = null;
        Iterator edgeIt = this.aPattern.outgoingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(edgeIt.hasNext()) {
            Edge oedge = (Edge) edgeIt.next();
            if(oedge instanceof SelectionEdge) {
                cedge = oedge;
                break;
            }
        }
        // 首先判断哪个是直接相关的条件节点
        Set<ASTNode> removeList = new HashSet<>();
        Node directCon = this.addedTargetNodes.get(tNode);
        for(Edge iedge : this.aPattern.incomingEdgesOf(cedge.getTarget())) {
            if(iedge instanceof SelectionEdge) {
                if(getEGroumId(iedge.getSource()) > getEGroumId(directCon)) {
                    directCon = iedge.getSource();
                }
            }
        }
        // 如果是间接条件，先跳过
        // TODO 还需处理If条件-elseIf条件之间关系，目前程序认为elseIf条件比If条件更direct，这会导致最终不会处理If条件
        if(((BaseNode) directCon).egroumNode.getSourceLineNumber().get().intValue()
                == ((BaseNode) this.addedTargetNodes.get(tNode)).egroumNode.getSourceLineNumber().get().intValue()) {
            directCon = this.addedTargetNodes.get(tNode);
        }
        if(directCon != this.addedTargetNodes.get(tNode)) {
            return ;
        }
        //this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(this.addedTargetNodes.get(tNode)));
        // 创建新条件
        Expression newCon = (Expression) addedTargetNodes_ref.inverse().get(directCon);
        // 首先判断函数返回类型
        String returnType = getReturnType(this.aTarget);
        // 找到原条件控制的T分支的所有节点
        List<Node> tBranchNodes = new ArrayList<>();
        EGroumNode controlNode = ((BaseNode) directCon).egroumNode;
        for(Edge oedge :  this.aPattern.outgoingEdgesOf(directCon).stream()
                .filter(p -> p instanceof SelectionEdge).collect(Collectors.toSet())) {
            if(((SelectionEdge) oedge).conditionType == ConditionEdge.ConditionType.SELECTION_T) {
                tBranchNodes.add(oedge.getTarget());
            }
        }
        tBranchNodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return getEGroumId(o1) - getEGroumId(o2);
            }
        });
        Node mappedNode = null;
        for(Node node : tBranchNodes) {
            // missingNodes包含要添加的ActionNodes，是所有缺失节点的一部分，所以!contains包括已匹配节点和部分缺失节点
            // addedNodesToBeDeleted包含已添加的ActionNodes，表示之前缺失的现在已匹配
            // 所有节点分为missing和mapped两类，this.missingNodes在两类都有涉及，实际匹配的是mapped中去除this.missingNodes的
            if ( (this.anOverlap.getTargetNodeByPatternNode().keySet().contains(node)
                    && !this.missingNodes.contains(node))
                    || this.addedNodesToBeDeleted.contains(this.addedTargetNodes_ref.inverse().get(node))) {
                mappedNode = node;
                break;
            }
        }
        if(mappedNode != null) {
            // 找到最近的mappedNode
            Node nearestMapped = findNearestMappedNode(this.addedTargetNodes.get(tNode));
            for(Node key: this.anOverlap.getTargetNodeByPatternNode().keySet()) {
                if(this.anOverlap.getMappedTargetNode(key) == nearestMapped) {
                    nearestMapped = key;
                    break;
                }
            }
            if( getEGroumId(nearestMapped) > getEGroumId(this.addedTargetNodes.get(tNode)) ) {
                mappedNode = nearestMapped;
            }
            // 找到该匹配语句的所在block
            Node targetNode = this.anOverlap.getMappedTargetNode(mappedNode);
            ASTNode originBlock = null, mappedStmt = null;
            if(targetNode != null) {
                originBlock = ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)).egroumNode.getAstNode();
                mappedStmt = ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)).egroumNode.getAstNode();
                if(mappedStmt == null) {
                    EGroumNode notNull = ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)).egroumNode.control;
                    mappedStmt = notNull.getAstNode();
                    originBlock = notNull.getAstNode();
                }
            }
            else {
                originBlock = this.addedTargetNodes_ref.inverse().get(mappedNode);
                mappedStmt = this.addedTargetNodes_ref.inverse().get(mappedNode);
            }
            while (!(mappedStmt instanceof Statement)) {
                mappedStmt = mappedStmt.getParent();
            }
            originBlock = mappedStmt.getParent();
            if(originBlock instanceof IfStatement) {
                Block newIfBlock = this.tAST.newBlock();
                if(((IfStatement) originBlock).getThenStatement() == mappedStmt)
                    ((IfStatement) originBlock).setThenStatement(newIfBlock);
                else
                    ((IfStatement) originBlock).setElseStatement(newIfBlock);
                newIfBlock.statements().add(mappedStmt);
                originBlock = newIfBlock;
            }
            int mappedIndex;
            List<ASTNode> statements;
            if(originBlock instanceof SwitchStatement) {
                statements = ((SwitchStatement) originBlock).statements();
            }
            else {
                statements = ((Block) originBlock).statements();
            }
            mappedIndex = statements.indexOf(mappedStmt);
            // find T branch nodes in target
            Set<Statement> targetTBranch = getBranchStatements(tBranchNodes);
            // 如果是构造函数
            if(returnType.equals("isConstructor")) {
                // 如果T分支内存在已匹配的语句，那么if括住它之后的所有语句
                IfStatement newIfStmt = this.tAST.newIfStatement();
                newIfStmt.setExpression(newCon);
                this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(this.addedTargetNodes.get(tNode)));
                Block newIfBody = this.tAST.newBlock();
                Block newElseBody = this.tAST.newBlock();
                // find statements irrelevant to pattern-then-stmts in target
                Set<Statement> newElseBlock = findIrrelevantStmts(targetTBranch, (Statement) mappedStmt);
                newIfStmt.setThenStatement(newIfBody);
                newIfStmt.setElseStatement(newElseBody);
                Iterator stmtIt = statements.subList(mappedIndex, ((Block) originBlock).statements().size() - 1).iterator();
                while (stmtIt.hasNext()) {
                    Statement stmt = (Statement) stmtIt.next();
                    stmt.delete();
                    newIfBody.statements().add(stmt);
                }
                for(Statement stmt : newElseBlock) {
                    Statement newStmt = (Statement) ASTNode.copySubtree(this.tAST, stmt);
                    newElseBody.statements().add(newStmt);
                }
                statements.add(newIfStmt);
                return ;
            }
            // 如果不是构造函数
            else {
                // 如果pattern中if控制的T分支的第一个节点未匹配那么先创建这个节点再插入if
                IfStatement newIfStmt = this.tAST.newIfStatement();
                ParenthesizedExpression parenthesizedExp = this.tAST.newParenthesizedExpression();
                parenthesizedExp.setExpression(newCon);
                PrefixExpression rltExp = this.tAST.newPrefixExpression(); // if-exp is (!condition)
                rltExp.setOperator(PrefixExpression.Operator.NOT);
                rltExp.setOperand(parenthesizedExp);
                newIfStmt.setExpression(rltExp);
                this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(this.addedTargetNodes.get(tNode)));
                Block newIfBody = this.tAST.newBlock(); // then-body
                // find statements irrelevant to pattern-then-stmts in target
                Set<Statement> newThenBlock = findIrrelevantStmts(targetTBranch, (Statement) mappedStmt);
                newIfStmt.setThenStatement(newIfBody);
                for(Statement stmt : newThenBlock) {
                    //stmt.delete();
                    Statement newStmt = (Statement) ASTNode.copySubtree(this.tAST, stmt);
                    newIfBody.statements().add(newStmt);
                }
                int newIfBodySize = newIfBody.statements().size();
                if((newIfBodySize > 0 && !(newIfBody.statements().get(newIfBodySize-1) instanceof ReturnStatement))
                        || newIfBodySize == 0) {
                    ReturnStatement defaultReturn = createDefaultReturn(returnType);
                    newIfBody.statements().add(defaultReturn);
                }
                // insert if stmt
                statements.add(mappedIndex, newIfStmt);
                // update defUseInfoMap
                updateDefUseInfo(newIfStmt, this.md, mappedStmt);
                return ;
            }
        }
        // 如果不存在已匹配的，那么跳过此节点，先创建这些分支内的节点再回来创建if
        /*
        else {
            this.addedNodesToBeDeleted.remove(this.addedTargetNodes_ref.inverse().get(directCon));
            return ;
        }
        */
    }

    private void updateDefUseInfo(Statement newStmt, MethodDeclaration methodDecl, ASTNode mappedStmt) {
        Map<Statement, DefUseInfo> result = new LinkedHashMap<>();
        Map<Statement, DefUseInfo> newPart = findDefUseInfo(newStmt, methodDecl);
        for(Map.Entry<Statement, DefUseInfo> entry : this.defUseInfoMap.entrySet()) {
            if(entry.getKey() == mappedStmt) {
                result.putAll(newPart);
            }
            result.put(entry.getKey(), entry.getValue());
        }
        this.defUseInfoMap.clear();
        this.defUseInfoMap.putAll(result);
    }

    private Set<Statement> findIrrelevantStmts(Set<Statement> targetTBranch, Statement insertPos) {
        Set<Statement> irrelevantStmts = new LinkedHashSet<>();
        // first get set D (all defs in targetTBranch)
        Set<String> defs = new HashSet<>();
        for(Statement stmt : targetTBranch) {
            defs.addAll(this.defUseInfoMap.get(stmt).def);
        }
        // then check each statement after the target insert position, be irrelevant if its use doesn't intersect with D
        boolean flag = false;
        Set<String> preUse = new LinkedHashSet<>();
        for(Map.Entry<Statement, DefUseInfo> entry: this.defUseInfoMap.entrySet()) {
            if(entry.getKey() == insertPos) {
                flag = true;
                if(entry.getKey() instanceof IfStatement || entry.getKey() instanceof WhileStatement)
                    preUse.addAll(entry.getValue().use);
                continue;
            }
            if(flag == false) continue;
            boolean canBeAdded = true;
            for(String use : entry.getValue().use) {
                if(defs.contains(use) || (preUse.size()>0 && preUse.contains(use))) { // statement can be added condition 1: don't use any def
                    canBeAdded = false;
                    break;
                }
                else {
                    ASTNode cursor = entry.getKey();
                    boolean repeat = false;
                    while(!(cursor instanceof MethodDeclaration)) { // condition 2: not child of any added stmt
                        if(irrelevantStmts.contains(cursor)) {
                            repeat = true;
                            break;
                        }
                        cursor = cursor.getParent();
                    }
                    if(repeat) {
                        canBeAdded = false;
                        break;
                    }
                }
            }
            if(canBeAdded) {
                if(entry.getKey() instanceof Block) {
                    irrelevantStmts.addAll(((Block) entry.getKey()).statements());
                    int size = ((Block) entry.getKey()).statements().size();
                    if(((Block) entry.getKey()).statements().get(size-1) instanceof ReturnStatement)
                        break;
                }
                else {
                    irrelevantStmts.add(entry.getKey());
                    if(entry.getKey() instanceof ReturnStatement)
                        break;
                }
            }
        }
        return irrelevantStmts;
    }

    private Set<Statement> getBranchStatements(List<Node> tBranchNodes) {
        Set<Statement> targetBranch = new LinkedHashSet<>();
        for(Node pNode : tBranchNodes) {
            if(!this.anOverlap.getMissingNodes().contains(pNode)) { // mapped
                Node tNode = this.anOverlap.getMappedTargetNode(pNode);
                ASTNode tASTNode = ((BaseNode) tNode).egroumNode.getAstNode();
                if(tASTNode != null) {
                    while(!(tASTNode instanceof Statement)) {
                        tASTNode = tASTNode.getParent();
                    }
                    targetBranch.add((Statement) tASTNode);
                }
            }
            else if(this.addedNodesToBeDeleted.contains(this.addedTargetNodes_ref.inverse().get(pNode))) {
                ASTNode tASTNode = this.addedTargetNodes_ref.inverse().get(pNode);
                while(!(tASTNode instanceof Statement)) {
                    tASTNode = tASTNode.getParent();
                }
                targetBranch.add((Statement) tASTNode);
            }
        }
        return targetBranch;
    }

    private void insertAsLoop(ASTNode tNode) {
        Edge cedge = null;
        Iterator edgeIt = this.aPattern.outgoingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(edgeIt.hasNext()) {
            Edge oedge = (Edge) edgeIt.next();
            if(oedge instanceof RepetitionEdge) {
                cedge = oedge;
                break;
            }
        }
        Set<ASTNode> removeList = new HashSet<>();
        // 首先判断哪个是直接相关的条件节点
        Node directCon = this.addedTargetNodes.get(tNode);
        for(Edge iedge : this.aPattern.incomingEdgesOf(cedge.getTarget())) {
            if(iedge instanceof RepetitionEdge) {
                if(getEGroumId(iedge.getSource()) > getEGroumId(directCon)) {
                    directCon = iedge.getSource();
                }
            }
        }
        if(((BaseNode) directCon).egroumNode.getSourceLineNumber()
                == ((BaseNode) this.addedTargetNodes.get(tNode)).egroumNode.getSourceLineNumber()) {
            directCon = this.addedTargetNodes.get(tNode);
        }
        if(directCon != this.addedTargetNodes.get(tNode)) {
            return ;
        }
        //this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(directCon));
        // 检查rep边的终点所在结构是什么
        boolean isInLoop = false;
        ASTNode cursor = null; //表示target中的
        for(Edge oedge : this.aPattern.outgoingEdgesOf(cedge.getSource())) {
            if(oedge instanceof RepetitionEdge) {
                if(!this.missingNodes.contains(oedge.getTarget())
                        || this.addedNodesToBeDeleted.contains(this.addedTargetNodes_ref.inverse().get(oedge.getTarget()))) {
                    Node targetNode = this.anOverlap.getMappedTargetNode(oedge.getTarget());
                    if(targetNode != null)
                        cursor = ((BaseNode) targetNode).egroumNode.getAstNode();
                    else
                        cursor = this.addedTargetNodes_ref.inverse().get(oedge.getTarget());
                    if(cursor != null) {
                        while(!(cursor instanceof WhileStatement) && !(cursor instanceof ForStatement) && cursor != null) {
                            cursor = cursor.getParent();
                            if(cursor instanceof MethodDeclaration) break;
                        }
                        if(cursor instanceof WhileStatement || cursor instanceof ForStatement)
                            isInLoop = true;
                    }
                }
            }
        }
        // 创建新条件
        Expression newCon = (Expression) this.addedTargetNodes_ref.inverse().get(directCon);
        // 在循环结构中，那么直接在循环条件中插入
        if(isInLoop) {
            // 创建合并后的新条件，'&&'连接
            InfixExpression rltExp = this.tAST.newInfixExpression();
            rltExp.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
            // right operand
            ParenthesizedExpression parenthesizedExp1 = this.tAST.newParenthesizedExpression();
            parenthesizedExp1.setExpression(newCon);
            rltExp.setRightOperand(parenthesizedExp1);
            // left operand
            if(cursor instanceof WhileStatement) {
                Expression lExp = (Expression) ASTNode.copySubtree(this.tAST, ((WhileStatement) cursor).getExpression());
                ((WhileStatement) cursor).setExpression(rltExp);
                ParenthesizedExpression parenthesizedExp0 = this.tAST.newParenthesizedExpression();
                parenthesizedExp0.setExpression(lExp);
                rltExp.setLeftOperand(parenthesizedExp0);
            }
            else if(cursor instanceof ForStatement) {
                Expression lExp = (Expression) ASTNode.copySubtree(this.tAST, ((ForStatement) cursor).getExpression());
                ((ForStatement) cursor).setExpression(rltExp);
                ParenthesizedExpression parenthesizedExp0 = this.tAST.newParenthesizedExpression();
                parenthesizedExp0.setExpression(lExp);
                rltExp.setLeftOperand(parenthesizedExp0);
            }
            this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(directCon));
            return ;
        }
        // 不在循环结构中
        else {
            // 首先判断函数返回类型
            String returnType = getReturnType(this.aTarget);
            // 找到原条件控制的T分支的所有节点
            List<Node> tBranchNodes = new ArrayList<>();
            for(Edge oedge :  this.aPattern.outgoingEdgesOf(directCon).stream()
                    .filter(p -> p instanceof RepetitionEdge).collect(Collectors.toSet())) {
                // 判断是否在T分支
                if(((RepetitionEdge) oedge).conditionType == ConditionEdge.ConditionType.REPETITION_T) {
                    tBranchNodes.add(oedge.getTarget());
                }
            }
            tBranchNodes.sort(new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return getEGroumId(o1) - getEGroumId(o2);
                }
            });
            Node mappedNode = null;
            for(Node node : tBranchNodes) {
                if( (this.anOverlap.getTargetNodeByPatternNode().keySet().contains(node)
                        && !this.missingNodes.contains(node))
                        || this.addedNodesToBeDeleted.contains(this.addedTargetNodes_ref.inverse().get(node))) {
                    mappedNode = node;
                    break;
                }
            }
            if(mappedNode != null) {
                // 找到该匹配语句的所在block
                ASTNode originBlock = ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)) == null ? this.addedTargetNodes_ref.inverse().get(mappedNode) : ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)).egroumNode.getAstNode();
                ASTNode mappedStmt = ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)) == null ? this.addedTargetNodes_ref.inverse().get(mappedNode) : ((BaseNode) this.anOverlap.getMappedTargetNode(mappedNode)).egroumNode.getAstNode();
                while (!(mappedStmt instanceof Statement)) {
                    mappedStmt = mappedStmt.getParent();
                }
                originBlock = mappedStmt.getParent();
                // 如果then/else/for内只有一行语句且没被括住，那么originBlock为IfStmt节点
                if(!(originBlock instanceof Block)) {
                    Block newBlock = this.tAST.newBlock();
                    if(originBlock instanceof IfStatement) {
                        if(isInThenStmt((IfStatement) originBlock, mappedStmt))
                            ((IfStatement) originBlock).setThenStatement(newBlock);
                        else
                            ((IfStatement) originBlock).setElseStatement(newBlock);
                        newBlock.statements().add(mappedStmt);
                        originBlock = newBlock;
                    }
                    else if(originBlock instanceof ForStatement) {
                        ((ForStatement) originBlock).setBody(newBlock);
                        newBlock.statements().add(mappedStmt);
                        originBlock = newBlock;
                    }
                }
                int mappedIndex = ((Block) originBlock).statements().indexOf(mappedStmt);
                // find T branch nodes in target
                Set<Statement> targetTBranch = getBranchStatements(tBranchNodes);
                // 如果是构造函数
                if(returnType.equals("isConstructor")) {
                    // 如果T分支内存在已匹配的语句，那么if括住它之后的所有语句
                    IfStatement newIfStmt = this.tAST.newIfStatement();
                    newIfStmt.setExpression(newCon);
                    this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(directCon));
                    Block newIfBody = this.tAST.newBlock();
                    Block newElseBody = this.tAST.newBlock();
                    // find statements irrelevant to pattern-then-stmts in target
                    Set<Statement> newElseBlock = findIrrelevantStmts(targetTBranch, (Statement) mappedStmt);
                    newIfStmt.setThenStatement(newIfBody);
                    newIfStmt.setElseStatement(newElseBody);
                    Iterator stmtIt = ((Block) originBlock).statements().subList(mappedIndex, ((Block) originBlock).statements().size()).iterator();
                    Set<Statement> newThenBlock = new LinkedHashSet<>();
                    while (stmtIt.hasNext()) {
                        Statement stmt = (Statement) stmtIt.next();
                        newThenBlock.add(stmt);
                        //stmt.delete();
                        //newIfBody.statements().add(stmt);
                    }
                    for(Statement stmt : newThenBlock) {
                        stmt.delete();
                        newIfBody.statements().add(stmt);
                    }
                    for(Statement stmt : newElseBlock) {
                        Statement newStmt = (Statement) ASTNode.copySubtree(this.tAST, stmt);
                        newElseBody.statements().add(newStmt);
                    }
                    ((Block) originBlock).statements().add(newIfStmt);
                    return ;
                }
                // 如果不是构造函数
                else {
                    // 如果pattern中if控制的T分支的第一个节点未匹配那么先创建这个节点再插入if
                    IfStatement newIfStmt = this.tAST.newIfStatement();
                    PrefixExpression rltExp = this.tAST.newPrefixExpression(); // if-exp is (!condition)
                    rltExp.setOperator(PrefixExpression.Operator.NOT);
                    ParenthesizedExpression parenthesizedExp = this.tAST.newParenthesizedExpression();
                    parenthesizedExp.setExpression(newCon);
                    rltExp.setOperand(parenthesizedExp);
                    newIfStmt.setExpression(rltExp);
                    this.addedNodesToBeDeleted.add(this.addedTargetNodes_ref.inverse().get(directCon));
                    Block newIfBody = this.tAST.newBlock(); // then-body
                    // find statements irrelevant to pattern-then-stmts in target
                    Set<Statement> newThenBlock = findIrrelevantStmts(targetTBranch, (Statement) mappedStmt);
                    newIfStmt.setThenStatement(newIfBody);
                    for(Statement stmt : newThenBlock) {
                        //stmt.delete();
                        Statement newStmt = (Statement) ASTNode.copySubtree(this.tAST, stmt);
                        newIfBody.statements().add(newStmt);
                    }
                    int newIfBodySize = newIfBody.statements().size();
                    if((newIfBodySize > 0 && !(newIfBody.statements().get(newIfBodySize-1) instanceof ReturnStatement))
                            || newIfBodySize == 0) {
                        ReturnStatement defaultReturn = createDefaultReturn(returnType);
                        newIfBody.statements().add(defaultReturn);
                    }
                    // insert if stmt
                    ((Block) originBlock).statements().add(mappedIndex, newIfStmt);
                    // update defUseInfoMap
                    updateDefUseInfo(newIfStmt, this.md, mappedStmt);
                    return ;
                }
            }
            // 如果不存在已匹配的，那么跳过此节点，先创建这些分支内的节点再回来创建if
            else {
                return ;
            }
        }
    }

    private ReturnStatement createDefaultReturn(String returnType) {
        ReturnStatement newReturnStmt = this.tAST.newReturnStatement();

        if(Character.isUpperCase(returnType.charAt(0))) { // isClass, default "return null;"
            NullLiteral content = this.tAST.newNullLiteral();
            newReturnStmt.setExpression(content);
        }
        else if(returnType.equals("void")) { } // default "return ;"
        else if(returnType.equals("boolean")) { // default "return false;"
            BooleanLiteral content = this.tAST.newBooleanLiteral(false);
            newReturnStmt.setExpression(content);
        }
        else if(returnType.equals("int")) { // default "return 0;"
            NumberLiteral content = this.tAST.newNumberLiteral("0");
            newReturnStmt.setExpression(content);
        }
        else if(returnType.equals("char")) { // default "return '#';"
            CharacterLiteral content = this.tAST.newCharacterLiteral();
            content.setCharValue('#');
            newReturnStmt.setExpression(content);
        }
        return newReturnStmt;
    }

    private void insertAtCatch(ASTNode tNode) {
        Edge cedge = null;
        Iterator edgeIt = this.aPattern.incomingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(edgeIt.hasNext()) {
            Edge iedge = (Edge) edgeIt.next();
            if(iedge instanceof ExceptionHandlingEdge) {
                cedge = iedge;
                break;
            }
        }
        // 首先判断是否已存在该catch结构
        // 遍历得到pattern中的catch块内所有语句
        Node csource = cedge.getSource();
        List<Node> catchNodes = new ArrayList<>();
        for(Edge oedge : this.aPattern.outgoingEdgesOf(csource)) {
            if(oedge instanceof ExceptionHandlingEdge)
                catchNodes.add(oedge.getTarget());
        }
        catchNodes.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return getEGroumId(o1) - getEGroumId(o2);
            }
        });
        // 若存在catch结构
        if(!this.missingNodes.contains(csource)) {
            // 找到target中的该catchclause
            Node targetCatch = this.anOverlap.getMappedTargetNode(csource);
            CatchClause catchClause = null;
            for(Edge iedge : this.aTarget.incomingEdgesOf(targetCatch)) {
                if(iedge instanceof ParameterEdge) {
                    ASTNode cursor = ((BaseNode) iedge.getSource()).egroumNode.getAstNode();
                    while(!(cursor instanceof CatchClause)) {
                        cursor = cursor.getParent();
                    }
                    catchClause = (CatchClause) cursor;
                }
            }
            Iterator cIt = catchNodes.iterator();
            List<Statement> tmpStmts = new ArrayList<>();
            while(cIt.hasNext()) {
                Node pNode = (Node) cIt.next();
                // 已匹配的节点
                if(!this.missingNodes.contains(pNode)) {
                    ASTNode tnode = ((BaseNode) this.anOverlap.getMappedTargetNode(pNode)).egroumNode.getAstNode();
                    ASTNode tStmt = tnode;
                    while(!(tStmt instanceof Statement)) {
                        tStmt = tStmt.getParent();
                    }
                    //int mappedIndex = catchClause.getBody().statements().indexOf(tStmt);
                    // 因为tStmt_block可能不是CatchClause的Block，可能是CatchBody中的某个语句的Block
                    Block tStmt_block = (Block) tStmt.getParent();
                    int mappedIndex = tStmt_block.statements().indexOf(tStmt);
                    //catchClause.getBody().statements().addAll(mappedIndex, tmpStmts);
                    tStmt_block.statements().addAll(mappedIndex, tmpStmts);
                    tmpStmts.clear();
                }
                // 缺失的节点
                else {
                    ASTNode cnode = this.addedTargetNodes_ref.inverse().get(pNode);
                    Statement cStmt = createStatementNode(pNode, cnode);
                    tmpStmts.add(cStmt);
                    this.addedNodesToBeDeleted.add(cnode);
                }
                cIt.remove();
            }
            if(!tmpStmts.isEmpty()) {
                if(catchClause.getBody().statements().get(catchClause.getBody().statements().size()-1) instanceof ReturnStatement) {
                    catchClause.getBody().statements().addAll(catchClause.getBody().statements().size()-1, tmpStmts);
                    return ;
                }
                else {
                    catchClause.getBody().statements().addAll(tmpStmts);
                    return ;
                }
            }
        }
        // 若不存在catch结构
        else {
            // 判断是否存在try结构
            // 目前只根据catch的入order边的起点（tryBody内的节点）是否匹配
            TryStatement tryStatement = null;
            for(Edge iedge : this.aPattern.incomingEdgesOf(csource).stream()
                    .filter(p -> p instanceof OrderEdge).collect(Collectors.toSet())) {
                if(!this.missingNodes.contains(iedge.getSource())) {
                    Node targetNode = this.anOverlap.getMappedTargetNode(iedge.getSource());
                    ASTNode cursor = ((BaseNode) targetNode).egroumNode.getAstNode();
                    while(!(cursor instanceof TryStatement) && !(cursor instanceof MethodDeclaration)) {
                        cursor = cursor.getParent();
                    }
                    if(cursor instanceof TryStatement) {
                        tryStatement = (TryStatement) cursor;
                        break;
                    }
                }
            }
            // 若存在try结构
            if(tryStatement != null) {
                // 创建CatchClause
                CatchClause newCatchClause = this.tAST.newCatchClause();
                tryStatement.catchClauses().add(newCatchClause);
                // exception
                ASTNode excepDecl = null;
                String excepName = "";
                for(Edge iedge : this.aPattern.incomingEdgesOf(csource).stream()
                        .filter(p -> p instanceof ParameterEdge).collect(Collectors.toSet())) {
                    excepDecl = ((BaseNode) iedge.getSource()).egroumNode.getAstNode();
                    excepName += ((SimpleName) excepDecl).getIdentifier();
                    break;
                }
                SingleVariableDeclaration newExcepDecl = (SingleVariableDeclaration) ASTNode.copySubtree(this.tAST, excepDecl.getParent());
                this.targetVarByPattern.put(excepName, excepName);
                newCatchClause.setException(newExcepDecl);
                // catch body
                Block newCatchBody = this.tAST.newBlock();
                newCatchClause.setBody(newCatchBody);
                Iterator cIt = catchNodes.iterator();
                List<Statement> tmpStmts = new ArrayList<>();
                while(cIt.hasNext()) {
                    Node pNode = (Node) cIt.next();
                    // 是已匹配的节点就将其从原来的block中删掉再添加
                    if(!this.missingNodes.contains(pNode)) {
                        ASTNode cnode = ((BaseNode) this.anOverlap.getMappedTargetNode(pNode)).egroumNode.getAstNode();
                        ASTNode cStmt = cnode;
                        while(!(cStmt instanceof Statement)) {
                            cStmt = cStmt.getParent();
                        }
                        Block originBlock = (Block) cStmt.getParent();
                        originBlock.statements().remove(cStmt);
                        newCatchBody.statements().add(cStmt);
                    }
                    // 是缺失的节点直接创建语句节点并添加
                    else {
                        ASTNode cnode = this.addedTargetNodes_ref.inverse().get(pNode);
                        Statement cStmt = createStatementNode(pNode, cnode);
                        newCatchBody.statements().add(cStmt);
                        this.addedNodesToBeDeleted.add(cnode);
                    }
                }
            }
            // TODO 若不存在try结构
        }
    }

    private void insertAtFinally(ASTNode tNode) {
        Edge fedge = null;
        Iterator edgeIt = this.aPattern.incomingEdgesOf(this.addedTargetNodes.get(tNode)).iterator();
        while(edgeIt.hasNext()) {
            Edge iedge = (Edge) edgeIt.next();
            if(iedge instanceof FinallyEdge) {
                fedge = iedge;
                break;
            }
        }
        // 首先判断是否已存在Finally结构，先检查是否存在匹配的finally块中的节点
        Node fsource = fedge.getSource();
        Node mappedfNode = null;
        List<Node> finallyNodes = new ArrayList<>();
        for(Edge oedge : this.aPattern.outgoingEdgesOf(fsource)) {
            if(oedge instanceof FinallyEdge) {
                if(!this.missingNodes.contains(oedge.getTarget())) {
                    for(Edge iedge : this.aPattern.incomingEdgesOf(oedge.getTarget())) {
                        if(iedge instanceof FinallyEdge
                                && !this.anOverlap.getMissingEdges().contains(iedge))
                            mappedfNode = oedge.getTarget();
                    }
                }
                finallyNodes.add(oedge.getTarget());
            }
        }
        // 再检查匹配的try块内的节点
        Block finallyBlock = null;
        if(mappedfNode == null) {
            Node ftarget0 = fedge.getTarget();
            for(Edge iedge : this.aPattern.incomingEdgesOf(ftarget0)) {
                if(iedge instanceof FinallyEdge) {
                    if(!this.missingNodes.contains(iedge.getSource())) {
                        ASTNode cursor = findNearestBlock(iedge.getSource());
                        while(!(cursor instanceof TryStatement)
                                && !(cursor instanceof MethodDeclaration)) {
                            cursor = cursor.getParent();
                        }
                        if(cursor instanceof TryStatement) {
                            if(((TryStatement) cursor).getFinally() != null) {
                                mappedfNode = iedge.getSource();
                                finallyBlock =  ((TryStatement) cursor).getFinally();
                            }
                        }
                    }
                }
            }
        }
        // 若存在finally结构
        if(mappedfNode != null) {
            // 找到最近的该finallyBlock
            finallyBlock = finallyBlock == null ? findNearestBlock(mappedfNode) : finallyBlock;
            // 把pattern的finally中所有语句按id从小到大排序
            finallyNodes.sort(new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return getEGroumId(o1) - getEGroumId(o2);
                }
            });
            Iterator fIt = finallyNodes.iterator();
            List<Statement> tmpStmts = new ArrayList<>();
            while(fIt.hasNext()) {
                Node pNode = (Node) fIt.next(); // pattern中finally内部的augNode
                // 遇到已匹配的节点
                if(!this.missingNodes.contains(pNode)) {
                    ASTNode tnode = ((BaseNode) this.anOverlap.getMappedTargetNode(pNode)).egroumNode.getAstNode();
                    ASTNode tStmt = tnode;
                    while(!(tStmt instanceof Statement)) {
                        tStmt = tStmt.getParent();
                    }
                    int mappedIndex = finallyBlock.statements().indexOf(tStmt);
                    finallyBlock.statements().addAll(mappedIndex, tmpStmts);
                    tmpStmts.clear();
                }
                // 把这个简单的表达式节点根据pattern aug包装成语句节点
                else {
                    ASTNode fnode = this.addedTargetNodes_ref.inverse().get(pNode); // 对应的给target新创建的astNode
                    Statement fStmt = createStatementNode(pNode, fnode);
                    tmpStmts.add(fStmt);
                    // remove related "missing and finally" nodes in later iterations
                    //this.addedTargetNodes.remove(fnode);
                    this.addedNodesToBeDeleted.add(fnode);
                }
                fIt.remove();
            }
            // 若全遍历完仍未空，那把剩下所有直接加到末尾，注意在returnStmt之前
            if(!tmpStmts.isEmpty()) {
                if(finallyBlock.statements().get(finallyBlock.statements().size()-1) instanceof ReturnStatement) {
                    finallyBlock.statements().addAll(finallyBlock.statements().size()-1, tmpStmts);
                    return ;
                }
                else {
                    finallyBlock.statements().addAll(tmpStmts);
                    return ;
                }
            }
        }
        // 若不存在finally结构
        else {
            // 构建finally块内容
            Block newFinallyBlock = this.tAST.newBlock();
            Block insertTry = null;
            int insertTryPos = -1;
            for(Node pNode : finallyNodes) {
                ASTNode fnode = this.addedTargetNodes_ref.inverse().get(pNode); // 对应的给target新创建的astNode
                ASTNode fStmt;
                if(fnode != null) {
                    fStmt = createStatementNode(pNode, fnode);
                }
                else{
                    // fnode为null说明已匹配，这时fnode对应语句在target中存在但不在finally块中
                    fStmt = ((BaseNode) (this.anOverlap.getMappedTargetNode(pNode))).egroumNode.getAstNode();
                    while(!(fStmt instanceof Statement)) {
                        fStmt = fStmt.getParent();
                    }
                    if(insertTry == null) {
                        insertTry = (Block) fStmt.getParent();
                        insertTryPos = insertTry.statements().indexOf(fStmt);
                    }
                    ((Block) (fStmt.getParent())).statements().remove(fStmt);
                }
                newFinallyBlock.statements().add(fStmt);
                //this.addedTargetNodes.remove(fnode); // remove related nodes in later iteration
                this.addedNodesToBeDeleted.add(fnode);
            }
            // 判断是否存在try结构
            Node ftarget = fedge.getTarget();
            TryStatement tryStmt = null;
            List<Node> tryNodes = new ArrayList<>();
            for(Edge iedge : this.aPattern.incomingEdgesOf(ftarget)) {
                if(iedge instanceof FinallyEdge) {
                    if(!this.missingNodes.contains(iedge.getSource())) {
                        tryStmt = isInTryStmt(this.anOverlap.getMappedTargetNode(iedge.getSource()));
                        if(insertTry == null && tryNodes.isEmpty()) { //找到第一个trynode在原来block中的index，即为新建try块的插入位置
                            ASTNode temp = ((BaseNode) (this.anOverlap.getMappedTargetNode(iedge.getSource())))
                                    .egroumNode.getAstNode();
                            while(!(temp instanceof Statement)) {
                                temp = temp.getParent();
                            }
                            insertTry = (Block) temp.getParent();
                            insertTryPos = insertTry.statements().indexOf(temp);
                        }
                    }
                    tryNodes.add(iedge.getSource());
                }
            }
            // 若存在try结构
            if(tryStmt != null) {
                tryStmt.setFinally(newFinallyBlock);
            }
            // 若不存在try结构
            else {
                tryNodes.sort(new Comparator<Node>() {
                    @Override
                    public int compare(Node o1, Node o2) {
                        return getEGroumId(o1) - getEGroumId(o2);
                    }
                });
                tryStmt = this.tAST.newTryStatement();
                // 构建try块内容
                Block tryBody = this.tAST.newBlock();
                tryStmt.setBody(tryBody);
                for(Node pNode : tryNodes) {
                    ASTNode tnode, tstmt;
                    if(!this.missingNodes.contains(pNode)) { // 如果已匹配那还需从原block中删掉
                        tnode = ((BaseNode) this.anOverlap.getMappedTargetNode(pNode)).egroumNode.getAstNode();
                        tstmt = tnode;
                        while(!(tstmt instanceof Statement)) {
                            tstmt = tstmt.getParent();
                        }
                        ASTNode block = tstmt;
                        while(!(block instanceof Block)) {
                            block = block.getParent();
                        }
                        ((Block) block).statements().remove(tstmt);
                    }
                    else { // 否则直接添加即可
                        tnode = this.addedTargetNodes_ref.inverse().get(pNode); // 对应的给target新创建的astNode
                        tstmt = createStatementNode(pNode, tnode);
                        //this.addedTargetNodes.remove(tnode);
                        this.addedNodesToBeDeleted.add(tnode);
                    }
                    tryBody.statements().add(tstmt);
                }
                // 添加finally块
                tryStmt.setFinally(newFinallyBlock);
                if(insertTry == null) {
                    insertTry = findNearestBlock(this.addedTargetNodes.get(tNode));
                }
                insertTry.statements().add(insertTryPos, tryStmt);
            }
       }
    }

    private Statement createStatementNode(Node pNode, ASTNode tASTNode) {
        Edge defEdge;
        for(Edge oedge : this.aPattern.outgoingEdgesOf(pNode)) {
            if(oedge instanceof DefinitionEdge) {
                String lOperand = ((SimpleName) ((Assignment) tASTNode).getLeftHandSide()).getIdentifier();
                SimpleName name = this.tAST.newSimpleName(lOperand); // left
                Expression exp = (Expression) ASTNode.copySubtree(this.tAST, ((Assignment) tASTNode).getRightHandSide()); // right
                if(lOperand.startsWith("dummy_")) {
                    VariableDeclarationFragment tVarDecFrag = this.tAST.newVariableDeclarationFragment();
                    tVarDecFrag.setName(name);
                    tVarDecFrag.setInitializer(exp);
                    VariableDeclarationStatement tVarDecStmt = this.tAST.newVariableDeclarationStatement(tVarDecFrag);
                    String type = ((AggregateDataNode) oedge.getTarget()).dataType;
                    Type tVarType;
                    if(Character.isUpperCase(type.charAt(0)))
                        tVarType = this.tAST.newSimpleType(this.tAST.newName(((AggregateDataNode) oedge.getTarget()).dataType));
                    else
                        tVarType = this.tAST.newPrimitiveType(PrimitiveType.toCode(type));
                    tVarDecStmt.setType(tVarType);
                    return tVarDecStmt;

                }
                else {
                    Assignment tAssign = this.tAST.newAssignment();
                    tAssign.setLeftHandSide(name);
                    tAssign.setRightHandSide(exp);
                    tAssign.setOperator(Assignment.Operator.toOperator("="));
                    ExpressionStatement tExpStmt = this.tAST.newExpressionStatement(tAssign);
                    return tExpStmt;
                }
            }
        }
        // 出边不含def边，说明是：单纯的函数调用语句/return语句/
        Statement newStmt;
        if(tASTNode instanceof Statement)
            newStmt = (Statement) tASTNode;
        else {
            //tASTNode = ASTNode.copySubtree(this.tAST, tASTNode); //如果在函数中改变了副本的地址，如new一个，那么副本就指向了一个新的地址，此时传入的参数还是指向原来的地址，所以不会改变参数的值
            if(tASTNode.getParent() != null && tASTNode.getParent() instanceof Statement)
                newStmt = (Statement) tASTNode.getParent();
            else
                newStmt = this.tAST.newExpressionStatement((Expression) tASTNode);
            //TODO: how to change the state of tASTNode since methods can't attach new objects to object parameters
        }
        return newStmt;
    }

    private ASTNode buildTargetNode(Node mNode) {
        ASTNode pNode = ((BaseNode) mNode).egroumNode.getAstNode();
        Node isAssign = isAssignment(mNode, this.aPattern);
        // Assignment expression
        if(isAssign != null) {
            return buildAssignmentNode(mNode, isAssign);
        }
        // MethodCall
        if(mNode instanceof MethodCallNode) {
            if(!this.anOverlap.getMissingNodes().contains(mNode)) {
                Node tNode = this.anOverlap.getMappedTargetNode(mNode);
                return ASTNode.copySubtree(this.tAST, ((BaseNode) tNode).egroumNode.getAstNode());
            }
            return buildTargetNode((MethodCallNode) mNode);
        }
        // Infix expression
        else if(mNode instanceof NullCheckNode) {
            return buildTargetNode((InfixOperatorNode) mNode);
        }
        else if(mNode instanceof InfixOperatorNode) {
            return buildTargetNode((InfixOperatorNode) mNode);
        }
        // directly copy as default
        else {
            return ASTNode.copySubtree(this.tAST, pNode);
        }
    }

    private Assignment buildAssignmentNode(Node rNode, Node lNode) {
        Assignment newAssign = this.tAST.newAssignment();
        newAssign.setOperator(Assignment.Operator.ASSIGN);
        ASTNode rOperand = buildTargetNode((MethodCallNode) rNode);
        newAssign.setRightHandSide((Expression) rOperand);
        // 找到receiver对应的egroumNode
        String pVar = "";
        EGroumNode pRecvEGNode = null;
        for(EGroumEdge oedge : ((BaseNode) rNode).egroumNode.getOutEdges()) {
            if(oedge.isDef()) {
                pRecvEGNode = oedge.getTarget();
                break;
            }
        }
        Set<DataNode> aggNodes = ((AggregateDataNode) lNode).aggregatedNodes;
        for(DataNode aggNode : aggNodes) {
            if(aggNode instanceof BaseNode) {
                if(((BaseNode) aggNode).egroumNode == pRecvEGNode) {
                    pVar += aggNode.getName();
                    if(pVar.equals("dummy_")) { pVar += getEGroumId(aggNode); }
                    break;
                }
            }
        }
        String tVar = findMappedVar(pVar);
        SimpleName lOperand = this.tAST.newSimpleName(tVar);
        newAssign.setLeftHandSide(lOperand);
        return newAssign;
    }

    private InfixExpression buildTargetNode(InfixOperatorNode mNode) {
        ASTNode pNode = mNode.egroumNode.getAstNode();
        InfixExpression tNode = createTargetASTNode((InfixExpression) pNode);
        return tNode;
    }

    private InfixExpression createTargetASTNode(InfixExpression pNode) {
        InfixExpression tNode = this.tAST.newInfixExpression();
        // operator
        tNode.setOperator(InfixExpression.Operator.toOperator(pNode.getOperator().toString()));
        // left operand
        Expression tLeftOpr = createTargetASTNode(pNode.getLeftOperand());
        tNode.setLeftOperand(tLeftOpr);
        // right operand
        Expression tRightOpr = createTargetASTNode(pNode.getRightOperand());
        tNode.setRightOperand(tRightOpr);
        return tNode;
    }

    private ClassInstanceCreation createTargetASTNode(ClassInstanceCreation pNode) {
        ClassInstanceCreation tNode = this.tAST.newClassInstanceCreation();
        // type
        Name typeName = this.tAST.newName(pNode.getType().toString());
        Type newType = this.tAST.newSimpleType(typeName);
        tNode.setType(newType);
        // args
        for(Object arg : pNode.arguments()) {
            Expression newArg = createTargetASTNode((Expression) arg);
            tNode.arguments().add(newArg);
        }
        return tNode;
    }

    private ASTNode buildTargetNode(MethodCallNode mNode) {
        if(hasArgs(mNode, this.aPattern)) {
            ASTNode pNode = ((BaseNode) mNode).egroumNode.getAstNode();
            if(pNode instanceof ClassInstanceCreation)
                return createTargetASTNode((ClassInstanceCreation) pNode);
            else
                return createTargetASTNode((MethodInvocation) pNode);
        }
        ASTNode pNode = ((BaseNode) mNode).egroumNode.getAstNode();
        if(pNode instanceof ClassInstanceCreation)
            return createTargetASTNode((ClassInstanceCreation) pNode);
        // method name
        String methodSig = mNode.getMethodSignature();
        MethodInvocation tNode = this.tAST.newMethodInvocation();
        String methodName = methodSig.substring(0, methodSig.length()-2);
        SimpleName methodNameNode = this.tAST.newSimpleName(methodName);
        tNode.setName(methodNameNode);
        // receiver
        String pRecv = "";
        // 找到receiver对应的egroumNode
        EGroumNode pRecvEGNode = null;
        for(EGroumEdge iedge : mNode.egroumNode.getInEdges()) {
            if(iedge.isRecv()) {
                pRecvEGNode = iedge.getSource();
                break;
            }
        }
        for(Edge iedge : this.aPattern.incomingEdgesOf(mNode)) {
            if(iedge instanceof ReceiverEdge) {
                Node pRecvNode = iedge.getSource();
                // 通过比较egroumNode是否相同，从AggregatedDataNode中找到对应的节点
                Set<DataNode> aggNodes = ((AggregateDataNode) pRecvNode).aggregatedNodes;
                for(DataNode aggNode : aggNodes) {
                    if(aggNode instanceof BaseNode) {
                        if(((BaseNode) aggNode).egroumNode == pRecvEGNode) {
                            pRecv += aggNode.getName();
                            if(pRecv.equals("dummy_")) {
                                pRecv += getEGroumId(aggNode);
                                aggNode.setName(pRecv);
                            }
                            break;
                        }
                    }
                }
                break;
            }
        }
        String tRecv = "";
        try {
            tRecv = findMappedVar(pRecv);
            // receiver是methodcall
            if(tRecv.startsWith("dummy")) {
                for(Node anode : this.anOverlap.getTargetNodeByPatternNode().keySet()) {
                    if(anode instanceof ActionNode || this.anOverlap.getMappedTargetNode(anode) instanceof LiteralNode)
                        continue;
                    if(((DataNode) this.anOverlap.getMappedTargetNode(anode)).getName().equals(tRecv)) {
                        // anode是receiver
                        for(Edge edge : this.aPattern.outgoingEdgesOf(anode)) {
                            if(edge instanceof ReceiverEdge && !this.missingNodes.contains(edge.getTarget())) {
                                ASTNode receiver = ((MethodInvocation) ((BaseNode) this.anOverlap.getMappedTargetNode(edge.getTarget()))
                                        .egroumNode.getAstNode()).getExpression();
                                ASTNode newReceiver = ASTNode.copySubtree(this.tAST, receiver);
                                tNode.setExpression((Expression) newReceiver);
                                break;
                            }
                        }
                        break;
                    }
                }

            }
            if(tRecv.equals("NOTEXIST")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Build MethodCallNode " + methodSig + " failed");
        } finally {
            if(tRecv.contains(".")) {
                String[] names = tRecv.split("\\.");  //.必须得加转义字符
                SimpleName name1 = this.tAST.newSimpleName(names[0]);
                SimpleName name2 = this.tAST.newSimpleName(names[1]);
                QualifiedName name = this.tAST.newQualifiedName(name1, name2);
                for(int i=2; i<names.length; i++) {
                    SimpleName n = this.tAST.newSimpleName(names[i]);
                    name = this.tAST.newQualifiedName(name, n);
                }
                tNode.setExpression(name);
            }
            else {
                if(!tRecv.startsWith("dummy")) {
                    SimpleName tRecvNode = this.tAST.newSimpleName(tRecv);
                    tNode.setExpression(tRecvNode);
                }
            }
            return tNode;
        }
    }

    private ASTNode createTargetASTNode(SimpleName pNode) {
        String pVar = pNode.getIdentifier();
        // get mapped
        String tVar = findMappedVar(pVar);
        ASTNode tNode;
        if(tVar.startsWith("dummy")) {
            for(Node anode : this.anOverlap.getTargetNodeByPatternNode().keySet()) {
                if(anode instanceof ActionNode)
                    continue;
                if(((DataNode) this.anOverlap.getMappedTargetNode(anode)).getName().equals(tVar)) {
                    // anode是receiver
                    for(Edge edge : this.aPattern.outgoingEdgesOf(anode)) {
                        if(edge instanceof ReceiverEdge && !this.missingNodes.contains(edge.getTarget())) {
                            ASTNode receiver = ((MethodInvocation) ((BaseNode) this.anOverlap.getMappedTargetNode(edge.getTarget()))
                                    .egroumNode.getAstNode()).getExpression();
                            tNode = ASTNode.copySubtree(this.tAST, receiver);
                            return tNode;
                        }
                    }
                    break;
                }
            }
        }
        if(tVar.contains(".")) {
            tVar = tVar.substring(tVar.indexOf(".")+1);
        }
        else if(isStartsWithNumber(tVar)) {
            tNode = this.tAST.newNumberLiteral(tVar);
            return tNode;
        }
        tNode = this.tAST.newSimpleName(tVar);
        return tNode;
    }

    private Expression createTargetASTNode(Expression pNode) {
        if(pNode instanceof SimpleName) { // single var
            return (Expression) createTargetASTNode((SimpleName) pNode);
        }
        //TODO: also need to consider when expression is a method invocation
        else if(pNode instanceof ClassInstanceCreation) {
            return createTargetASTNode((ClassInstanceCreation) pNode);
        }
        else if(pNode instanceof MethodInvocation) {
            return createTargetASTNode((MethodInvocation) pNode);
        }
        else if(pNode instanceof InfixExpression) {
            return createTargetASTNode((InfixExpression) pNode);
        }
        else { // qualified name
            return (Expression) ASTNode.copySubtree(this.tAST, pNode);
        }
    }

    private MethodInvocation createTargetASTNode(MethodInvocation pNode) {
        MethodInvocation tNode = this.tAST.newMethodInvocation();
        // receiver
        if(pNode.getExpression() != null) {
            Expression pExp = pNode.getExpression();
            Expression tExp = createTargetASTNode((Expression) pExp);
            tNode.setExpression(tExp);
        }
        // method name
        SimpleName mName = this.tAST.newSimpleName(pNode.getName().getIdentifier());
        tNode.setName(mName);
        // arguments
        for(Object arg : pNode.arguments()) {
            //TODO: need to consider when a method invocation is passed as an argument
            if(arg instanceof SimpleName) {
                // get mapped var
                ASTNode tVar = createTargetASTNode((SimpleName) arg);
                tNode.arguments().add(tVar);
            }
            else {
                // directly copy
                tNode.arguments().add(
                        ASTNode.copySubtree(this.tAST, (ASTNode) arg)
                );
            }
        }
        return tNode;
    }

    private String findMappedVar(String pVar) {
        if(this.targetVarByPattern.containsKey(pVar)) {
            return this.targetVarByPattern.get(pVar);
        }
        if(pVar.length() > 0 && Character.isUpperCase(pVar.charAt(0))) {
            this.targetVarByPattern.put(pVar, pVar);
            return pVar;
        }
        String tVar;
        // pVar在target已mapped
        for(Node node : this.anOverlap.getTargetNodeByPatternNode().keySet()) {
            if(node instanceof AggregateDataNode) {
                for(Node varNode : ((AggregateDataNode) node).aggregatedNodes) {
                    if(varNode instanceof LiteralNode)
                        continue;
                    if(((DataNode) varNode).getName().equals(pVar)
                            || (((DataNode) varNode).getName() + getEGroumId(varNode)).equals(pVar)) {
                        Node tVarNode = this.anOverlap.getTargetNodeByPatternNode().get(node);
                        if(tVarNode instanceof LiteralNode) {
                            tVar = ((LiteralNode) tVarNode).getValue();
                        }
                        else {
                            tVar = ((DataNode) tVarNode).getName();
                        }
                        if(tVar.equals("dummy_")) {
                            tVar += "new" + this.newVarId++;
                            ((DataNode) tVarNode).setName(tVar);
                        }
                        this.targetVarByPattern.put(pVar, tVar);
                        return tVar;
                    }
                }
            }
        }
        // pVar在target没有mapped
        if(pVar.startsWith("dummy_")) {
            tVar = "dummy_new" + this.newVarId++;
            this.targetVarByPattern.put(pVar, tVar);
            return tVar;
        }
        LOGGER.info("Finding target fix: "
                + "#Violation" + this.violationId + "# "
                + "[var]" + pVar + " in pattern not mapped "
                + "in [target]" + this.aTarget.getLocation().getFilePath() + " "
                + this.aTarget.getLocation().getMethodSignature());
        //this.mistakeId++;
        return "NOTEXIST";
    }

    /*
    * 找到target中最近一个
    * */
    private Block findNearestBlock(Node mNode) {
        int mindist = Integer.MAX_VALUE;
        Node nearestMappedNode = null;
        Set<Node> range = this.anOverlap.getTargetNodeByPatternNode().keySet().stream()
                .filter(p -> p instanceof ActionNode).collect(Collectors.toSet());
        for(Node mappedNode : range) {
            int dist = Math.abs(getEGroumId(mappedNode) - getEGroumId(mNode));
            if(dist < mindist) {
                nearestMappedNode = mappedNode;
                mindist = dist;
            }
        }
        ASTNode rltBlock = ((BaseNode) this.anOverlap.getMappedTargetNode(nearestMappedNode)).egroumNode.getAstNode();
        while(!(rltBlock instanceof Block)) {
            if(rltBlock instanceof IfStatement) {
                rltBlock = ((IfStatement) rltBlock).getThenStatement();
            }
            else if(rltBlock instanceof WhileStatement) {
                rltBlock = ((WhileStatement) rltBlock).getBody();
            }
            else if(rltBlock instanceof ForStatement) {
                rltBlock = ((ForStatement) rltBlock).getBody();
            }
            else if(rltBlock instanceof EnhancedForStatement) {
                rltBlock = ((EnhancedForStatement) rltBlock).getBody();
            }
            else if(rltBlock instanceof CatchClause) {
                rltBlock = ((CatchClause) rltBlock).getBody();
            }
            else {
                rltBlock = rltBlock.getParent();
            }
        }
        return (Block)rltBlock;
    }

    private boolean hasArgs(Node mNode, APIUsageGraph aug) {
        for(Edge iedge : aug.incomingEdgesOf(mNode)) {
            if(iedge instanceof ParameterEdge)
                return true;
        }
        return false;
    }

    private TryStatement isInTryStmt(Node augNode) {
        ASTNode cursor = ((BaseNode) augNode).egroumNode.getAstNode();
        while(!(cursor instanceof TryStatement) && !(cursor instanceof MethodDeclaration)) {
            cursor = cursor.getParent();
        }
        if(cursor instanceof TryStatement)
            return (TryStatement) cursor;
        else
            return null;
    }

    private String getReturnType(APIUsageGraph aug) {
        // get MethodDeclaration ASTNode
        MethodDeclaration md = null;
        ASTNode cursor = null;
        for(Node node : aug.vertexSet()) {
            if(((BaseNode) node).egroumNode.getAstNode() != null) {
                cursor = ((BaseNode) node).egroumNode.getAstNode();
                while(!(cursor instanceof MethodDeclaration))
                    cursor = cursor.getParent();
                break;
            }
        }
        md = (MethodDeclaration) cursor;
        IMethodBinding methodBinding = md.resolveBinding();
        if(methodBinding.isConstructor())
            return "isConstructor";
        else
            return methodBinding.getReturnType().getName();
    }

    private EGroumEdge getEGroumEdge(EGroumGraph egg, EGroumNode source, EGroumNode target) {
        for(EGroumEdge edge : egg.getEdges().stream().filter(p -> p instanceof EGroumControlEdge).collect(Collectors.toSet())) {
            if(edge.getSource() == source) {
                for(EGroumEdge oedge : source.getOutEdges()) {
                    if(oedge.getTarget() == target) {
                        return oedge;
                    }
                }
            }
        }
        return null;
    }

    private int getEGroumId(Node augNode) {
        return ((BaseNode) augNode).egroumNode.getId();
    }

    private Node isAssignment(Node mNode, APIUsageGraph aug) {
        for(Edge iedge : aug.outgoingEdgesOf(mNode)) {
            if(iedge instanceof DefinitionEdge)
                return iedge.getTarget();
        }
        return null;
    }

    private boolean isDirectCond(ASTNode cond, Node controlled, APIUsageGraph aug) {
        // 首先判断哪个是直接相关的条件节点
        Node directCon = this.addedTargetNodes.get(cond);
        for(Edge iedge : this.aPattern.incomingEdgesOf(controlled)) {
            if(iedge instanceof RepetitionEdge) {
                if(getEGroumId(iedge.getSource()) > getEGroumId(directCon)) {
                    directCon = iedge.getSource();
                }
            }
        }
        // 如果是间接条件
        if(directCon == this.addedTargetNodes.get(cond)
                || ((BaseNode) directCon).egroumNode.getSourceLineNumber() != ((BaseNode) this.addedTargetNodes.get(cond)).egroumNode.getSourceLineNumber())
            return true;
        return false;
    }

    private boolean isInThenStmt(IfStatement ifStmt, ASTNode branchStmt) {
        Statement thenStmt = ifStmt.getThenStatement();
        if(thenStmt instanceof Block) {
            if(findInBlock((Block) thenStmt, branchStmt)) return true;
            else return false;
        }
        else { // then only a single stmt
            if(thenStmt == branchStmt) return true;
            else return false;
        }
    }

    private boolean findInBlock(Block block, ASTNode target) {
        for(Object stmt : block.statements()) {
            if(stmt instanceof Block) {
                if(findInBlock((Block) stmt, target)) return true;
            }
            else {
                if(stmt == target) return true;
            }
            continue;
        }
        return false;
    }

    private boolean isStartsWithNumber(String str) {
        Pattern pattern = Pattern.compile("[0-9].*");
        Matcher isNum = pattern.matcher(str.charAt(0) + "");
        if(!isNum.matches()) {
            return false;
        }
        return true;
    }

    private Map<Statement, DefUseInfo> findDefUseInfo(Statement stmt, MethodDeclaration md) {
        Map<Statement, DefUseInfo> result = new LinkedHashMap<>();
        if(stmt instanceof IfStatement) {
            result.put(stmt, new DefUseInfo(md, stmt)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement thenStmt = ((IfStatement) stmt).getThenStatement();
            if(thenStmt != null) {
                result.putAll(findDefUseInfo(thenStmt, md, result.get(stmt).use)); //findDefUseInfo(thenStmt, md);
            }
            Statement elseStmt = ((IfStatement) stmt).getElseStatement();
            if(elseStmt != null) {
                result.putAll(findDefUseInfo(elseStmt, md)); //findDefUseInfo(elseStmt, md);
            }
        }
        else if(stmt instanceof WhileStatement) {
            result.put(stmt, new DefUseInfo(md, stmt)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((WhileStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, result.get(stmt).use)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof DoStatement) {
            result.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((DoStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, result.get(stmt).use));
            }
        }
        else if(stmt instanceof ForStatement) {
            result.put(stmt, new DefUseInfo(md, stmt)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((ForStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, result.get(stmt).use)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof EnhancedForStatement) {
            result.put(stmt, new DefUseInfo(md, stmt)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((EnhancedForStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, result.get(stmt).use)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof TryStatement) {
            result.put(stmt, new DefUseInfo(md, stmt));
            Statement tryStmt = ((TryStatement) stmt).getBody();
            result.putAll(findDefUseInfo(tryStmt, md)); //findDefUseInfo(tryStmt, md);
            for(Object catchClause : ((TryStatement) stmt).catchClauses()) {
                Statement catchStmt = ((CatchClause) catchClause).getBody();
                result.putAll(findDefUseInfo(catchStmt, md)); //findDefUseInfo(catchStmt, md);
            }
            Statement finallyStmt = ((TryStatement) stmt).getFinally();
            if(finallyStmt != null) {
                result.putAll(findDefUseInfo(finallyStmt, md)); //findDefUseInfo(finallyStmt, md);
            }
        }
        else if(stmt instanceof Block) {
            result.put(stmt, new DefUseInfo(md, stmt));
            for(Object s : ((Block) stmt).statements()) {
                result.putAll(findDefUseInfo((Statement) s, md)); //findDefUseInfo((Statement) s, md);
            }
        }
        else if(stmt instanceof SwitchStatement) {
            result.put(stmt, new DefUseInfo(md, stmt));
            Set<String> preUse = result.get(stmt).use;
            for(Object s : ((SwitchStatement) stmt).statements()) {
                Map<Statement, DefUseInfo> tmp = findDefUseInfo((Statement) s, md, preUse);
                result.putAll(tmp);
                for(DefUseInfo info : tmp.values()) {
                    result.get(stmt).def.addAll(info.def);
                    result.get(stmt).use.addAll(info.use);
                }
            }
        }
        else {
            result.put(stmt, new DefUseInfo(md, stmt)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
        }
        return result;
    }

    private Map<Statement,DefUseInfo> findDefUseInfo(Statement stmt, MethodDeclaration md, Set<String> preUse) {
        Map<Statement, DefUseInfo> result = new LinkedHashMap<>();
        if(stmt instanceof IfStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement thenStmt = ((IfStatement) stmt).getThenStatement();
            if(thenStmt != null) {
                result.putAll(findDefUseInfo(thenStmt, md, preUse)); //findDefUseInfo(thenStmt, md);
            }
            Statement elseStmt = ((IfStatement) stmt).getElseStatement();
            if(elseStmt != null) {
                result.putAll(findDefUseInfo(elseStmt, md, preUse)); //findDefUseInfo(elseStmt, md);
            }
        }
        else if(stmt instanceof WhileStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((WhileStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, preUse)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof DoStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse));
            Statement loopStmt = ((DoStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, preUse));
            }
        }
        else if(stmt instanceof ForStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((ForStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, preUse)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof EnhancedForStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
            Statement loopStmt = ((EnhancedForStatement) stmt).getBody();
            if(loopStmt != null) {
                result.putAll(findDefUseInfo(loopStmt, md, preUse)); //findDefUseInfo(loopStmt, md);
            }
        }
        else if(stmt instanceof TryStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse));
            Statement tryStmt = ((TryStatement) stmt).getBody();
            result.putAll(findDefUseInfo(tryStmt, md, preUse)); //findDefUseInfo(tryStmt, md);
            for(Object catchClause : ((TryStatement) stmt).catchClauses()) {
                Statement catchStmt = ((CatchClause) catchClause).getBody();
                result.putAll(findDefUseInfo(catchStmt, md, preUse)); //findDefUseInfo(catchStmt, md);
            }
            Statement finallyStmt = ((TryStatement) stmt).getFinally();
            if(finallyStmt != null) {
                result.putAll(findDefUseInfo(finallyStmt, md, preUse)); //findDefUseInfo(finallyStmt, md);
            }
        }
        else if(stmt instanceof Block) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse));
            for(Object s : ((Block) stmt).statements()) {
                result.putAll(findDefUseInfo((Statement) s, md, preUse)); //findDefUseInfo((Statement) s, md);
            }
        }
        else if(stmt instanceof SwitchStatement) {
            result.put(stmt, new DefUseInfo(md, stmt, preUse));
            for(Object s : ((SwitchStatement) stmt).statements()) {
                Map<Statement, DefUseInfo> tmp = findDefUseInfo((Statement) s, md, preUse);
                result.putAll(tmp);
                for(DefUseInfo info : tmp.values()) {
                    result.get(stmt).def.addAll(info.def);
                    result.get(stmt).use.addAll(info.use);
                }
            }
        }
        else {
            result.put(stmt, new DefUseInfo(md, stmt, preUse)); //this.defUseInfoMap.put(stmt, new DefUseInfo(md, stmt));
        }
        return result;
    }
}
