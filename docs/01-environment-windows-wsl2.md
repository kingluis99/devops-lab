# Environment 1 — Windows laptop (WSL2 + Docker Desktop)

> **When to read this doc**
> **Day 0** — work through it top to bottom, *except* the k3d/kubectl section.
> **Days 1–2** — live in the "Linux skills to drill" list at the bottom.
> **Day 3** — the shell-scripting line in that list, plus the self-test.
> **Day 16** — come back for "kubectl + a local cluster".
> Day-by-day map for the whole lab: `00-START-HERE.md`.

This is your everyday workbench. Everything in weeks 1–3 happens here.

## Install WSL2 + Ubuntu

> _When: **Day 0.** Nothing else in the lab works until this does._

In **PowerShell as Administrator**:

```powershell
wsl --install -d Ubuntu-24.04
wsl --set-default-version 2
wsl -l -v          # confirm VERSION is 2
```

Reboot when asked, then open **Ubuntu** from the Start menu and create your Linux
user. From now on, "the terminal" means this Ubuntu shell, not PowerShell.

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl wget unzip tree jq htop net-tools
```

## Install Java 21 + Maven inside WSL

> _When: **Day 0.** Needed before the `mvn clean verify` in `README.md`._

```bash
sudo apt install -y openjdk-21-jdk maven
java -version && mvn -version
```

## Install Docker Desktop (Windows side)

> _When: **Day 0** to install — but you first *use* Docker on Day 6._

Download Docker Desktop for Windows, install, then
**Settings → Resources → WSL Integration → enable Ubuntu-24.04**.

Verify from the Ubuntu shell:

```bash
docker run --rm hello-world
docker compose version
```

## Where to keep your code — this matters

> _When: **Day 0.** Get this wrong and you pay for it every day after._

Keep repos in the **Linux** filesystem (`~/projects/...`), not in `/mnt/c/...`.
Cross-filesystem I/O is 5–20× slower and breaks file-permission bits, which will
bite you the first time you `chmod +x` a script.

```bash
mkdir -p ~/projects && cd ~/projects
```

Open the folder in VS Code with the **WSL** extension: `code .` from inside WSL.

## kubectl + a local cluster (week 4)

> _When: **Day 16.** Skip on Day 0 — k3d eats RAM you need for Weeks 1–3._

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# k3d — runs a k3s cluster inside Docker; lightest option on a laptop
curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash
k3d cluster create devops-lab --agents 2 -p "8080:80@loadbalancer"
kubectl get nodes
```

Delete it with `k3d cluster delete devops-lab` when you want the RAM back.

## Linux skills to drill here (week 1)

> _When: **Days 1–3.** Days 1–2 are the drills; the shell-scripting line and the self-test are Day 3._

You do not need a course. Do these until they are muscle memory:

- Navigation and inspection: `pwd cd ls -la find grep -r less tail -f tree`
- Files and permissions: `cp mv rm mkdir chmod chown` — and understand `755` vs `644`
- Pipes and text: `|  >  >>  grep  awk '{print $1}'  sed 's/a/b/g'  sort  uniq -c  wc -l  cut  xargs`
- Processes: `ps aux  top/htop  kill -9  jobs  bg  fg  nohup`
- Services: `systemctl status/start/stop/enable`, `journalctl -u <svc> -f`
- Networking: `ss -tulpn  curl -v  dig  ping  traceroute`
- Users and sudo: `whoami  id  sudo  /etc/passwd`
- Disk: `df -h  du -sh *  lsblk`
- Editor: pick `nano` now, learn `vim` basics (`i`, `Esc`, `:wq`, `/search`) — every
  server has vi and one day you will be on one that has nothing else.
- Shell scripting: shebang, `$1`, `if`/`for`, exit codes, `set -euo pipefail`

**Self-test:** write `healthcheck.sh` that curls `/actuator/health`, exits 0 if the
JSON status is UP and 1 otherwise, logs to a file with a timestamp, and runs every
5 minutes from cron. If you can do that unaided, week 1 Linux is done.
