"""Helper functions for parsing and analyzing C code."""

import re


def extract_includes(code: str) -> list[str]:
    """Extract all #include directives from the code."""
    pattern = r'#include\s*[<"]([^>"]+)[>"]'
    return re.findall(pattern, code)


def extract_functions(code: str) -> list[str]:
    """Extract function names defined in the code (simplistic)."""
    pattern = r'\b(\w+)\s+\**(\w+)\s*\([^)]*\)\s*\{'
    matches = re.findall(pattern, code)
    return [m[1] for m in matches]


def count_lines(code: str) -> int:
    """Return the number of non-empty lines."""
    return len([l for l in code.split("\n") if l.strip()])
