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
    {
        "code": """#include <stdio.h>
#include <string.h>
int main() {
    char s[1001];
    int n, i, cnt = 0;
    scanf("%d", &n);
    while (n--) {
        scanf("%s", s);
        int len = strlen(s);
        int valid = 1;
        for (i = 0; i < len / 2; i++) {
            if (s[i] != s[len - 1 - i]) {
                valid = 0;
                break;
            }
        }
        if (valid) cnt++;
    }
    printf("%d\\n", cnt);
    return 0;
}""",
        "problem": "给定n个字符串，统计其中回文串的数量",
        "static_result": {
            "compile_success": True,
            "passed_cases": 5,
            "total_cases": 5,
            "warnings": ["Use of unsafe scanf with %s (buffer overflow risk)"],
        },
        "expected_output": {
            "total_score": 82,
            "correctness_score": 100,
            "style_score": 55,
            "efficiency_score": 70,
            "summary": "代码功能完全正确，通过了全部5个测试用例。使用了不安全的scanf %s读取字符串，存在缓冲区溢出风险。变量命名基本合理，但缺少注释。算法效率一般，对于长字符串可以提前终止比较。",
            "line_annotations": [
                {"line": 5, "severity": "warning", "message": "使用scanf %s读取字符串不安全，建议改用fgets或限制读取长度%1000s"},
                {"line": 8, "severity": "info", "message": "可以添加对字符串长度的判断，空串和单字符串也是回文"},
                {"line": 10, "severity": "praise", "message": "回文判断逻辑正确，使用了双指针从两端向中间比较"},
            ],
            "improvements": [
                {
                    "priority": "high",
                    "category": "style",
                    "title": "使用安全的字符串输入",
                    "detail": "将scanf(\"%s\", s)改为fgets(s, sizeof(s), stdin)并去除末尾换行，或至少使用scanf(\"%1000s\", s)限制长度。",
                },
                {
                    "priority": "medium",
                    "category": "efficiency",
                    "title": "优化回文判断",
                    "detail": "当前算法O(n*len)已经合理，但可以在发现不匹配时立即break（已实现），这一点做得不错。",
                },
                {
                    "priority": "low",
                    "category": "style",
                    "title": "添加注释",
                    "detail": "在关键逻辑处添加注释说明，如回文判断的思路、变量含义等。",
                },
            ],
        },
    },
]
