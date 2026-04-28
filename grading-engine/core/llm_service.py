import requests
import json
import logging
from prompts.system_prompt_template import SYSTEM_PROMPT
from prompts.few_shot_examples import FEW_SHOT_EXAMPLES
from prompts.output_schema import GRADING_SCHEMA, SCHEMA_DESCRIPTION

logger = logging.getLogger(__name__)


class LLMService:
    """Encapsulates communication with the LLM (DeepSeek primary, OpenAI fallback)."""

    def __init__(self, api_key, base_url="https://api.deepseek.com/v1", model="deepseek-chat"):
        self.api_key = api_key
        self.base_url = base_url
        self.model = model

    def evaluate(self, code_content, static_result, rag_context):
        """Build the full prompt with context and request grading from the LLM."""
        messages = [
            {"role": "system", "content": SYSTEM_PROMPT},
        ]

        for example in FEW_SHOT_EXAMPLES:
            messages.append({
                "role": "user",
                "content": self._build_prompt(
                    example["code"],
                    example["static_result"],
                    example.get("rag_context", {}),
                    example["problem"],
                ),
            })
            messages.append({
                "role": "assistant",
                "content": json.dumps(example["expected_output"], ensure_ascii=False),
            })

        messages.append({
            "role": "user",
            "content": self._build_prompt(code_content, static_result, rag_context),
        })

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": 0.2,
            "max_tokens": 2000,
            "response_format": {"type": "json_object"},
        }

        logger.info("Sending grading request to LLM...")

        try:
            response = requests.post(
                f"{self.base_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=60,
            )
            response.raise_for_status()

            result = response.json()
            content = result.get("choices", [{}])[0].get("message", {}).get("content", "{}")
            try:
                return json.loads(content)
            except json.JSONDecodeError:
                logger.warning("LLM returned non-JSON content")
                return {"raw": content, "total_score": None}
        except Exception as e:
            logger.error(f"LLM API call failed: {str(e)}")
            raise e

    def _build_prompt(self, code, static_res, rag_context, problem_desc=None):
        prompt = ""
        if problem_desc:
            prompt += f"## 题目要求\n{problem_desc}\n\n"

        if rag_context and rag_context.get("rubric"):
            prompt += f"## 评分标准\n{rag_context.get('rubric', '')}\n\n"
        if rag_context and rag_context.get("knowledge_points"):
            kps = ", ".join(rag_context["knowledge_points"])
            prompt += f"## 涉及知识点\n{kps}\n\n"
        if rag_context and rag_context.get("common_errors"):
            errs = rag_context["common_errors"]
            err_texts = [f"- {e.get('content', str(e))}" if isinstance(e, dict) else f"- {e}" for e in errs[:5]]
            prompt += f"## 常见错误参考\n" + "\n".join(err_texts) + "\n\n"

        prompt += f"""## 静态分析结果
- 编译状态: {'通过' if static_res.get('compile_success') else '失败'}
{f"- 编译错误: {static_res.get('compile_error', '')}" if not static_res.get('compile_success') else ""}
- 通过测试用例: {static_res.get('passed_cases', 0)}/{static_res.get('total_cases', 0)}
{f"- 代码警告: {', '.join(static_res.get('warnings', []))}" if static_res.get('warnings') else ""}

## 学生提交的代码
```c
{code}
```

{SCHEMA_DESCRIPTION}
"""
        return prompt
