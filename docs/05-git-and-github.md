# Git & GitHub (week 1)

> **When to read this doc**
> **Day 0** — "One-time setup" only, so the first commit has your name on it.
> **Day 4** — everything from "Push this project" to "Branching model".
> **Day 5** — "Repo hygiene that CI depends on", alongside `ci.yml`.
> **Day 10** — "GHCR — where your built images live".
> **Any day** — the commands table, whenever Git surprises you.
> Day-by-day map for the whole lab: `00-START-HERE.md`.

Git is the spine of everything else — every pipeline in this course is triggered
by a push. Spend real time here; people who are shaky on Git are shaky on CI.

## One-time setup

> _When: **Day 0.** Do it before your first commit, not after._

```bash
git config --global user.name  "Mike Lam"
git config --global user.email "lamkw.mike@gmail.com"
git config --global init.defaultBranch main
git config --global pull.rebase true          # linear history, fewer merge bubbles
git config --global core.editor "nano"

ssh-keygen -t ed25519 -C "mike@laptop-github"
cat ~/.ssh/id_ed25519.pub    # paste into GitHub → Settings → SSH and GPG keys
ssh -T git@github.com        # "Hi mike! You've successfully authenticated"
```

## Push this project

> _When: **Day 4.**_

```bash
cd ~/projects/devops-lab
git init
git add .
git commit -m "chore: initial task-api scaffold"
gh repo create devops-lab --public --source=. --push   # or create it on github.com
```

## The daily loop — practise it until it is automatic

> _When: **Day 4** to learn it, then **every day for the rest of the lab**. Every pipeline run you trigger starts here._

```bash
git switch -c feature/add-priority-field
# ...edit...
git status
git diff                       # unstaged changes
git add -p                     # stage hunk by hunk — forces you to read your own diff
git commit -m "feat: add priority field to Task"
git push -u origin feature/add-priority-field
gh pr create --fill            # or open the PR in the browser
# CI runs → review → squash merge → delete branch
git switch main && git pull
```

## Commands you will actually need under pressure

> _When: **Reference — any day.** Bookmark it; don't memorise it._

| Situation | Command |
|---|---|
| "What did I change?" | `git diff`, `git diff --staged` |
| "What happened here?" | `git log --oneline --graph --all -20` |
| "Who wrote this line and why?" | `git blame <file>` then `git show <sha>` |
| Undo an unstaged edit | `git restore <file>` |
| Unstage without losing work | `git restore --staged <file>` |
| Fix the last commit message | `git commit --amend` |
| Undo the last commit, keep changes | `git reset --soft HEAD~1` |
| Park work to switch branches | `git stash` / `git stash pop` |
| Bring one commit onto this branch | `git cherry-pick <sha>` |
| Undo a commit already pushed | `git revert <sha>` (never `reset --hard` on a shared branch) |
| "I've completely lost a commit" | `git reflog` — it is almost always still there |

## Resolve a merge conflict on purpose

> _When: **Day 4.** Deliberately, while nothing is at stake._

Do this deliberately in week 1 so the first real one is not a panic:

```bash
git switch main && echo "A" >> README.md && git commit -am "main edit"
git switch -c conflict-demo main~1 && echo "B" >> README.md && git commit -am "branch edit"
git merge main            # CONFLICT
# edit README.md, remove <<<<<<< ======= >>>>>>> markers, keep what you want
git add README.md && git commit
```

## Branching model to use

> _When: **Day 4.** Decide now; Day 5's branch protection depends on it._

`main` is always deployable. Feature branches, short-lived, merged via PR.
That is **GitHub Flow**, and it is what most UK teams run. GitFlow (develop /
release / hotfix branches) is worth *understanding* for interviews but do not
inflict it on a solo project.

## GHCR — where your built images live

> _When: **Day 10** — the roadmap's "Registry, scanning, and CI that ships"._

Your GitHub account gives you a container registry as well as a Git host:
**GHCR**, at `ghcr.io`. The CI workflow pushes there; the Pi pulls from there.
Read an image reference left to right — it is four fields, not one blob:

```
ghcr.io  /  <you>  /  devops-lab  :  latest
   │          │           │           │
registry  namespace   repository     tag
```

| Field | What it is |
|---|---|
| `ghcr.io` | The **registry** — the server storing images. Docker assumes a default only for Docker Hub, so every other registry has to be written out. `docker pull postgres` is really `docker.io/library/postgres:latest`. |
| `<you>` | The **namespace** — your GitHub username or org. GHCR requires it to match the account that owns the package. Never type it in CI: `${{ github.repository }}` already expands to `owner/repo`. |
| `devops-lab` | The **repository** — one named bucket holding every version of this image. |
| `latest` | The **tag** — a movable label pointing at one version in that bucket. It means nothing special to Docker; it is just the string used when you don't supply one. |

### Tag `:latest` *and* the commit SHA

`.github/workflows/ci.yml` pushes both on every build:

```yaml
tags: |
  ghcr.io/${{ env.IMAGE_NAME }}:${{ github.sha }}
  ghcr.io/${{ env.IMAGE_NAME }}:latest
```

`latest` moves with every merge to `main`, so it is convenient and ambiguous.
The SHA tag is immutable and answers the question you will eventually have to
answer under pressure: *which commit is actually running on the Pi right now?*
Deploy by SHA, keep `latest` for convenience. Beneath both tags the real
identity is a content digest, `sha256:…` — that is what a rollback pins to, and
what a multi-arch manifest list points at (see `02-environment-raspberry-pi.md`).

### The first pull from the Pi will fail — that's expected

New GHCR packages are **private**, and the Pi is a different machine from the
laptop that pushed. You get `denied` until you do one of:

```bash
# Option A — make it public:
#   GitHub → your profile → Packages → devops-lab → Package settings
#   → Change visibility

# Option B — authenticate on the Pi with a PAT scoped to read:packages
echo "$GHCR_TOKEN" | docker login ghcr.io -u <you> --password-stdin
```

Option B is the one worth practising: it is how a real deployment host reaches a
private registry, and it is the same shape as the credential you later hand to
Jenkins and to Kubernetes as an `imagePullSecret`.

Finally, link the package back to the repo so its page shows the code that built
it — add to the `Dockerfile`:

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/<you>/devops-lab"
```

## Repo hygiene that CI depends on

> _When: **Day 5**, together with `.github/workflows/ci.yml`._

- Branch protection on `main`: require the CI check to pass, require a PR
  (Settings → Branches → Add rule). Then try to push straight to `main` and watch
  it get rejected — that is the "pipeline as gatekeeper" lesson.
- Conventional commit prefixes (`feat:`, `fix:`, `chore:`, `ci:`) — cheap, and
  later lets tooling generate changelogs.
- `.gitignore` before the first commit. Committing `target/` once and having to
  purge it is a rite of passage you can skip.
- **Never** commit `.env`, kubeconfigs, or a Docker Hub token. If you do:
  rotate the credential immediately — deleting the commit is not enough, it is
  already in the reflog and in anyone's clone.
