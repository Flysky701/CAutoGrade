# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An LLM-powered C programming homework auto-grading system ("C语言程序设计" course). Students submit C code through Monaco Editor; the system grades it automatically via a three-layer pipeline (static analysis + LLM evaluation + RAG enhancement) and returns structured feedback (score + line annotations + improvement suggestions). Three roles: Student, Teacher, Admin.

## Build & Run Commands

### Backend (Spring Boot 3.2, Java 17, Maven)
```bash
cd backend
mvn clean install          # build
mvn spring-boot:run        # run (port 8080)
mvn test                   # run tests
mvn test -Dtest=ClassName  # run single test class
```

### Frontend (Vue 3, TypeScript, Vite)
```bash
cd frontend
npm run dev       # dev server (port 3000, proxies /api → localhost:8080)
npm run build     # type-check + production build
npm run preview   # preview production build
```

### Grading Engine (Python, Celery)
```bash
cd grading-engine
.\venv\Scripts\Activate.ps1    # activate virtualenv (Windows)
pip install -r requirements.txt

# Start Celery worker
celery -A celery_app worker --loglevel=info --concurrency=4

# Run a single grading task for testing
python -c "from tasks.grading_task import grade_code; grade_code.delay(1, '#include <stdio.h>...', 1)"
```

### Docker (MySQL 8.0 + Redis 7.0 + Backend + Grading Worker)
```bash
docker-compose up -d                       # start all services
docker-compose up -d mysql redis           # start infrastructure only
```

## Architecture

```
Frontend (Vue3, port 3000)
  → Nginx / Vite proxy
    → Backend API (Spring Boot, port 8080)
        → MySQL 8.0 (business data)
        → Redis (cache + Celery broker)
        → Celery Worker (Python grading-engine)
            → Static Analyzer (clang-tidy + Docker sandbox)
            → RAG Service (Chroma vector DB — planned)
            → LLM Service (DeepSeek API / OpenAI fallback)
            → Rule Scorer (fallback when LLM deviation exceeds threshold)
```

### Grading Pipeline (three-layer)
1. **Static Analysis** — syntax check via clang-tidy, compile + run test cases in Docker sandbox
2. **RAG Enhancement** — retrieve course knowledge + grading rubrics from vector store, inject into LLM context
3. **LLM Evaluation** — structured prompt with code + static results + RAG context → JSON grading result (correctness / style / efficiency scores)

### Entity dual-mapping pattern
Entities use **both** JPA and MyBatis-Plus annotations on the same class. `@Entity` + `@Table` for JPA, `@TableName` + `@TableId` + `@TableField` + `@TableLogic` for MyBatis-Plus. Mappers extend `BaseMapper<T>` (MyBatis-Plus style, no XML needed). JPA repositories (`JpaRepository`) exist alongside mappers for specific queries. JPA `ddl-auto: none` — the schema is managed entirely by `docker/mysql/init.sql` (13 tables + default admin user).

### API response pattern
All controllers return `Result<T>` — a generic wrapper with `code` (200 = success), `msg`, `data`. Controllers use constructor injection (no `@Autowired`). DTOs use `@Valid` for validation. Controller `@RequestMapping` paths include the `/api` prefix (e.g., `@RequestMapping("/api/auth")`).

### Auth & Security
- Stateless JWT: `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`
- `SecurityConfig` permits `/api/auth/**`, `/doc.html`, and Swagger/Knife4j paths; all others require authentication
- JWT secret and 24h expiration configured in `application.yml` (`jwt.secret`, `jwt.expiration`)
- Frontend router guards check `localStorage` token + role, redirect on mismatch
- Password hashing via `BCryptPasswordEncoder`

### Frontend structure
- Three role-based view trees: `/student/*`, `/teacher/*`, `/admin/*` — each with its own `Layout.vue`
- `api/request.ts` — Axios instance with `baseURL: http://localhost:8080/api`, token interceptor, auto-redirects on 401. Note: the Vite dev proxy (`/api` → `localhost:8080`) is configured but bypassed by this absolute baseURL; CORS on the backend handles cross-origin dev requests.
- `api/index.ts` — single module exporting all API functions (one per resource)
- Stores (Pinia) planned but not all implemented yet
- `@` alias maps to `src/`
- Monaco Editor for code submission, ECharts for analytics charts, Element Plus for UI components

### Config files
- `backend/src/main/resources/application.yml` — main Spring Boot config (JPA, MyBatis-Plus, JWT, Knife4j)
- `backend/src/main/resources/application-dev.yml` — dev profile: localhost MySQL/Redis, debug logging
- `backend/src/main/resources/application-prod.yml` — prod profile: Docker service hostnames
- `grading-engine/config.yaml` — Redis URL, LLM provider (DeepSeek), compile/run timeouts (5s), memory limit (256MB)
- `grading-engine/celery_app.py` — Celery app init, task routing (`grading_queue`), registered tasks
- `docker/mysql/init.sql` — full schema (13 tables) + default admin user

## Project Status

The plan.md is the **complete design document**; the codebase is still in active development. Core infrastructure (auth, entities, controllers, services, mappers) is scaffolded. Grading engine core modules (`dispatcher.py`, `llm_service.py`, `sandbox.py`, `static_analyzer.py`, `rag_service.py`, `scorer.py`) are implemented. Frontend views have skeleton pages with Element Plus components. Docker Compose starts MySQL, Redis, backend, and grading worker (sandbox + Chroma are TODO per docker-compose.yml comments).

## Copilot Instructions

Per `.github/copilot-instructions.md`: call the `#askQuestions` tool after every output. When providing suggestions during code analysis, use Chinese for descriptions.
