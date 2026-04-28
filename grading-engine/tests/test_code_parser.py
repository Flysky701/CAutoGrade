"""Tests for C code parsing utilities."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from utils.code_parser import extract_includes, extract_functions, count_lines


class TestExtractIncludes:
    def test_extract_standard_include(self):
        code = '#include <stdio.h>\nint main() { return 0; }'
        includes = extract_includes(code)
        assert "stdio.h" in includes

    def test_extract_multiple_includes(self):
        code = '#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>'
        includes = extract_includes(code)
        assert len(includes) == 3
        assert includes == ["stdio.h", "stdlib.h", "string.h"]

    def test_extract_quoted_include(self):
        code = '#include "myheader.h"\n'
        includes = extract_includes(code)
        assert "myheader.h" in includes

    def test_no_includes(self):
        code = 'int main() { return 0; }'
        includes = extract_includes(code)
        assert includes == []


class TestExtractFunctions:
    def test_extract_main(self):
        code = 'int main() {\n    return 0;\n}'
        funcs = extract_functions(code)
        assert "main" in funcs

    def test_extract_multiple_functions(self):
        code = '''
int add(int a, int b) {
    return a + b;
}
void print_hello() {
    printf("hello");
}
int main() {
    return 0;
}
'''
        funcs = extract_functions(code)
        assert "add" in funcs
        assert "print_hello" in funcs
        assert "main" in funcs

    def test_extract_pointer_return_type(self):
        code = 'char *get_name() {\n    return "hello";\n}'
        funcs = extract_functions(code)
        assert "get_name" in funcs

    def test_no_functions(self):
        code = ''
        funcs = extract_functions(code)
        assert funcs == []


class TestCountLines:
    def test_count_non_empty(self):
        code = 'line1\nline2\n\nline3\n'
        assert count_lines(code) == 3

    def test_empty_string(self):
        assert count_lines("") == 0

    def test_only_whitespace(self):
        assert count_lines("   \n\t\n  ") == 0
