"""JSON output schema definition for LLM grading responses."""

GRADING_SCHEMA = {
    "type": "object",
    "properties": {
        "total_score": {
            "type": "number",
            "description": "Overall score (0-100)",
            "minimum": 0,
            "maximum": 100,
        },
        "correctness_score": {
            "type": "number",
            "description": "Correctness score — how well the code solves the problem",
            "minimum": 0,
            "maximum": 100,
        },
        "style_score": {
            "type": "number",
            "description": "Code style score — naming, formatting, readability",
            "minimum": 0,
            "maximum": 100,
        },
        "efficiency_score": {
            "type": "number",
            "description": "Efficiency score — algorithm choice, time/space complexity",
            "minimum": 0,
            "maximum": 100,
        },
        "summary": {
            "type": "string",
            "description": "Brief overall assessment in Chinese (2-3 sentences)",
        },
        "line_annotations": {
            "type": "array",
            "description": "Per-line annotations for the submitted code",
            "items": {
                "type": "object",
                "properties": {
                    "line": {"type": "integer", "description": "1-indexed line number"},
                    "severity": {
                        "type": "string",
                        "enum": ["error", "warning", "info", "praise"],
                    },
                    "message": {"type": "string", "description": "Annotation text in Chinese"},
                },
                "required": ["line", "severity", "message"],
            },
        },
        "improvements": {
            "type": "array",
            "description": "Suggested improvements sorted by priority (most important first)",
            "items": {
                "type": "object",
                "properties": {
                    "priority": {"type": "string", "enum": ["high", "medium", "low"]},
                    "category": {
                        "type": "string",
                        "enum": ["correctness", "style", "efficiency", "knowledge"],
                    },
                    "title": {"type": "string", "description": "Short title of the improvement"},
                    "detail": {"type": "string", "description": "Detailed explanation with example if applicable"},
                },
                "required": ["priority", "category", "title", "detail"],
            },
        },
    },
    "required": ["total_score", "correctness_score", "style_score", "efficiency_score", "summary"],
}

SCHEMA_DESCRIPTION = """
You MUST return a valid JSON object following the schema above.
- total_score = correctness_score * 0.6 + style_score * 0.2 + efficiency_score * 0.2
- line_annotations: annotate specific lines with errors or suggestions
- improvements: 2-5 actionable suggestions sorted by priority
- All descriptive text MUST be in Chinese
"""
