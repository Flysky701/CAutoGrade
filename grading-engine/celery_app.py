import os
import yaml
from celery import Celery

def load_config():
    config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
    if os.path.exists(config_path):
        with open(config_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    return {}

config = load_config()
redis_url = config.get('redis', {}).get('url', 'redis://localhost:6379/1')

# 初始化 Celery
app = Celery('grading_engine',
             broker=redis_url,
             backend=redis_url,
             include=['tasks.grading_task', 'tasks.consistency_check_task', 'tasks.notification_task'])

# 可选配置 Celery
app.conf.update(
    task_routes={
        'tasks.grading_task.grade_code': {'queue': 'grading_queue'},
    },
    task_serializer='json',
    accept_content=['json'], 
    result_serializer='json',
    timezone='Asia/Shanghai',
    enable_utc=True,
)

if __name__ == '__main__':
    app.start()
