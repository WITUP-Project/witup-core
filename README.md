# WITUp
WITUp is a static analyser that determines the conditions that can cause Java methods to throw

# Setup
This project requires Java 21 and Z3, which in turns requires GCC/G++ 13 or 
earlier to compile. The jar can be found in `src/main/solver/z3.jar`. Otherwise
you can compile it.

## Installing Z3
Clone and build
```bash
git clone https://github.com/Z3Prover/z3.git
cd z3
python scripts/mk_make.py --java
cd build
make
make install
```
This will generate the jar in the build folder, as well as two shared objects:
- `libz3.so`
- `libz3java.so`

Export them to your path:
```bash
export LD_LIBRARY_PATH=/absolute/path/to/z3/build:$LD_LIBRARY_PATH
```
shared object. Install it into maven:
```bash
mvn install:install-file \
  -Dfile=com.microsoft.z3.jar \
  -DgroupId=com.microsoft.z3 \
  -DartifactId=z3 \
  -Dversion=4.17.0 \
  -Dpackaging=jar
```

At the moment we do not have a driver to orchestrate the analysis.
Run the tests with `mvn test`.