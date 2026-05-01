import re
from dataclasses import dataclass, field
from core.sandbox import SandboxRunner


@dataclass
class TestCaseResult:
    case_id: int
    passed: bool
    input_data: str
    expected: str
    actual: str
    weight: int


@dataclass
class StaticAnalysisResult:
    compile_success: bool
    compile_error: str
    passed_cases: int
    total_cases: int
    warnings: list[str]
    test_case_results: list[TestCaseResult] = field(default_factory=list)


class StaticAnalyzer:
    UNSAFE_PATTERNS = [
        (r'\bgets\s*\(', "Use of unsafe function: gets()"),
        (r'\bscanf\s*\(\s*["\']%s["\']', "Use of unsafe scanf with %s (buffer overflow risk)"),
        (r'\bstrcpy\s*\(', "Use of unsafe function: strcpy()"),
        (r'\bstrcat\s*\(', "Use of unsafe function: strcat()"),
        (r'\bsprintf\s*\(', "Use of unsafe function: sprintf()"),
    ]

    def __init__(self):
        self.sandbox = SandboxRunner()

    def _check_code_quality(self, code: str) -> list[str]:
        warnings = []
        for pattern, message in self.UNSAFE_PATTERNS:
            if re.search(pattern, code):
                warnings.append(message)
        if "main(" not in code:
            warnings.append("Missing main function")
        if "#include" not in code:
            warnings.append("Missing #include directives")
        if "malloc(" in code and "free(" not in code:
            warnings.append("malloc without matching free")
        return warnings

    def _normalize_output(self, text: str) -> str:
        lines = text.rstrip().split("\n")
        return "\n".join(line.rstrip() for line in lines)

    def _run_test_case(self, tmpdir: str, test_case: dict) -> TestCaseResult:
        stdin = test_case.get("input_data", test_case.get("input", ""))
        expected = test_case.get("expected_output", test_case.get("expected", ""))

        result = self.sandbox.run_test(tmpdir, stdin_input=stdin)

        actual = self._normalize_output(result.get("stdout", ""))
        expected_normalized = self._normalize_output(expected)

        passed = actual == expected_normalized

        if not passed:
            display = result.get("stderr", "") or result.get("stdout", "")
        else:
            display = result.get("stdout", "")

        return TestCaseResult(
            case_id=test_case.get("id", 0),
            passed=passed,
            input_data=stdin,
            expected=expected,
            actual=display.strip(),
            weight=test_case.get("weight", 10),
        )

    def analyze(self, code_content: str, test_cases: list[dict] = None) -> StaticAnalysisResult:
        test_cases = test_cases or []
        warnings = self._check_code_quality(code_content)

        compile_result = self.sandbox.compile(code_content)
        tmpdir = compile_result.get("tmpdir")

        if not compile_result["success"]:
            if tmpdir:
                self.sandbox.cleanup(tmpdir)
            return StaticAnalysisResult(
                compile_success=False,
                compile_error=compile_result["error"],
                passed_cases=0,
                total_cases=len(test_cases),
                warnings=warnings,
            )

        results = []
        passed = 0
        try:
            for tc in test_cases:
                result = self._run_test_case(tmpdir, tc)
                results.append(result)
                if result.passed:
                    passed += 1
        finally:
            if tmpdir:
                self.sandbox.cleanup(tmpdir)

        return StaticAnalysisResult(
            compile_success=True,
            compile_error="",
            passed_cases=passed,
            total_cases=len(test_cases),
            warnings=warnings,
            test_case_results=results,
        )
