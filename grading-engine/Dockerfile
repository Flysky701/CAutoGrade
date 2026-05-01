FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends supervisor && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PYTHONUNBUFFERED=1
ENV PYTHONIOENCODING=utf-8
ENV LANG=C.UTF-8
ENV HF_HOME=/app/.cache/huggingface
ENV HF_ENDPOINT=https://hf-mirror.com

COPY supervisord.conf /etc/supervisor/supervisord.conf

CMD ["supervisord", "-c", "/etc/supervisor/supervisord.conf", "-n"]
