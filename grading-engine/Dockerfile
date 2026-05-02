FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends supervisor && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt ./
RUN pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple && \
    pip install --no-cache-dir celery==5.3.4 redis==5.0.1 requests==2.31.0 PyYAML==6.0.1 docker==7.0.0 pymysql==1.1.0 cryptography>=41.0 && \
    pip install --no-cache-dir chromadb>=1.5.0 && \
    pip install --no-cache-dir sentence-transformers>=5.0

COPY . .

ENV PYTHONUNBUFFERED=1
ENV PYTHONIOENCODING=utf-8
ENV LANG=C.UTF-8
ENV HF_HOME=/app/.cache/huggingface
ENV HF_ENDPOINT=https://hf-mirror.com

COPY supervisord.conf /etc/supervisor/supervisord.conf

CMD ["supervisord", "-c", "/etc/supervisor/supervisord.conf", "-n"]
