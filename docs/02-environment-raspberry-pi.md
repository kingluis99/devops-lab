# Environment 2 — Raspberry Pi 5 as your "production" server

> **When to read this doc**
> This one is organised by *machine*, not by day — it is used on four separate
> days and reading it straight through will not make sense.
> **Day 3** — guard rails, finding the Pi, SSH hygiene. Stop there.
> **Day 9** — the multi-arch section, and only that.
> **Day 10** — Deploy target A (Compose on the Pi).
> **Day 19** — Deploy target B (k3s).
> Day-by-day map for the whole lab: `00-START-HERE.md`.

You already run Nextcloud, Caddy, Tailscale and Docker on the Pi. That makes it
the most valuable box in this course: it is a real Linux server, remote, ARM64,
with finite RAM — which is exactly where DevOps lessons actually land.

Treat the Pi as **production** and the laptop as **dev**. Nothing gets to the Pi
except through a pipeline.

## Guard rails first

> _When: **Day 3**, before you touch anything on the Pi._

Your Pi already has services you care about. Before you start:

```bash
# 1. Snapshot what's running
docker ps -a > ~/before-devops-lab.txt
sudo systemctl list-units --type=service --state=running >> ~/before-devops-lab.txt

# 2. Know your ports — task-api will want 8080
sudo ss -tulpn | grep LISTEN
```

Nextcloud AIO and Caddy already hold 80/443. Give task-api its own port
(e.g. 8081) or let Caddy reverse-proxy a subdomain to it.

## Finding the Pi: `raspberrypi.local` and why it may not resolve

> _When: **Day 3.** Do this the moment `ssh` fails to resolve the name._

`raspberrypi.local` is not configured anywhere. It is resolved at request time by
**mDNS** (multicast DNS — also called Bonjour or Avahi). The Pi runs
`avahi-daemon`, installed by default on Raspberry Pi OS, which answers to
`<hostname>.local` on the local network. Your machine multicasts "who is
raspberrypi?" and the Pi replies with its current IP — which is why the name keeps
working after a new DHCP lease.

**WSL2 breaks this by default.** WSL2 is a NAT'd VM behind Windows, and multicast
does not cross that NAT. So `.local` names commonly fail inside your Ubuntu shell
even though the identical name resolves fine from PowerShell — Windows has its own
mDNS resolver, WSL2 does not reach the LAN's.

Test first:

```bash
ping raspberrypi.local      # "Name or service not known" → use one of the fixes below
```

### Fix 1 — use the IP (blunt, instant)

```bash
# on the Pi
hostname -I
```

### Fix 2 — /etc/hosts + a DHCP reservation (what most people settle on)

Reserve the Pi's IP in your router's DHCP settings so it never moves, then pin the
name inside WSL:

```bash
sudo nano /etc/hosts
# add:   192.168.1.42   raspberrypi.local  pi
```

### Fix 3 — WSL2 mirrored networking (Windows 11)

In `C:\Users\<you>\.wslconfig`:

```ini
[wsl2]
networkingMode=mirrored
```

Then `wsl --shutdown` from PowerShell and reopen Ubuntu. WSL now shares the Windows
network stack and mDNS works.

### Fix 4 — install the resolver

`sudo apt install avahi-daemon libnss-mdns`. Correct on native Linux; under WSL2's
default NAT this often still fails, because the obstacle is the network mode rather
than a missing package.

> This is the same class of problem you meet in week 2 — a name that resolves in one
> network namespace and not another. `raspberrypi.local` from WSL2 and `localhost`
> from inside a container fail for the same underlying reason.

## SSH hygiene (week 1 lab)

> _When: **Day 3.** This is the Day 3 lab itself._

From WSL:

```bash
ssh-keygen -t ed25519 -C "mike@laptop"
ssh-copy-id mike@raspberrypi.local     # or mike@192.168.1.42 — see above
ssh mike@raspberrypi.local             # should not prompt for a password
```

The `-C` value is only a **comment** appended to the `.pub` file. It has no effect on
authentication; it exists so that when `~/.ssh/authorized_keys` on the Pi holds five
keys, you can tell which machine each one came from. Omit it and `ssh-keygen`
defaults to `user@hostname` of the machine you generated on. Useful convention as
the keys multiply: `mike@laptop`, `mike@laptop-github`, `jenkins@ci`.

Then harden — on the Pi, `/etc/ssh/sshd_config`:

```
PasswordAuthentication no
PermitRootLogin no
```

`sudo systemctl restart ssh`. **Keep your current session open** while you test a
second one; locking yourself out of your own server is a classic day-one mistake.

Add a shortcut in `~/.ssh/config` on the laptop:

```
Host pi
    HostName raspberrypi.local     # or the reserved IP if mDNS doesn't resolve
    User mike
    IdentityFile ~/.ssh/id_ed25519
```

Now `ssh pi` and `scp file pi:~/` just work.

## Multi-arch: the thing that will bite you

> _When: **Day 9** — the roadmap's "Compose, and the ARM64 lesson". Referred back to on Day 10 when CI starts pushing images._

### The problem, precisely

A CPU executes machine code written for its **instruction set architecture**
(ISA). Your laptop is x86-64 (Docker calls it `amd64`); the Pi 5 is 64-bit ARM
(`arm64`, also written `aarch64`). The two ISAs share no machine code. A binary
compiled for one is meaningless bytes to the other.

Check what you actually have:

```bash
uname -m                      # laptop: x86_64        Pi: aarch64
dpkg --print-architecture     # laptop: amd64         Pi: arm64
```

> If the Pi says `armv7l`, you installed 32-bit Raspberry Pi OS. Then your target
> is `linux/arm/v7`, not `linux/arm64`, and half the images on Docker Hub won't
> have a build for you. Reflash with the 64-bit image before going further.

**"But Java is portable."** It is — `app.jar` is bytecode and runs on any JVM.
That is exactly the trap. What you ship is not the jar, it is the *image*, and
look at what else is in it (see the `Dockerfile`): a `eclipse-temurin:21-jre-jammy`
base, which is a whole Ubuntu userland — glibc, `sh`, `curl`, and the JVM itself,
`java`, a native ELF executable. All of that is architecture-specific. Portable
application, non-portable container.

### What failure looks like

Two different errors, and knowing which is which saves you an hour:

```
exec /app/app.jar: exec format error
```
The image ran but the kernel refused the binary inside it — you pulled an amd64
image onto the Pi. (Same error class as running a Windows .exe on Linux.)

```
no matching manifest for linux/arm64/v8 in the manifest list entries
```
Docker refused to pull at all — the tag exists, but it has no arm64 entry. This
is the *good* failure: it fails at pull time, not at 3am in a restart loop.

### How one tag serves both machines

A tag like `ghcr.io/you/devops-lab:latest` does not have to point at one image.
It can point at a **manifest list** (OCI "image index") — a tiny JSON document
that says "for linux/amd64 use digest sha256:aaa…, for linux/arm64 use
sha256:bbb…". When you `docker pull`, the daemon reports its own platform and the
registry hands back the matching digest. That is why `docker pull postgres` just
works on both boxes and why nobody ever thinks about this until they publish
their own image.

Inspect one — a real one and then your own:

```bash
docker buildx imagetools inspect eclipse-temurin:21-jre-jammy
docker buildx imagetools inspect ghcr.io/<you>/devops-lab:latest
```

You want to see `linux/amd64` **and** `linux/arm64` in the list. Also confirm
after a pull on the Pi:

```bash
docker image inspect ghcr.io/<you>/devops-lab:latest --format '{{.Architecture}}'
```

### Why plain `docker build` can't do it

The default builder (the `docker` driver) builds one image for the host platform
and stores it in the local image store, which has no concept of a manifest list.
`buildx` with the `docker-container` driver does — that is all the `create` line
below is doing:

```bash
docker buildx create --name multi --driver docker-container --use
docker buildx inspect --bootstrap        # starts the builder, prints platforms
docker buildx ls                         # which builder is active
```

`inspect --bootstrap` prints something like
`linux/amd64, linux/arm64, linux/arm/v7, …`. The extra platforms come from
**QEMU** user-mode emulation, wired into the kernel via `binfmt_misc`: when the
kernel is handed an aarch64 binary it silently runs it under `qemu-aarch64`.
Docker Desktop ships this. On a bare Linux host, install it once:

```bash
docker run --privileged --rm tonistiigi/binfmt --install all
```

Then build both and push in one command:

```bash
docker buildx build --platform linux/amd64,linux/arm64 \
  -t ghcr.io/<you>/devops-lab:latest --push .
```

**`--push` is not optional here.** `--load` moves the result into the local image
store, which can only hold a single-platform image, so buildx errors out. A
multi-arch build has to land in a registry. (Enabling the containerd image store
in Docker Desktop lifts this, but don't rely on it.)

### Emulation is slow — and you can skip most of it

Under QEMU the arm64 half of the build runs at maybe a quarter speed, and a Maven
build is the worst possible workload for it: minutes of pure CPU. Your build has
a way out, because the jar it produces is architecture-neutral. Pin the build
stage to the *builder's* native architecture and let only the thin runtime stage
be produced per target:

```dockerfile
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
# … mvn package runs once, natively, at full speed …

FROM eclipse-temurin:21-jre-jammy AS runtime
# … this stage is built once per --platform, but it is only a COPY + chown …
```

`BUILDPLATFORM` (the machine doing the building) and `TARGETPLATFORM` /
`TARGETARCH` (what you asked for) are supplied automatically by buildx. For a Go
or Rust project you would instead pass `TARGETARCH` to the compiler and
cross-compile; for Java you get to dodge the whole problem. Knowing *why* the
trick works for Java and not for a Go binary that uses cgo is the part an
interviewer will actually probe.

### In GitHub Actions

`docker/build-push-action` needs QEMU registered on the runner before it can
target a foreign platform, so it is two changes, not one:

```yaml
      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3      # ← add this, before Buildx

      - name: Set up Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          platforms: linux/amd64,linux/arm64   # ← and this
          push: true
          tags: |
            ghcr.io/${{ env.IMAGE_NAME }}:${{ github.sha }}
            ghcr.io/${{ env.IMAGE_NAME }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

Note `${{ github.repository }}` is `owner/repo` — GHCR tags come out as
`ghcr.io/<you>/devops-lab`. New GHCR packages are **private** by default; the Pi
will get `denied` on pull until you either make the package public in the
package settings or `docker login ghcr.io` on the Pi with a PAT that has
`read:packages`.

Once the manifest list exists, nothing on the Pi changes: `docker compose pull`
and `kubectl rollout` fetch the arm64 side automatically, and the same
`docker-compose.yml` and k8s manifests work on both boxes.

### The interview answer

Short version, when someone asks what breaks when you deploy to ARM:

> Container images are architecture-specific because they contain native
> binaries, even when the application on top is portable. A tag can resolve to a
> manifest list with one entry per platform, so the registry serves each host the
> right image. I build those with `docker buildx --platform linux/amd64,linux/arm64
> --push`, with QEMU registered for the foreign target — and because our artefact
> was a jar, I pinned the build stage to `$BUILDPLATFORM` so only the runtime
> layer was emulated, which took the CI job from ~9 minutes back to ~3.

That is a genuine STAR story: symptom (`exec format error` on the Pi), cause,
fix, measured result. Put it in `LEARNINGS.md` the day it bites you, with the
real numbers from your own pipeline.

## Deploy target A — Docker Compose on the Pi (end of week 2)

> _When: **Day 10.** First time anything you built reaches the Pi._

```bash
ssh pi
mkdir -p ~/apps/task-api && cd ~/apps/task-api
# copy docker-compose.yml + .env here
docker compose pull && docker compose up -d
curl -s localhost:8081/api/info
```

## Deploy target B — k3s single-node Kubernetes (week 4)

> _When: **Day 19.** Needs Days 16–18 (local cluster) done first._

k3s is a full, certified Kubernetes in ~100MB — it runs happily on a Pi 5.

```bash
ssh pi
curl -sfL https://get.k3s.io | sh -
sudo systemctl status k3s
sudo k3s kubectl get nodes
```

Copy the kubeconfig back to your laptop so you can drive the Pi cluster remotely:

```bash
# on the Pi
sudo cat /etc/rancher/k3s/k3s.yaml
# on the laptop: save as ~/.kube/pi-config, replace 127.0.0.1 with the Pi's IP
export KUBECONFIG=~/.kube/pi-config
kubectl get nodes
```

That same file is what you upload to Jenkins as the `kubeconfig-file` credential.

**Watch the memory.** Give the Deployment `requests: memory: 384Mi` and drop to
`replicas: 1` on the Pi if Nextcloud is also running. When a pod gets OOMKilled,
`kubectl describe pod` tells you — learning to read that is the point.

## Uninstall cleanly

> _When: **After Day 20**, or any time you want the Pi's RAM back for Nextcloud._

```bash
/usr/local/bin/k3s-uninstall.sh
```
