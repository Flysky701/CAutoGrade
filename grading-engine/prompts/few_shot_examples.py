"""Few-shot examples to calibrate LLM grading behavior."""

FEW_SHOT_EXAMPLES = [
    {
        "code": """#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d\\n", a + b);
    return 0;
}""",
        "problem": "输入两个整数，输出它们的和",
        "static_result": {
            "compile_success": True,
            "passed_cases": 2,
            "total_cases": 3,
            "warnings": ["Use of unsafe function: scanf("],
        },
        "expected_output": {
            "total_score": 75,
            "correctness_score": 80,
            "style_score": 65,
            "efficiency_score": 80,
            "summary": "代码功能基本正确，通过了2/3测试用例。使用了不安全的scanf函数，缺少输入提示，变量名不够直观。算法直接明了，效率合理。",
            "line_annotations": [
                {"line": 1, "severity": "info", "message": "包含了正确的头文件"},
                {"line": 3, "severity": "warning", "message": "使用scanf是不安全的，建议使用fgets+sscanf或添加返回值检查"},
                {"line": 3, "severity": "info", "message": "变量名a、b含义不清，建议使用更具描述性的名称如num1、num2"},
                {"line": 4, "severity": "info", "message": "直接输出结果，但未处理可能的溢出情况"},
            ],
            "improvements": [
                {
                    "priority": "high",
                    "category": "correctness",
                    "title": "处理边界条件",
                    "detail": "未命中的测试用例可能是由于未处理大数相加溢出的情况。建议检查两数之和是否超出int范围。",
                },
                {
                    "priority": "medium",
                    "category": "style",
                    "title": "使用安全的输入函数",
                    "detail": "用fgets读取输入再通过sscanf解析，或至少检查scanf的返回值以确保读取成功。",
                },
                {
                    "priority": "low",
                    "category": "style",
                    "title": "改进变量命名",
                    "detail": "变量名应反映其用途，例如将a改为firstNumber，b改为secondNumber。",
                },
            ],
        },
    },
    {
        "code": """#include <stdio.h>
int main() {
    printf("Hello World");
}""",
        "problem": "输入两个整数，输出它们的和",
        "static_result": {
            "compile_success": True,
            "passed_cases": 0,
            "total_cases": 3,
            "warnings": [],
        },
        "expected_output": {
            "total_score": 10,
            "correctness_score": 0,
            "style_score": 50,
            "efficiency_score": 0,
            "summary": "代码未能完成题目要求。程序只输出了固定字符串'Hello World'，完全没有读取输入或计算两数之和。需要重新理解题目要求。",
            "line_annotations": [
                {"line": 3, "severity": "error", "message": "输出'Hello World'与题目要求完全不符，应该读取两个整数并输出它们的和"},
            ],
            "improvements": [
                {
                    "priority": "high",
                    "category": "correctness",
                    "title": "理解题目要求",
                    "detail": "题目要求输入两个整数并计算它们的和。需要：(1)声明变量存储输入，(2)使用输入函数读取数据，(3)计算和并输出。",
                },
            ],
        },
    },
]
