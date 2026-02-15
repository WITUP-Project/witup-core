# WITUp
WITUp is a static analyser that determines the conditions that can cause Java methods to throw

# Setup
This project requires Java 21 or earlier and Python 3.14 or earlier.

Create a virtual environment called `.venv` at the project's root folder.
```bash
cd /path/to/project
python -m venv .venv
source .venv/bin/activate
pip install -r src/main/solver/requirements.txt
```

At the moment we do not have a driver to orchestrate the analysis.
Run the tests with `mvn test`.