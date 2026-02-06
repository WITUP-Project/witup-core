#!/usr/bin/env python3
import sys, json
from sympy import S, And, Or, Not, simplify_logic, sympify, Symbol, Eq
import z3
from z3 import Solver, sat, unsat, String, StringVal
import re
import logging
from typing import List, TypedDict, Union, Literal
from sympy.parsing.sympy_parser import parse_expr

logging.basicConfig(
    level=logging.INFO,
    stream=sys.stderr,
    format='[%(levelname)s] %(message)s'
)
logger = logging.getLogger("Solver")

# schemas
class SolverCondition(TypedDict):
    truthValue: bool
    condition: str

class SolverPath(TypedDict):
    pathId: str
    conditions: List[SolverCondition]

class SolverRequest(TypedDict):
    paths: List[SolverPath]

class SolverPathResult(TypedDict):
    pathId: str
    isSat: Union[bool, Literal["unknown", "error"]]
    solutions: List[dict]  # {"variable": str, "value": str}

class SolverResponse(TypedDict):
    paths: List[SolverPathResult]



# on the shoulders of giants
# https://github.com/mmaaz-git/sym2z/blob/main/sym2z.py
def _sympy_to_z3(expr):
    if isinstance(expr, Eq):
        lhs, rhs = expr.lhs, expr.rhs
        return _sympy_to_z3(lhs) == _sympy_to_z3(rhs)

    variables = set(expr.free_symbols)

    expr_str = str(expr)
    for var in variables:
        # Add word boundaries to only replace whole variable names
        var_str = r'\b' + str(var) + r'\b'
        # might need to consider more types here
        if var.is_integer:
            expr_str = re.sub(var_str, f"z3.Int('{var}')", expr_str)
        else:
            expr_str = re.sub(var_str, f"z3.Real('{var}')", expr_str)

    expr_str = expr_str.replace("And", "z3.And")
    expr_str = expr_str.replace("Or", "z3.Or")
    expr_str = expr_str.replace("Not", "z3.Not")

    return eval(expr_str)


def check_feasibility(z3_constraints):
    if not z3_constraints:
        return {"isSat": "unknown", "solutions": []}

    solver = Solver()
    for c in z3_constraints:
        solver.add(c)

    status = solver.check()

    if status == sat:
        model = solver.model()
        solutions = []
        for d in model.decls():
            var_name = d.name()
            value = model[d]

            if isinstance(value, z3.SeqRef):
                value_str = value.as_string()
            else:
                value_str = str(value)

            solutions.append({"variable": var_name, "value": value_str})

        return {
            "isSat": True,
            "solutions": solutions,
        }
    elif status == unsat:
        return {"isSat": False, "solutions": []}
    else:
        return {"isSat": "unknown", "solutions": []}


def normalize_java_expr(expr: str):
    """
    Replace 'this.var' with 'this_var' so sympy/Z3 can handle it.
    Returns normalized expr and a mapping for de-normalization.
    """
    mapping = {}

    def replacer(match):
        original = match.group(0)
        normalized = f"this_{match.group(1)}"
        mapping[normalized] = original
        return normalized

    normalized_expr = re.sub(r"\bthis\.([a-zA-Z_][a-zA-Z0-9_]*)\b", replacer, expr)
    return normalized_expr, mapping


def denormalize_solutions(solutions, mapping):
    for sol in solutions:
        name = sol["variable"]
        if name in mapping:
            sol["variable"] = mapping[name]
    return solutions


def extract_symbols(expr: str) -> set[str]:
    """
    Extract variable identifiers from an expression string.
    Assumes Java-like identifiers after normalization.
    """
    return set(re.findall(r"\b[a-zA-Z_][a-zA-Z0-9_]*\b", expr))


def build_numeric_constraint(expr_str: str, truth_value: bool):
    parsed = parse_expr(expr_str, evaluate=False)
    expr = parsed if truth_value else Not(parsed)
    return expr


def build_string_constraint(expr_str: str, truth_value: bool):
    # "(s != 'abc')" or "(var == 'abc')"
    expr_str = expr_str.strip()[1:-1]  # remove outer parentheses
    if "!=" in expr_str:
        var, lit = map(str.strip, expr_str.split("!="))
        constraint = String(var) != StringVal(lit.strip("'").strip('"'))
    elif "==" in expr_str:
        var, lit = map(str.strip, expr_str.split("=="))
        constraint = String(var) == StringVal(lit.strip("'").strip('"'))
    else:
        raise ValueError(f"Unsupported string constraint: {expr_str}")

    if not truth_value:
        constraint = z3.Not(constraint)

    return constraint


def contains_string_literal(expr_str: str) -> bool:
    # potentially very bad. Assumes we are passing things like
    # {"condition":"(s != 'abc')","truthValue":False}
    return "'" in expr_str or '"' in expr_str


def build_constraint(expr_str: str, truth_value: bool):
    if contains_string_literal(expr_str):
        return build_string_constraint(expr_str, truth_value)
    else:
        return build_numeric_constraint(expr_str, truth_value)


def main():
    try:
        raw = sys.stdin.read()
        request: SolverRequest = json.loads(raw)
        # request = {"paths":[{"pathId":"<br.unb.cic.witup.samples.Math: boolean invalidString(java.lang.String)>#0","conditions":[{"condition":"(s != 'abc')","truthValue":False}]}]}
    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse JSON request: {e}")
        sys.exit(1)

    paths: List[SolverPath] = request.get("paths", [])
    logger.info(f"Received {len(paths)} paths for solving")

    symbols = {}

    response: SolverResponse = {"paths": []}

    for path in paths:
        path_id = path.get("pathId", "<unknown>")
        conditions = path.get("conditions", [])
        logger.info(f"[{path_id}] Starting symbol resolution ({len(conditions)} conditions)")

        # track normalised names
        var_mapping = {}
        sympy_exprs = []
        z3_exprs = []

        try:
            for c in conditions:
                normalized, mapping = normalize_java_expr(c["condition"])
                var_mapping.update(mapping)

                if contains_string_literal(normalized):
                    expr = build_string_constraint(normalized, c["truthValue"])
                else:
                    expr = build_numeric_constraint(normalized, c["truthValue"])
                    # track symbols
                    for name in extract_symbols(normalized):
                        if name not in symbols:
                            symbols[name] = Symbol(name)

                # Need to separate flows out here as sympy can't handle strings
                if isinstance(expr, z3.ExprRef):
                    z3_exprs.append(expr)
                else:
                    for name in extract_symbols(normalized):
                        if name not in symbols:
                            symbols[name] = Symbol(name)
                    sympy_exprs.append(expr)

            if sympy_exprs:
                combined_numeric_expr = And(*sympy_exprs)
                simplified = simplify_logic(combined_numeric_expr, form="dnf")
                z3_constraints = [_sympy_to_z3(simplified)] + z3_exprs
            else:
                z3_constraints = z3_exprs

            logger.info(f"[{path_id}] Starting Z3 model checking")
            result = check_feasibility(z3_constraints)

            result["solutions"] = denormalize_solutions(result["solutions"], var_mapping)

            logger.info(f"[{path_id}] Z3 result: {result['isSat']}")

        except Exception as e:
            logger.error(f"[{path_id}] ERROR during solving: {e}")
            result = {"isSat": "error", "solutions": []}

        response["paths"].append({
            "pathId": path_id,
            **result
        })

    # Only JSON to stdout
    print(json.dumps(response))

if __name__ == "__main__":
    main()