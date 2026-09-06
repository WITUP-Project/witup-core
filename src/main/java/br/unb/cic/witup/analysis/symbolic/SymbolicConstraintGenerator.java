package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.ResolvedCallee;
import br.unb.cic.witup.analysis.SummaryResolver;
import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.WITUpPath;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.node.CaughtExceptionNode;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymCast;
import br.unb.cic.witup.analysis.symbolic.expr.SymCaughtExceptionRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymClassConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymLength;
import br.unb.cic.witup.analysis.symbolic.expr.SymNew;
import br.unb.cic.witup.analysis.symbolic.expr.SymNewMultiArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticFieldRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.expr.SymVirtualInvoke;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.Type;

/**
 * Translates Jimple constraints into SymbolicConstraints. Uses backwards data flow resolution to
 * trace temporaries, parameters back to their origin node Produces a path of symbolic constraints
 * to be tested by Z3.
 */
public final class SymbolicConstraintGenerator {
  public static final String RET_PREFIX = "_ret_";
  private static final AtomicInteger GLOBAL_FRESH_COUNTER = new AtomicInteger(0);
  private final WITUpGraph cpg;
  private Set<WITUpNode> currentPathNodes = Collections.emptySet();
  private Map<WITUpNode, Integer> currentPathLastIndex = Collections.emptyMap();
  private final SummaryResolver resolver;

  public SymbolicConstraintGenerator(final WITUpGraph cpg, final SummaryResolver resolver) {
    this.cpg = cpg;
    this.resolver = resolver;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(final WITUpPath p) {
    setCurrentPath(p);
    IterationContext iterCtx = IterationContext.compute(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>(iterCtx.linkPreconditions());

    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      StmtGraphNode n = (StmtGraphNode) throwConstraint.node().getNode();
      List<SymbolicConstraint> preconditions = new ArrayList<>();

      SymExpr symExpr;
      if (n.getStmt() instanceof JIfStmt ifStmt) {
        SymExpr ifCond = SymExpr.fromJimple(ifStmt.getCondition());
        // Rewrite self-update var uses (loop counters / accumulators) to iter-indexed
        // SymVars before substitution. Without this, the same variable name on either
        // side of a back-edge would resolve to its initial def in both places, producing
        // contradictory same-SymExpr constraints (e.g. `0 >= xs.length` asserted both
        // true and false on the same path).
        ifCond = iterCtx.rewriteAt(ifCond, throwConstraint.forwardEdgeIndex());

        SubstituteResult result = substituteWithPreconditions(ifCond, throwConstraint.node());
        symExpr = result.expr();
        preconditions.addAll(result.preconditions());
      } else if (throwConstraint.node() instanceof CaughtExceptionNode caught) {
        SymCaughtExceptionRef catchRef = new SymCaughtExceptionRef(caught.getCaughtExceptionRef());
        preconditions.add(sealCaughtExceptionRef(catchRef));
        symExpr = catchRef;
      } else {
        throw new IllegalStateException(
            "Unexpected constraint node type: " + throwConstraint.node().getClass());
      }
      symExpr = SymExpr.simplifyBoxingPatterns(symExpr);

      symbolicConstraints.addAll(preconditions);
      symbolicConstraints.add(new SymbolicConstraint(symExpr, throwConstraint.truthValue()));
    }
    // The catch-ref seal can be emitted from both the CaughtExceptionNode throw-constraint
    // path and the tightened-substitution path on the same path; keep first occurrence so
    // ordering still reads bottom-up but drop the structural duplicate.
    return new ArrayList<>(new java.util.LinkedHashSet<>(symbolicConstraints));
  }

  private void setCurrentPath(final WITUpPath p) {
    this.currentPathNodes = new HashSet<>(p.nodes());
    List<WITUpNode> forward = p.forwardNodes();
    Map<WITUpNode, Integer> lastIndex = new HashMap<>(forward.size() * 2);
    // overwrites: a node revisited under loop unrolling ends up mapped to its
    // latest occurrence — the right anchor for "latest reaching def on path".
    for (int i = 0; i < forward.size(); i++) {
      lastIndex.put(forward.get(i), i);
    }
    this.currentPathLastIndex = lastIndex;
  }

  public List<List<SymbolicConstraint>> buildThrowConstraintPaths(final WITUpNode throwNode) {
    List<WITUpPath> throwConstraintPaths = cpg.getThrowPaths(throwNode);
    return generateThrowConstraintPath(throwConstraintPaths);
  }

  private List<List<SymbolicConstraint>> generateThrowConstraintPath(
      final List<WITUpPath> throwConstraintPaths) {
    List<List<SymbolicConstraint>> symbolicConstraints = new ArrayList<>();
    for (WITUpPath p : throwConstraintPaths) {
      List<SymbolicConstraint> resolved = generateSymbolicConstraints(p);
      if (hasDirectContradiction(resolved)) {
        continue;
      }
      List<SymbolicConstraint> folded = foldAndFilterConstraints(resolved);
      if (folded == null) {
        continue; // any constraint folded to a constant that contradicts its truthValue
      }
      symbolicConstraints.add(folded);
    }
    return symbolicConstraints;
  }

  // Path is trivially UNSAT if it asserts both `(X, true)` and `(X, false)` for the same
  // SymExpr. Happens when CFG branches converge onto a throw site (loop unrolling, dead
  // branches that the structural enumerator can't tell from live ones). Z3 would prove
  // these UNSAT anyway; checking here just saves the trip and removes noise rows from
  // the summary output. SymExpr.equals/hashCode are structural, so the map keys by
  // semantic identity.
  private static boolean hasDirectContradiction(final List<SymbolicConstraint> constraints) {
    if (constraints.size() < 2) {
      return false;
    }
    Map<SymExpr, Boolean> seen = new HashMap<>(constraints.size() * 2);
    for (SymbolicConstraint c : constraints) {
      Boolean prev = seen.putIfAbsent(c.symExpr(), c.truthValue());
      if (prev != null && prev != c.truthValue()) {
        return true;
      }
    }
    return false;
  }

  // Constant-folds each constraint's SymExpr and filters out tautologies. Returns null if
  // any constraint folds to a constant that contradicts its truthValue — the path is
  // trivially UNSAT. Otherwise returns the (possibly-empty) list of remaining constraints;
  // an empty list means every constraint folded to "trivially satisfied", and the path is
  // unconditionally reachable (kept). Z3 would deliver the same verdicts on each constraint
  // shape; doing it here saves a solver round trip and de-noises the JSON output. Public so
  // implicit-exception collectors and the rollup walker can apply the same filter — they
  // build constraints directly and never go through generateThrowConstraintPath.
  public static List<SymbolicConstraint> foldAndFilterConstraints(
      final List<SymbolicConstraint> constraints) {
    List<SymbolicConstraint> result = new ArrayList<>(constraints.size());
    for (SymbolicConstraint c : constraints) {
      SymExpr folded = foldConstants(c.symExpr());
      if (folded instanceof SymIntConst constant) {
        boolean value = constant.getValue() != 0;
        if (value == c.truthValue()) {
          continue;
        }
        return null;
      }
      SymbolicConstraint next =
          folded == c.symExpr() ? c : new SymbolicConstraint(folded, c.truthValue());
      result.add(stripBooleanEncoding(next));
    }
    return result;
  }

  // Strip `b == 0` to `b` (with flipped truthValue) and `b != 0` to `b` (truthValue
  // unchanged) when `b` has boolean kind. Operates at the constraint level because
  // handling `b == 0` requires flipping truthValue — a SymExpr-only version would
  // either drop polarity or force every caller to remember which op was stripped.
  // Cosmetic for the JSON output; semantically equivalent for Z3.
  private static SymbolicConstraint stripBooleanEncoding(final SymbolicConstraint c) {
    if (!(c.symExpr() instanceof SymBinOp bin)) {
      return c;
    }
    if (!(bin.getRhs() instanceof SymIntConst rc) || rc.getValue() != 0) {
      return c;
    }
    SymExpr lhs = bin.getLhs();
    if (lhs.getKind() != SymKind.BOOLEAN && lhs.getKind() != SymKind.BOOLEAN_METHOD) {
      return c;
    }
    if (bin.getOp() == BinOp.EQ) {
      return new SymbolicConstraint(lhs, !c.truthValue());
    }
    if (bin.getOp() == BinOp.NE) {
      return new SymbolicConstraint(lhs, c.truthValue());
    }
    return c;
  }

  // Recursively evaluates SymBinOps whose children are both SymIntConst, producing a new
  // SymIntConst. Also applies short-circuit identities for AND/OR when one operand is a
  // known constant — critical because partial folding can otherwise leave a SymBinOp with
  // one int-typed child and one bool-typed child, which the Z3 translator can't handle
  // cleanly (its OR/AND paths assume homogeneous operand types). Non-foldable cases fall
  // through to a structurally-rebuilt SymBinOp if any child changed, or the original
  // expr if not.
  public static SymExpr foldConstants(final SymExpr expr) {
    // `new T[N].length` folds to `N` when N is a constant — Java guarantees the field
    // equals the allocation size. Common in <clinit> initialisers like BOM byte arrays
    // where the analysis would otherwise treat `newarray(int[])[3].length` as opaque and
    // emit spurious AIOOBE paths.
    if (expr instanceof SymLength len && len.getOp() instanceof SymArray arr) {
      SymExpr size = foldConstants(arr.getSize());
      if (size instanceof SymIntConst) {
        return size;
      }
    }
    if (!(expr instanceof SymBinOp b)) {
      return expr;
    }
    SymExpr lhs = foldConstants(b.getLhs());
    SymExpr rhs = foldConstants(b.getRhs());
    if (lhs instanceof SymIntConst lc && rhs instanceof SymIntConst rc) {
      Integer evaluated = evaluateBinOp(b.getOp(), lc.getValue(), rc.getValue());
      if (evaluated != null) {
        return SymIntConst.of(evaluated);
      }
    }
    // `null == null` / `null != null` fall out of substitution when a `var == null`
    // branch decision is enumerated onto a path on which `var` has only the initial
    // `var = null` reaching def (or vice versa). Z3 catches these as UNSAT but they
    // outnumber genuine UNSAT in loop-heavy methods like Tailer.run.
    if (lhs instanceof SymNull && rhs instanceof SymNull) {
      if (b.getOp() == BinOp.EQ) {
        return SymIntConst.one();
      }
      if (b.getOp() == BinOp.NE) {
        return SymIntConst.zero();
      }
    }
    // `new T(...)`, `new T[N]`, `new T[N][M]`, string literals, and class literals are all
    // JLS-guaranteed non-null. Implicit NPE-receiver collectors emit (recv == null, true)
    // for every dereference site; when the receiver is one of these forms (common for
    // <clinit> static initialisers and StringBuilder chains) the constraint is trivially
    // false. Z3 says SAT because these are opaque symbols — the analysis doesn't model
    // JVM allocation invariants — so without this fold these become spurious SAT rows.
    if (isProvablyNonNull(lhs) && rhs instanceof SymNull
        || lhs instanceof SymNull && isProvablyNonNull(rhs)) {
      if (b.getOp() == BinOp.EQ) {
        return SymIntConst.zero();
      }
      if (b.getOp() == BinOp.NE) {
        return SymIntConst.one();
      }
    }
    // Short-circuit identities: (0 || X) = X, (1 || X) = 1, (1 && X) = X, (0 && X) = 0,
    // and symmetric forms. Without these, a partial fold leaves a mixed-type SymBinOp.
    if (b.getOp() == BinOp.OR) {
      if (lhs instanceof SymIntConst lc) {
        return lc.getValue() == 0 ? rhs : lc;
      }
      if (rhs instanceof SymIntConst rc) {
        return rc.getValue() == 0 ? lhs : rc;
      }
    } else if (b.getOp() == BinOp.AND) {
      if (lhs instanceof SymIntConst lc) {
        return lc.getValue() == 0 ? lc : rhs;
      }
      if (rhs instanceof SymIntConst rc) {
        return rc.getValue() == 0 ? rc : lhs;
      }
    }
    if (lhs != b.getLhs() || rhs != b.getRhs()) {
      return new SymBinOp(b.getOp(), lhs, rhs);
    }
    return expr;
  }

  // Static fields known to be non-null after class init. JDK and well-known library
  // constants. Grow as the benchmark surfaces more. Format matches
  // SymStaticFieldRef.getFieldSignature().
  private static final Set<String> NON_NULL_STATIC_FIELDS =
      Set.of(
          "<java.nio.ByteOrder: java.nio.ByteOrder BIG_ENDIAN>",
          "<java.nio.ByteOrder: java.nio.ByteOrder LITTLE_ENDIAN>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset ISO_8859_1>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset US_ASCII>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset UTF_8>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset UTF_16>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset UTF_16BE>",
          "<java.nio.charset.StandardCharsets: java.nio.charset.Charset UTF_16LE>",
          "<org.apache.commons.io.IOCase: org.apache.commons.io.IOCase SENSITIVE>",
          "<org.apache.commons.io.IOCase: org.apache.commons.io.IOCase INSENSITIVE>",
          "<org.apache.commons.io.IOCase: org.apache.commons.io.IOCase SYSTEM>");

  // JDK static methods documented to always return a non-null value. Format matches
  // SymStaticInvoke.getInvokeName() (the full SootUp method signature).
  private static final Set<String> NON_NULL_STATIC_METHODS =
      Set.of(
          "<java.lang.Integer: java.lang.String toHexString(int)>",
          "<java.lang.Integer: java.lang.String toBinaryString(int)>",
          "<java.lang.Integer: java.lang.String toOctalString(int)>",
          "<java.lang.Integer: java.lang.String toString(int)>",
          "<java.lang.Integer: java.lang.String toString(int,int)>",
          "<java.lang.Long: java.lang.String toHexString(long)>",
          "<java.lang.Long: java.lang.String toBinaryString(long)>",
          "<java.lang.Long: java.lang.String toOctalString(long)>",
          "<java.lang.Long: java.lang.String toString(long)>",
          "<java.lang.Long: java.lang.String toString(long,int)>",
          "<java.lang.Float: java.lang.String toString(float)>",
          "<java.lang.Double: java.lang.String toString(double)>",
          "<java.lang.Boolean: java.lang.String toString(boolean)>",
          "<java.lang.Character: java.lang.String toString(char)>",
          "<java.lang.String: java.lang.String valueOf(int)>",
          "<java.lang.String: java.lang.String valueOf(long)>",
          "<java.lang.String: java.lang.String valueOf(float)>",
          "<java.lang.String: java.lang.String valueOf(double)>",
          "<java.lang.String: java.lang.String valueOf(boolean)>",
          "<java.lang.String: java.lang.String valueOf(char)>",
          "<java.lang.String: java.lang.String valueOf(java.lang.Object)>",
          "<java.lang.String: java.lang.String valueOf(char[])>",
          "<java.lang.String: java.lang.String valueOf(char[],int,int)>");

  // idea: grab invariants from language specification
  private static boolean isProvablyNonNull(final SymExpr e) {
    if (e instanceof SymNew
        || e instanceof SymArray
        || e instanceof SymNewMultiArray
        || e instanceof SymStringConst
        || e instanceof SymClassConst) {
      return true;
    }
    // Reference casts don't change null-ness: `(T)X` is null iff X is null. Java
    // permits casting null to any reference type without throwing.
    if (e instanceof SymCast cast) {
      return isProvablyNonNull(cast.getOp());
    }
    if (e instanceof SymStaticFieldRef sfr
        && NON_NULL_STATIC_FIELDS.contains(sfr.getFieldSignature())) {
      return true;
    }
    if (e instanceof SymStaticInvoke si && NON_NULL_STATIC_METHODS.contains(si.getInvokeName())) {
      return true;
    }
    // StringBuilder/StringBuffer.append(*) returns `this` (return-self contract). The
    // chain is non-null iff the receiver is — recurse. Catches the typical fluent
    // error-message construction pattern: `new StringBuilder().append(x).append(y)...`.
    if (e instanceof SymVirtualInvoke vi
        && "append".equals(vi.getSignature())
        && isStringBuilderTyped(vi.getBase())) {
      return isProvablyNonNull(vi.getBase());
    }
    return false;
  }

  private static boolean isStringBuilderTyped(final SymExpr e) {
    if (e instanceof SymNew sn) {
      String t = sn.getClassType();
      return "java.lang.StringBuilder".equals(t) || "java.lang.StringBuffer".equals(t);
    }
    if (e instanceof SymVirtualInvoke vi && "append".equals(vi.getSignature())) {
      return isStringBuilderTyped(vi.getBase());
    }
    return false;
  }

  private static Integer evaluateBinOp(final BinOp op, final int a, final int b) {
    return switch (op) {
      case EQ -> a == b ? 1 : 0;
      case NE -> a != b ? 1 : 0;
      case LT -> a < b ? 1 : 0;
      case LE -> a <= b ? 1 : 0;
      case GT -> a > b ? 1 : 0;
      case GE -> a >= b ? 1 : 0;
      case ADD -> a + b;
      case SUB -> a - b;
      case MUL -> a * b;
      case DIV -> b == 0 ? null : a / b;
      case MOD -> b == 0 ? null : a % b;
      case AND -> a & b;
      case OR -> a | b;
      case XOR -> a ^ b;
      case SHIFT_LEFT -> a << b;
      case SHIFT_RIGHT -> a >> b;
      case UNSIGNED_SHIFT_RIGHT -> a >>> b;
      case CMP, CMPG, CMPL -> Integer.compare(a, b);
    };
  }

  private record SubstituteResult(SymExpr expr, List<SymbolicConstraint> preconditions) {}

  private SubstituteResult substituteWithPreconditions(
      final SymExpr initial, final WITUpNode startNode) {
    List<SymbolicConstraint> preconditions = new ArrayList<>();
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), preconditions);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    symExpr = SymExpr.simplifyBoxingPatterns(symExpr);
    return new SubstituteResult(symExpr, preconditions);
  }

  public List<SymParamRef> buildFormals() {
    List<Type> paramTypes = cpg.getMethod().getParameterTypes();
    List<SymParamRef> formals = new ArrayList<>();
    for (int i = 0; i < paramTypes.size(); i++) {
      formals.add(new SymParamRef(i, paramTypes.get(i)));
    }
    // @this in -1 index
    if (!cpg.getMethod().isStatic()) {
      formals.add(new SymParamRef(-1, cpg.getMethod().getDeclaringClassType()));
    }
    return formals;
  }

  private Optional<ResolvedCallee> tryResolveLambda(
      final JInterfaceInvokeExpr invoke, final WITUpNode node) {

    String receiverName = SymVar.nameOf(invoke.getBase());
    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(node)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);
      if (nodeNotInPath(sourceNode)) {
        continue;
      }
      if (!(sourceNode instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      if (!SymVar.nameOf(assign.getLeftOp()).equals(receiverName)) {
        continue;
      }

      if (assign.getRightOp() instanceof JDynamicInvokeExpr dynInvoke) {
        List<Immediate> bootstrapArgs = dynInvoke.getBootstrapArgs();
        if (bootstrapArgs.size() < 2) {
          return Optional.empty();
        }
        // this should always be the implementation method handle for lambda
        // metafactory invocations
        if (!(bootstrapArgs.get(1) instanceof MethodHandle mh)) {
          return Optional.empty();
        }

        // e.g. <Int: Integer lambda$applyAndCheckResult$1(int)>
        String className = mh.getReferenceSignature().getDeclClassType().getFullyQualifiedName();
        String subSig = mh.getReferenceSignature().getSubSignature().toString();
        String lambdaSig = "<" + className + ": " + subSig + ">";

        List<SymExpr> actuals =
            dynInvoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());

        return resolver.resolveCallee(lambdaSig, actuals);
      }
      // not a dynamic invoke — skip
    }

    return Optional.empty();
  }

  private SymExpr backwardSubstitute(
      final SymExpr symExpr,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final List<SymbolicConstraint> preconditions) {

    Set<String> freeVars = new HashSet<>();
    symExpr.collectVarNames(freeVars);
    if (freeVars.isEmpty()) {
      return symExpr;
    }
    Map<String, SymExpr> env = new HashMap<>();
    collectBindings(freeVars, env, currentNode, visited, true, preconditions);
    return env.isEmpty() ? symExpr : symExpr.resolveWith(env);
  }

  private void collectBindings(
      final Set<String> freeVars,
      final Map<String, SymExpr> env,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity,
      final List<SymbolicConstraint> preconditions) {

    if (visited.contains(currentNode)) {
      return;
    }
    visited.add(currentNode);

    Map<String, WITUpNode> latestByVar = pickLatestPathDefs(currentNode, followIdentity);

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);
      if (nodeNotInPath(sourceNode)) {
        continue;
      }
      if (!(sourceNode instanceof SimpleNode simpleNode)) {
        continue;
      }
      if (!(simpleNode.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }

      Stmt stmt = stmtNode.getStmt();
      Value lhsOp;
      Value rhsOp;

      if (stmt instanceof JAssignStmt assign) {
        if (!isStackVariable(assign.getLeftOp()) && assign.getRightOp() instanceof JCastExpr) {
          continue;
        }
        rhsOp = assign.getRightOp();
        lhsOp = assign.getLeftOp();

        Optional<ResolvedCallee> resolvedCallee = resolveCallee(rhsOp);

        if (resolvedCallee.isEmpty() && rhsOp instanceof JInterfaceInvokeExpr ifaceInvoke) {
          resolvedCallee = tryResolveLambda(ifaceInvoke, sourceNode);
        }

        if (resolvedCallee.isPresent()) {
          String definedVar = getVariableName(assign.getLeftOp());
          if (freeVars.contains(definedVar)
              && !isShadowedByLaterDef(sourceNode, definedVar, latestByVar)) {
            ResolvedCallee callee = resolvedCallee.get();
            if (callee.guardedReturn() != null) {
              SymVar freshVar =
                  SymVar.fresh(
                      RET_PREFIX + GLOBAL_FRESH_COUNTER.getAndIncrement(),
                      callee.guardedReturn().isEmpty()
                          ? SymKind.INT
                          : callee.guardedReturn().getFirst().value().getKind());
              addBinding(freeVars, env, definedVar, freshVar);
              for (GuardedExpr ge : callee.guardedReturn()) {
                preconditions.addAll(ge.preconditions());
                SymExpr eq = new SymBinOp(BinOp.EQ, freshVar, ge.value());
                SymExpr implication = encodeImplication(ge.guard(), eq);
                preconditions.add(new SymbolicConstraint(implication, true));
              }
              if (callee.throwPathConditions() != null) {
                for (List<SymbolicConstraint> throwPath : callee.throwPathConditions()) {
                  SymExpr throwGuard = encodeConjunction(throwPath);
                  preconditions.add(new SymbolicConstraint(throwGuard, false));
                }
              }
            }
            collectBindings(freeVars, env, sourceNode, visited, followIdentity, preconditions);
          }
          continue;
        }
      } else if (stmt instanceof JIdentityStmt identity) {
        if (!followIdentity) {
          continue;
        }
        lhsOp = identity.getLeftOp();
        rhsOp = identity.getRightOp();
      } else {
        continue;
      }

      String definedVar = getVariableName(lhsOp);
      if (!freeVars.contains(definedVar)
          || isShadowedByLaterDef(sourceNode, definedVar, latestByVar)) {
        continue;
      }
      SymExpr boundValue = resolveCaughtExceptionRef(SymExpr.fromJimple(rhsOp), sourceNode);
      if (boundValue instanceof SymCaughtExceptionRef catchRef) {
        preconditions.add(sealCaughtExceptionRef(catchRef));
      }
      if (boundValue instanceof SymParamRef paramRef) {
        boundValue = paramRef.withName(definedVar);
      }
      addBinding(freeVars, env, definedVar, boundValue);
      collectBindings(freeVars, env, sourceNode, visited, followIdentity, preconditions);
    }
  }

  // Encodes the semantic invariant `caught_<type> <=> caught_<type>_is_null = false` as a
  // SymExpr-level constraint. The path's exceptional edge already pins the catch flag, and
  // the throw guard substitution pins the null check; without this lock, those two Z3 bool
  // consts are independent and a future change that touches one could leave the other free.
  // Translates as Z3 mkEq on two BoolExprs, which is iff.
  private static SymbolicConstraint sealCaughtExceptionRef(final SymCaughtExceptionRef ref) {
    SymExpr nonNull = new SymBinOp(BinOp.NE, ref, SymNull.INSTANCE);
    return new SymbolicConstraint(new SymBinOp(BinOp.EQ, ref, nonNull), true);
  }

  // SootUp materializes `catch (T t) { x = t; }` as a JIdentityStmt-into-stack-temp followed
  // by a JAssignStmt copying that temp into the user local. With followIdentity=false, the
  // backward walk stops at the stack temp, so a substitution of `x` lands on `stack_N` —
  // useful but not semantic. When the bound RHS is a Local whose only path-included DDG
  // source is a JIdentityStmt with JCaughtExceptionRef on the right, collapse the chain to
  // SymCaughtExceptionRef so the resulting Z3 const reads `caught_<type>_is_null` rather
  // than `stack_N_is_null`.
  private SymExpr resolveCaughtExceptionRef(final SymExpr fallback, final WITUpNode bindSource) {
    if (!(fallback instanceof SymVar)) {
      return fallback;
    }
    JCaughtExceptionRef caughtRef = null;
    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(bindSource)) {
      WITUpNode src = cpg.getEdgeSource(edge);
      if (nodeNotInPath(src)) {
        continue;
      }
      JCaughtExceptionRef ref = WITUpGraph.caughtExceptionRefOf(src);
      if (ref == null) {
        continue;
      }
      if (caughtRef != null && !caughtRef.getType().equals(ref.getType())) {
        return fallback;
      }
      caughtRef = ref;
    }
    return caughtRef != null ? new SymCaughtExceptionRef(caughtRef) : fallback;
  }

  private static boolean isShadowedByLaterDef(
      final WITUpNode src, final String var, final Map<String, WITUpNode> latestByVar) {
    WITUpNode latest = latestByVar.get(var);
    return latest != null && !src.equals(latest);
  }

  // For each variable defined by 2+ path-included DDG sources at currentNode, returns the
  // source whose forward-path position is latest — i.e. the reaching def that wins under
  // standard backward def-use chasing on this single path. Variables with a single source
  // are omitted (the existing collectBindings flow handles them without shadow filtering).
  // Sources are also excluded if collectBindings would itself skip them (cast-on-non-stack
  // assigns, identity statements when followIdentity is false, self-updates handled by
  // IterationContext) — picking such a source as "latest" would shadow an earlier eligible
  // def and silently drop the substitution.
  private Map<String, WITUpNode> pickLatestPathDefs(
      final WITUpNode currentNode, final boolean followIdentity) {
    Map<String, List<WITUpNode>> byVar = new HashMap<>();
    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode src = cpg.getEdgeSource(edge);
      if (nodeNotInPath(src) || !isEligibleDefSource(src, followIdentity)) {
        continue;
      }
      String var = definedVarOf(src);
      if (var == null || isSelfUpdate(src, var)) {
        continue;
      }
      byVar.computeIfAbsent(var, k -> new ArrayList<>()).add(src);
    }
    Map<String, WITUpNode> latest = new HashMap<>();
    for (Map.Entry<String, List<WITUpNode>> entry : byVar.entrySet()) {
      List<WITUpNode> srcs = entry.getValue();
      if (srcs.size() < 2) {
        continue;
      }
      WITUpNode latestSrc = null;
      int latestIdx = -1;
      for (WITUpNode src : srcs) {
        Integer idx = currentPathLastIndex.get(src);
        if (idx != null && idx > latestIdx) {
          latestIdx = idx;
          latestSrc = src;
        }
      }
      if (latestSrc != null) {
        latest.put(entry.getKey(), latestSrc);
      }
    }
    return latest;
  }

  private static boolean isEligibleDefSource(final WITUpNode src, final boolean followIdentity) {
    if (!(src instanceof SimpleNode sn)) {
      return false;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return false;
    }
    Stmt stmt = stmtNode.getStmt();
    if (stmt instanceof JAssignStmt assign) {
      // Mirror the cast-on-non-stack skip in collectBindings.
      return isStackVariable(assign.getLeftOp()) || !(assign.getRightOp() instanceof JCastExpr);
    }
    if (stmt instanceof JIdentityStmt) {
      return followIdentity;
    }
    return false;
  }

  //  private static boolean isStackVariableValue(final Value value) {
  //    return value.toString().contains("$stack");
  //  }

  private static String definedVarOf(final WITUpNode src) {
    if (!(src instanceof SimpleNode sn)) {
      return null;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return null;
    }
    Stmt stmt = stmtNode.getStmt();
    if (stmt instanceof JAssignStmt assign) {
      return getVariableName(assign.getLeftOp());
    }
    if (stmt instanceof JIdentityStmt identity) {
      return getVariableName(identity.getLeftOp());
    }
    return null;
  }

  private static boolean isSelfUpdate(final WITUpNode src, final String lhsName) {
    if (!(src instanceof SimpleNode sn)) {
      return false;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return false;
    }
    if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
      return false;
    }
    return SymExpr.fromJimple(assign.getRightOp()).contains(lhsName);
  }

  private static void addBinding(
      final Set<String> freeVars,
      final Map<String, SymExpr> env,
      final String varName,
      final SymExpr expr) {
    SymExpr existing = env.get(varName);
    if (existing != null) {
      // variable redefined on the path — compose with the earlier binding
      env.put(varName, existing.substitute(varName, expr));
    } else {
      env.put(varName, expr);
    }
    freeVars.remove(varName);
    expr.collectVarNames(freeVars);
  }

  private static SymExpr encodeImplication(
      final List<SymbolicConstraint> guard, final SymExpr consequent) {
    if (guard.isEmpty()) {
      return consequent;
    }
    SymExpr result = consequent;
    for (int i = guard.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = guard.get(i);
      SymExpr cond =
          c.truthValue() ? c.symExpr() : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.one());
    }
    return result;
  }

  private static SymExpr encodeConjunction(final List<SymbolicConstraint> constraints) {
    SymExpr result = SymIntConst.one();
    for (int i = constraints.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = constraints.get(i);
      SymExpr cond =
          c.truthValue() ? c.symExpr() : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.zero());
    }
    return result;
  }

  public List<GuardedExpr> traceGuardedReturn() {
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      return List.of();
    }

    List<GuardedExpr> result = new ArrayList<>();
    for (int i = returnNodes.size() - 1; i >= 0; i--) {
      result.addAll(generateReturnGuarded(returnNodes.get(i)));
    }
    return result;
  }

  private List<GuardedExpr> generateReturnGuarded(final ReturnStatementNode returnNode) {
    List<WITUpPath> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      SymExpr value = SymExpr.stripBoxing(SymExpr.fromJimple(returnNode.getOp()));
      return List.of(new GuardedExpr(List.of(), value));
    }

    List<GuardedExpr> result = new ArrayList<>(paths.size());
    for (WITUpPath path : paths) {
      setCurrentPath(path);
      SubstituteResult sr =
          substituteWithPreconditions(SymExpr.fromJimple(returnNode.getOp()), returnNode);
      SymExpr value = SymExpr.stripBoxing(sr.expr());
      List<SymbolicConstraint> guard = generateSymbolicConstraints(path);
      result.add(new GuardedExpr(guard, value, sr.preconditions()));
    }
    return result;
  }

  private Optional<ResolvedCallee> resolveCallee(final Value rhsOp) {
    String calleeSig;
    List<SymExpr> actuals;

    switch (rhsOp) {
      case JVirtualInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase()));
      }
      case JStaticInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JInterfaceInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase())); // add @this
      }
      case JSpecialInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase())); // add @this
      }
      case JDynamicInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case null, default -> {
        return Optional.empty();
      }
    }

    return resolver.resolveCallee(calleeSig, actuals);
  }

  private boolean nodeNotInPath(final WITUpNode node) {
    return !currentPathNodes.contains(node);
  }

  private static String getVariableName(final Value value) {
    return SymVar.nameOf(value);
  }

  private static boolean isStackVariable(final LValue value) {
    return value.toString().contains("$stack");
  }
}
