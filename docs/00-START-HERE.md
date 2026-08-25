# Start here — how this lab fits together

Nothing in `docs/` is meant to be read front to back. There are four kinds of
material here and they play different roles:

| Thing | Role | How to read it |
|---|---|---|
| `devops-roadmap.html` ("Twenty Days to Kubernetes") | **The spine.** What you do each day, in order. | One day at a time. This is the only sequential document. |
| `README.md` | The app you'll be shipping — endpoints, repo layout, how to run it. | Once, on Day 0. Refer back for the API table. |
| `docs/0X-*.md` | **Reference.** Setup and background for one environment or tool. | Only when the roadmap sends you there. Skip the rest. |
| `docs/06-troubleshooting.md` | The list of things that go wrong. | When something breaks. Look here *before* searching the web. |

The mental model: **the roadmap tells you what, the docs tell you how.** A day in
the roadmap might say "get the pipeline pushing an image the Pi can run" — the
matching doc holds the commands, the failure modes, and the reason it works.

## When to open which doc

| Day | Roadmap topic | Open |
|---|---|---|
| **0** | Setup checklist | `README.md`, then `01-environment-windows-wsl2.md` end to end |
| 1 | Filesystem, permissions, text | `01` — the "Linux skills to drill" list |
| 2 | Processes, services, networking | `01` — same list |
| 3 | SSH, the Pi, and shell scripting | `02-environment-raspberry-pi.md` — "Guard rails", "Finding the Pi", "SSH hygiene". **Stop before the multi-arch section.** |
| 4 | Git properly | `05-git-and-github.md` — everything down to "Branching model" |
| 5 | First pipeline: GitHub Actions | `05` — "Repo hygiene that CI depends on"; skim `.github/workflows/ci.yml` |
| 6 | Images, containers, layers | `README.md` repo tour; read the `Dockerfile` comments |
| 7 | Dockerfile craft | the `Dockerfile` itself |
| 8 | Networks, volumes, data that survives | `docker-compose.yml` |
| 9 | Compose, and the ARM64 lesson | `02` — the **whole multi-arch section**. This is the day it matters. |
| 10 | Registry, scanning, CI that ships | `05` — "GHCR — where your built images live"; `02` — "Deploy target A" |
| 11 | Stand Jenkins up | `03-jenkins-setup.md` — "Start it" through "Tools" |
| 12 | Your first Jenkinsfile | `03` — plus the `Jenkinsfile` |
| 13 | Credentials, images, triggers | `03` — "Credentials", "Trigger builds automatically" |
| 14 | Quality gates and approvals | `03` — "Exercises" 2 and 3 |
| 15 | Consolidate and compare | nothing new — write up Jenkins vs Actions |
| 16 | Cluster, pods, deployments, services | `01` — "kubectl + a local cluster"; `k8s/` manifests |
| 17 | Config, secrets, storage, probes | `k8s/10-config.yaml`, `k8s/20-postgres.yaml` |
| 18 | Rollouts, scaling, ingress | `k8s/30-task-api.yaml` |
| 19 | The Pi cluster, and the cloud | `02` — "Deploy target B (k3s)"; `04-environment-cloud.md` |
| 20 | Observability, and telling the story | `README.md` — `/actuator/prometheus`; write `LEARNINGS.md` |

## The three environments, and why there are three

| | Machine | Role | Rule |
|---|---|---|---|
| Env 1 | Windows laptop (WSL2) | **dev** — you write and build here | Doc `01`. Weeks 1–3 live here. |
| Env 2 | Raspberry Pi 5 | **production** — a real remote ARM64 Linux server | Doc `02`. Nothing reaches it except through a pipeline. |
| Env 3 | Cloud free tier | **exposure** — the vocabulary job adverts use | Doc `04`. Week 4, optional. |

Docs `01`, `02` and `04` are one per environment. Docs `03` (Jenkins) and `05`
(Git/GitHub) are one per tool, and both live on the laptop.

## If you only remember one thing

Open the roadmap, find today, and follow its links. The `docs/` folder is a
reference shelf — you are not behind because you haven't read all of it.
