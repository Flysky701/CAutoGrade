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
    UNSAFE_FUNCTIONS = ["gets(", "scanf(", "strcpy(", "strcat(", "sprintf("]

    def __init__(self):
        self.sandbox = SandboxRunner()

    def _check_code_quality(self, code: str) -> list[str]:
        """Basic code quality checks for common C pitfalls."""
        warnings = []
        lower_code = code.lower()
        for func in self.UNSAFE_FUNCTIONS:
            if func in lower_code:
                warnings.append(f"Use of unsafe function: {func}")
        if "main(" not in lower_code:
            warnings.append("Missing main function")
        if "#include" not in lower_code:
            warnings.append("Missing #include directives")
        if "malloc(" in code and "free(" not in code:
            warnings.append("malloc without matching free")
        return warnings

    def _normalize_output(self, text: str) -> str:
        """Strip trailing whitespace from each line and trailing newlines."""
        lines = text.rstrip().split("\n")
        return "\n".join(line.rstrip() for line in lines)

    def _run_test_case(self, compiled_code: str, test_case: dict) -> TestCaseResult:
        """Run one test case against the compiled code."""
        stdin = test_case.get("input_data", test_case.get("input", ""))
        expected = test_case.get("expected_output", test_case.get("expected", ""))

        result = self.sandbox.run(compiled_code, stdin_input=stdin)

        if not result["compile_success"]:
            return TestCaseResult(
                case_id=test_case.get("id", 0),
                passed=False,
                input_data=stdin,
                expected=expected,
                actual=f"Compilation error: {result['compile_error']}",
                weight=test_case.get("weight", 10),
            )

        actual = self._normalize_output(result["stdout"])
        expected_normalized = self._normalize_output(expected)

        passed = actual == expected_normalized

        if not passed and result["stderr"]:
            display = result["stderr"]
        elif not passed:
            display = result["stdout"].strip()
        else:
            display = result["stdout"].strip()

        return TestCaseResult(
            case_id=test_case.get("id", 0),
            passed=passed,
            input_data=stdin,
            expected=expected,
            actual=display,
            weight=test_case.get("weight", 10),
        )

    def analyze(self, code_content: str, test_cases: list[dict] = None) -> StaticAnalysisResult:
        """
        Compile and run C code against test cases.
        test_cases: list of dicts with keys: id, input_data, expected_output, weight, is_hidden
        """
        test_cases = test_cases or []

        warnings = self._check_code_quality(code_content)

        compile_result = self.sandbox.compile(code_content)
        if not compile_result["success"]:
            return StaticAnalysisResult(
                compile_success=False,
                compile_error=compile_result["error"],
                passed_cases=0,
                total_cases=len(test_cases),
                warnings=warnings,
            )

        results = []
        passed = 0
        for tc in test_cases:
            result = self._run_test_case(code_content, tc)
            results.append(result)
            if result.passed:
                passed += 1

        return StaticAnalysisResult(
            compile_success=True,
            compile_error="",
            passed_cases=passed,
            total_cases=len(test_cases),
            warnings=warnings,
            test_case_results=results,
        )
