# Troubleshooting playbook

Debugging *is* the skill. This is the order to work through, and the commands that
answer each question. Keep it open while you work.

## Docker

| Symptom | First command | Usual cause |
|---|---|---|
| Container exits immediately | `docker logs <name>` | app crashed on startup — read the stack trace |
| `Exited (137)` | `docker stats`, `docker inspect <name>` | OOMKilled. Raise memory or lower `-Xmx` |
| Can't reach app on localhost | `docker ps` (check the PORTS column) | forgot `-p 8080:8080`, or app binds 127.0.0.1 inside the container instead of 0.0.0.0 |
| `connection refused` to the DB | `docker network inspect <net>` | used `localhost` instead of the service name (`db`) |
| Build is slow every time | check `.dockerignore`, layer order | `COPY . .` before `mvn dependency:go-offline` busts the cache |
| Image is 900MB | `docker history <image>` | single-stage build shipping the whole JDK |
| `exec format error` on the Pi | `docker inspect <image> \| grep Architecture` | AMD64 image on ARM64 — rebuild with buildx |
| Disk full | `docker system df` then `docker system prune -a --volumes` | dangling images and volumes |

Useful anywhere: `docker exec -it <name> sh` to get a shell inside a running
container. If the image has no shell (distroless), that is by design.

## Kubernetes — always in this order

```bash
kubectl -n taskapi get pods                    # 1. what state?
kubectl -n taskapi describe pod <pod>          # 2. Events: at the bottom — read these first
kubectl -n taskapi logs <pod>                  # 3. app output
kubectl -n taskapi logs <pod> --previous       # 3b. output of the crashed instance
kubectl -n taskapi get events --sort-by=.lastTimestamp
```

| Pod status | Meaning | Fix |
|---|---|---|
| `ImagePullBackOff` | can't fetch the image | wrong tag/registry, or private repo needs an imagePullSecret |
| `CrashLoopBackOff` | starts then dies, repeatedly | `logs --previous`. Usually config: DB URL, missing env var |
| `Pending` | nothing will schedule it | `describe` → "Insufficient cpu/memory", or an unbound PVC |
| `Running` but not `Ready` | readiness probe failing | wrong path/port, or probe timeout shorter than app startup |
| `OOMKilled` | exceeded memory limit | raise `limits.memory`, or set `-XX:MaxRAMPercentage` |
| `Error: ErrImageNeverPull` | using kind/k3d local image | `k3d image import <image> -c devops-lab` |

Networking:

```bash
kubectl -n taskapi get svc,endpoints          # no endpoints = selector doesn't match pod labels
kubectl -n taskapi port-forward svc/task-api 8080:80
kubectl -n taskapi run tmp --rm -it --image=busybox -- sh   # then: wget -qO- http://task-api
```

Rollouts:

```bash
kubectl -n taskapi rollout status deployment/task-api
kubectl -n taskapi rollout history deployment/task-api
kubectl -n taskapi rollout undo deployment/task-api        # the "revert prod" button
```

## Jenkins

| Symptom | Cause |
|---|---|
| `docker: not found` in the pipeline | Docker CLI not installed in the Jenkins container, or `jenkins` user not in the `docker` group |
| `permission denied /var/run/docker.sock` | same — `usermod -aG docker jenkins` then restart the container |
| `Tool type "maven" does not have an install named "maven3"` | the name in `tools { }` doesn't match Manage Jenkins → Tools |
| Credentials "not found" | credential ID typo, or it was created in a folder scope instead of Global |
| Build hangs forever | no `timeout` option; also check for an unanswered `input` step |
| Webhook never fires | GitHub can't reach localhost — poll SCM or use ngrok |

## Spring Boot in a container

| Symptom | Cause |
|---|---|
| App works locally, fails in compose | `localhost` in `SPRING_DATASOURCE_URL` — inside a container that means *the container itself* |
| Startup >60s then killed | k8s `livenessProbe` firing before the JVM is up — that is what `startupProbe` is for |
| `Connection refused` to Postgres at boot | app started before the DB was ready — `depends_on: condition: service_healthy` |

## The general method

1. **Read the actual error.** Not the last line — the first line, and the `Caused by:`.
2. **Narrow the layer.** App? Container? Network? Cluster? Prove each one in turn.
3. **Change one thing.** Then re-test. Two changes at once and you learn nothing.
4. **Write it down.** Keep a `LEARNINGS.md` in the repo. Every entry is an interview
   story: "we had pods stuck in CrashLoopBackOff and it turned out to be…"
