from z3 import Solver, Int, Real

# --- Real variable ---
y_real = Real('y')
s_real = Solver()
s_real.add(y_real == 0)

print("=== Real version ===")
print("Check:", s_real.check())
print("Model:", s_real.model())
print("Type of y in model:", type(s_real.model()[y_real]))

# --- Int variable ---
y_int = Int('y')
s_int = Solver()
s_int.add(y_int == 0)

print("\n=== Int version ===")
print("Check:", s_int.check())
print("Model:", s_int.model())
print("Type of y in model:", type(s_int.model()[y_int]))