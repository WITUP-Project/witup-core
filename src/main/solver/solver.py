#!/usr/bin/env python3
import sys, json
from sympy import S, And, Or, Not, simplify_logic, sympify, Symbol, Eq, Ne, simplify
from sympy.logic.boolalg import BooleanTrue, BooleanFalse
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
    symbolKinds: dict

class SolverPathResult(TypedDict):
    pathId: str
    status: Literal["SAT", "UNSAT", "UNKNOWN", "ERROR"]
    solutions: List[dict]  # {"symbol": str, "value": str}

class SolverResponse(TypedDict):
    paths: List[SolverPathResult]



# on the shoulders of giants
# https://github.com/mmaaz-git/sym2z/blob/main/sym2z.py
# this is proving a bit painful. We may want to ditch sympy before z3
def sympy_to_z3(expr):
    # sympy will simplify things like (0 == 0) to BooleanTrue or BooleanFalse
    if isinstance(expr, BooleanTrue):
        return True

    if isinstance(expr, BooleanFalse):
        return False

    if isinstance(expr, Eq):
        lhs, rhs = expr.lhs, expr.rhs
        return sympy_to_z3(lhs) == sympy_to_z3(rhs)

    if isinstance(expr, Ne):
        lhs, rhs = expr.lhs, expr.rhs
        return sympy_to_z3(lhs) != sympy_to_z3(rhs)

    symbols = set(expr.free_symbols)
    expr_str = str(expr)
    for s in symbols:
        # Add word boundaries to only replace whole variable names
        var_str = r'\b' + str(s) + r'\b'
        # might need to consider more types here
        if s.is_integer:
            expr_str = re.sub(var_str, f"z3.Int('{s}')", expr_str)
        else:
            expr_str = re.sub(var_str, f"z3.Real('{s}')", expr_str)

    expr_str = expr_str.replace("And", "z3.And")
    expr_str = expr_str.replace("Or", "z3.Or")
    expr_str = expr_str.replace("Not", "z3.Not")

    return eval(expr_str)


def check_feasibility(z3_constraints):
    if not z3_constraints:
        return {"status": "UNKNOWN", "solutions": []}

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

            solutions.append({"symbol": var_name, "value": value_str})

        return {
            "status": "SAT",
            "solutions": solutions,
        }
    elif status == unsat:
        return {"status": "UNSAT", "solutions": []}
    else:
        return {"status": "UNKNOWN", "solutions": []}

def normalise_java_name(expr: str) -> str:
    return re.sub(r"\b([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)\b",
                  lambda m: f"{m.group(1)}_{m.group(2)}", expr)

def normalise_java_expr(expr: str, var_mapping):
    """
    Replace 's.abc' with 's_abc' so sympy/Z3 can handle it.
    Creates a local mapping of all normalised variables and updates the global
    mapping
    """
    local_mapping = {}
    # replace this.abc, a.xyz, etc.
    def field_replacer(match):
        original = match.group(0)
        obj = match.group(1)
        attr = match.group(2)
        normalised = f"{obj}_{attr}"
        local_mapping[normalised] = original
        return normalised

    normalised_expr = re.sub(
        r"\b([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)\b",
        field_replacer,
        expr
    )

    # replace arr[i] with arr_i
    def array_replacer(match):
        original = match.group(0)
        array_name = match.group(1)
        index_expr = match.group(2)

        index_norm = (
            index_expr
            .replace("+", "_plus_")
            .replace("-", "_minus_")
            .replace("*", "_mul_")
            .replace("/", "_div_")
            .replace(" ", "")
        )

        normalised = f"{array_name}_{index_norm}"
        local_mapping[normalised] = original
        return normalised

    normalised_expr = re.sub(
        r"\b([a-zA-Z_][a-zA-Z0-9_]*)\s*\[\s*([^\]]+)\s*\]",
        array_replacer,
        normalised_expr
    )

    def cast_replacer(match):
        original = match.group(0)
        cast_type = match.group(1)
        operand = match.group(2)
        normalised = f"{cast_type}_{operand}"
        local_mapping[normalised] = original
        return normalised

    normalised_expr = re.sub(
        r"\(([a-zA-Z_][a-zA-Z0-9_]*)\)([a-zA-Z_][a-zA-Z0-9_]*)",
        cast_replacer,
        normalised_expr
    )

    var_mapping.update(local_mapping)
    return normalised_expr


def denormalise_solutions(solutions, mapping):
    for sol in solutions:
        name = sol["symbol"]
        if name in mapping:
            sol["symbol"] = mapping[name]
    return solutions


def extract_symbols(expr: str) -> set[str]:
    """
    Extract variable identifiers from an expression string.
    Assumes Java-like identifiers after normalisation.
    """
    return set(re.findall(r"\b[a-zA-Z_][a-zA-Z0-9_]*\b", expr))


def build_numeric_constraint(expr_str: str, truth_value: bool, symbol_kinds):
    match = re.fullmatch(r"(\w+)\s*(==|!=)\s*0", expr_str)
    if match:
        var_name, op = match.groups()
        if symbol_kinds.get(var_name) == "BOOLEAN":
            sym = Symbol(var_name, bool=True)
            # booleans: is_Empty == 0 -> Not(is_Empty), is_Empty != 0 -> is_Empty
            # expr = Symbol(var_name, bool=True)
            expr = Not(sym) if (op == "==" and truth_value) else sym
            return expr if truth_value else Not(expr)
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
    # {"condition":"(s != 'abc')","truthValue":False}. let's see how it goes
    return "'" in expr_str or '"' in expr_str


# def build_constraint(expr_str: str, truth_value: bool):
#     if contains_string_literal(expr_str):
#         return build_string_constraint(expr_str, truth_value)
#     else:
#         return build_numeric_constraint(expr_str, truth_value)

# This is becoming a monster and it's gonna need a monster refactor some time
# DEPRECATED
def main():
    try:
        raw = sys.stdin.read()
        request: SolverRequest = json.loads(raw)
        # request = {"paths":[{'pathId': '<br.unb.cic.witup.samples.Array: int getElement(int[],int)>#0', 'conditions': [{'condition': '(arr[i] != 0)', 'truthValue': False}]}],"symbolKinds":{"arr[i]":"OTHER"}}
    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse JSON request: {e}")
        sys.exit(1)

    paths: List[SolverPath] = request.get("paths", [])
    logger.info(f"Received {len(paths)} paths for solving")

    symbol_table = {}
    symbol_kinds: dict = request.get("symbolKinds", {})
    response: SolverResponse = {"paths": []}
    normalised_symbol_kinds: dict = {}

    for name, kind in symbol_kinds.items():
        normalised_name = normalise_java_name(name)
        normalised_symbol_kinds[normalised_name] = kind

    for path in paths:
        path_id = path.get("pathId", "<unknown>")
        conditions = path.get("conditions", [])
        logger.info(f"[{path_id}] Starting symbol resolution ({len(conditions)} conditions)")
        logger.info(path)
        # track normalised names
        var_mapping = {}
        sympy_exprs = []
        z3_exprs = []
        try:
            for c in conditions:
                normalised = normalise_java_expr(c["condition"], var_mapping)
                kind = normalised_symbol_kinds.get(normalised)

                if contains_string_literal(normalised):
                    # strings skip sympy
                    expr = build_string_constraint(normalised, c["truthValue"])
                    z3_exprs.append(expr)

                elif kind == "BOOLEAN_METHOD" or kind == "BOOLEAN":
                    expr = z3.Bool(normalised)
                    if not c["truthValue"]:
                        expr = z3.Not(expr)
                    z3_exprs.append(expr)

                else:
                    expr = build_numeric_constraint(normalised, c["truthValue"], normalised_symbol_kinds)
                    expr = simplify(expr)
                    if isinstance(expr, BooleanTrue):
                        z3_exprs.append(True)
                    elif isinstance(expr, BooleanFalse):
                        z3_exprs.append(False)
                    sympy_exprs.append(expr)
                    for name in extract_symbols(normalised):
                        if name not in symbol_table:
                            symbol_table[name] = Symbol(name)

            if sympy_exprs:
                combined_numeric_expr = And(*sympy_exprs)
                simplified = simplify_logic(combined_numeric_expr, form="dnf")
                z3_constraints = [sympy_to_z3(simplified)] + z3_exprs
            else:
                z3_constraints = z3_exprs

            logger.info(f"[{path_id}] Starting Z3 model checking")
            result = check_feasibility(z3_constraints)

            result["solutions"] = denormalise_solutions(result["solutions"], var_mapping)
            logger.info(f"[{path_id}] Z3 result: {result['status']}")

        except Exception as e:
            logger.error(f"[{path_id}] ERROR during solving: {e}")
            result = {"status": "ERROR", "solutions": []}

        response["paths"].append({
            "pathId": path_id,
            **result
        })

    print(json.dumps(response))

if __name__ == "__main__":
    main()