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

    def _compare_output(self, actual: str, expected: str) -> bool:
        if actual == expected:
            return True
        actual_lines = actual.split("\n")
        expected_lines = expected.split("\n")
        if len(actual_lines) != len(expected_lines):
            return False
        for a_line, e_line in zip(actual_lines, expected_lines):
            a_stripped = a_line.strip()
            e_stripped = e_line.strip()
            if a_stripped == e_stripped:
                continue
            try:
                a_num = float(a_stripped)
                e_num = float(e_stripped)
                if abs(a_num - e_num) < 1e-6:
                    continue
                if abs(e_num) > 1e-9 and abs(a_num - e_num) / abs(e_num) < 1e-6:
                    continue
                return False
            except ValueError:
                return False
        return True

    def _run_test_case(self, container_id: str, test_case: dict) -> TestCaseResult:
        stdin = test_case.get("input_data") or test_case.get("input") or ""
        expected = test_case.get("expected_output") or test_case.get("expected") or ""

        result = self.sandbox.run_test(container_id, stdin_input=stdin)

        actual = self._normalize_output(result.get("stdout", ""))
        expected_normalized = self._normalize_output(expected)

        passed = self._compare_output(actual, expected_normalized)

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

    def analyze(self, code_content: str, test_cases: list[dict] = None, language: str = "c") -> StaticAnalysisResult:
        test_cases = test_cases or []
        warnings = self._check_code_quality(code_content)

        compile_result = self.sandbox.compile(code_content, language=language)
        container_id = compile_result.get("container_id")

        if not compile_result["success"]:
            if container_id:
                self.sandbox.cleanup(container_id)
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
                result = self._run_test_case(container_id, tc)
                results.append(result)
                if result.passed:
                    passed += 1
        finally:
            if container_id:
                self.sandbox.cleanup(container_id)

        return StaticAnalysisResult(
            compile_success=True,
            compile_error="",
            passed_cases=passed,
            total_cases=len(test_cases),
            warnings=warnings,
            test_case_results=results,
        )
