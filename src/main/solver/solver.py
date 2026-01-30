#!/usr/bin/env python3
import sys, json
from sympy import S, And, Or, Not, simplify_logic, sympify
import z3
from z3 import Solver, sat, unsat
import re
import logging
from typing import List, TypedDict, Union, Literal

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
    variables = set(expr.free_symbols)

    expr_str = str(expr)
    for var in variables:
        # Add word boundaries to only replace whole variable names
        var_str = r'\b' + str(var) + r'\b'
        if var.is_integer:
            expr_str = re.sub(var_str, f"z3.Int('{var}')", expr_str)
        else:
            expr_str = re.sub(var_str, f"z3.Real('{var}')", expr_str)

    # Boolean operators
    expr_str = expr_str.replace("And", "z3.And")
    expr_str = expr_str.replace("Or", "z3.Or")
    expr_str = expr_str.replace("Not", "z3.Not")

    return eval(expr_str)

def check_feasibility(system):
    if not isinstance(system, list):
        system = [system]

    variables = set()
    for expr in system:
        variables.update(expr.free_symbols)

    s = Solver()
    for expr in system:
        s.add(_sympy_to_z3(expr))

    if s.check() == sat:
        model = s.model()
        result = {}
        solutions = []
        for var in variables:
            z3_var = z3.Int(str(var)) if var.is_integer else z3.Real(str(var))
            value = model.get_interp(z3_var)
            result[var] = S(str(value))
            solution = {
                "variable": str(var),
                "value": str(value)
            }
            solutions.append(solution)
        return {
            "isSat": True,
            "solutions": solutions,
        }
    elif s.check() == unsat:
        return {"isSat": False, "solutions": []}
    else:
        return {"isSat": "unknown", "solutions": []}


def normalize_java_expr(expr: str):
    """
    Replace 'this.var' -> 'this_var' so sympy/Z3 can handle it.
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

def main():
    try:
        raw = sys.stdin.read()
        request: SolverRequest = json.loads(raw)  # type: ignore
    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse JSON request: {e}")
        sys.exit(1)

    paths: List[SolverPath] = request.get("paths", [])
    logger.info(f"Received {len(paths)} paths for solving")

    response: SolverResponse = {"paths": []}

    for path in paths:
        path_id = path.get("pathId", "<unknown>")
        conditions = path.get("conditions", [])
        logger.info(f"[{path_id}] Starting symbol resolution ({len(conditions)} conditions)")

        # track normalised names
        var_mapping = {}

        try:
            sympy_exprs = []
            for c in conditions:
                normalized, mapping = normalize_java_expr(c["condition"])
                var_mapping.update(mapping)
                expr = sympify(normalized) if c["truthValue"] else Not(sympify(normalized))
                sympy_exprs.append(expr)

            combined_expr = And(*sympy_exprs)
            simplified_expr = simplify_logic(combined_expr, form="dnf")

            logger.info(f"[{path_id}] Starting Z3 model checking")
            result = check_feasibility(simplified_expr)

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