# devops-lab — task-api

A deliberately small Spring Boot REST API. The application is *not* the point —
it is the thing you will version, build, containerise, test, ship and operate
while learning Git/GitHub, Linux, Docker, Jenkins, GitHub Actions and Kubernetes.

Keeping the app boring is the whole trick: every problem you hit from week 2
onwards is a DevOps problem, not a Java problem.

## What the app does

| Method | Path              | Notes                                    |
|--------|-------------------|------------------------------------------|
| GET    | `/api/tasks`      | list all; `?done=true` filters           |
| GET    | `/api/tasks/{id}` | 404 when missing                         |
| POST   | `/api/tasks`      | `{"title":"Learn Docker"}` → 201         |
| PUT    | `/api/tasks/{id}` | `{"title":"...","done":true}`            |
| DELETE | `/api/tasks/{id}` | 204                                      |
| GET    | `/api/info`       | build version, environment, **hostname** |
| GET    | `/actuator/health`| plus `/health/liveness`, `/health/readiness` |
| GET    | `/actuator/prometheus` | metrics for the week-4 monitoring lab |

`/api/info` returns the container hostname — curl it in a loop once you have two
pods running and you will *see* the Kubernetes Service load-balancing.

Storage: H2 in memory by default (so `mvn test` needs nothing installed),
Postgres when `SPRING_DATASOURCE_URL` is set (compose and Kubernetes both set it).

## Repository tour

```
pom.xml                     Maven build
src/main/java/...           4 small classes — entity, repository, 2 controllers
src/test/java/...           4 tests, run by every pipeline
Dockerfile                  multi-stage: builder JDK → JRE runtime, non-root
.dockerignore               keeps target/ and .git/ out of the build context
docker-compose.yml          api + postgres, one command
Jenkinsfile                 declarative pipeline: build → image → push → deploy
.github/workflows/ci.yml    the same pipeline on GitHub Actions
k8s/                        namespace, config/secret, postgres, deployment, svc, ingress, HPA
docs/                       environment setup guides
```

## Day 1 — prove it runs

```bash
mvn clean verify          # compiles, runs 4 tests, produces target/task-api-0.0.1-SNAPSHOT.jar
mvn spring-boot:run       # http://localhost:8080/api/tasks
```

```bash
curl -s localhost:8080/api/info
curl -s -XPOST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Learn Docker"}'
curl -s localhost:8080/api/tasks
```

> The first `mvn` run downloads dependencies from Maven Central — it needs
> internet and takes a few minutes. Everything after that is cached.

## Then

```bash
docker build -t task-api:dev .            # week 2
docker compose up -d --build              # week 2
kubectl apply -f k8s/                     # week 4
```

Full week-by-week plan: see **DevOps 4-Week Lab** (the roadmap page).

## Things you must change before pushing anywhere real

- `k8s/30-task-api.yaml` → `image: ghcr.io/CHANGE-ME/devops-lab:latest`
- `k8s/10-config.yaml` → the Secret holds `changeme`; create it from the CLI instead
- `Jenkinsfile` → `IMAGE_NAME`, and the credential IDs
- `.env` (never committed) → `POSTGRES_PASSWORD=...` for compose
