# Jenkins setup (week 3)

> **When to read this doc**
> Week 3, one section per day — do not install everything on Day 11 and then
> wonder what Days 12–14 are for.
> **Day 11** — Start it → Plugins → Tools.
> **Day 12** — Create the job, and Exercise 1.
> **Day 13** — Credentials, and Trigger builds automatically.
> **Day 14** — Exercises 2–5.
> Day-by-day map for the whole lab: `00-START-HERE.md`.

Run Jenkins in Docker on the **laptop**, not the Pi — it is a memory hog and you
want it next to your code.

## Start it

> _When: **Day 11.**_

```bash
docker network create jenkins || true

docker run -d --name jenkins \
  -p 8090:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network jenkins \
  --restart unless-stopped \
  jenkins/jenkins:lts-jdk21

docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open <http://localhost:8090>, paste the password, install **suggested plugins**,
create your admin user.

> Mounting `/var/run/docker.sock` lets Jenkins build images using the host's
> Docker daemon. It also means anything running in Jenkins has root on your host.
> Fine for a laptop lab; say "in production I'd use Kaniko or a dedicated build
> agent instead" in an interview and you have just answered a security question.

## Let the Jenkins container run docker + kubectl

> _When: **Day 11**, immediately after the container is up._

```bash
docker exec -u root jenkins bash -c '
  apt-get update &&
  apt-get install -y docker.io &&
  curl -LO "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" &&
  install -m 0755 kubectl /usr/local/bin/kubectl &&
  usermod -aG docker jenkins'
docker restart jenkins
```

## Plugins

> _When: **Day 11.**_

Manage Jenkins → Plugins → Available. Add on top of the suggested set:

- **Docker Pipeline** (gives you the `docker.build` / `docker.withRegistry` steps)
- **Pipeline: Stage View**
- **Blue Ocean** (optional, nicer pipeline visualisation)

## Tools

> _When: **Day 11.** The names here must match the `Jenkinsfile` you write on Day 12._

Manage Jenkins → **Tools**:

- JDK → *Add JDK* → name `jdk21`, untick "Install automatically", JAVA_HOME
  `/opt/java/openjdk` (that is where the `lts-jdk21` image puts it)
- Maven → *Add Maven* → name `maven3`, tick "Install automatically", latest 3.9.x

These names must match the `tools { }` block in the `Jenkinsfile`.

## Credentials

> _When: **Day 13.** The Day 12 pipeline builds without them; pushing an image needs them._

Manage Jenkins → **Credentials** → System → Global:

| ID                | Kind              | Contents                                   |
|-------------------|-------------------|--------------------------------------------|
| `dockerhub-creds` | Username/password | Docker Hub username + **access token**     |
| `kubeconfig-file` | Secret file       | your k3s/k3d kubeconfig                    |
| `github-token`    | Username/password | GitHub username + PAT (for private repos)  |

Never put a password in the `Jenkinsfile`. The whole point of this step is
learning that credentials live in the platform, not in the repo.

## Create the job

> _When: **Day 12**, once the `Jenkinsfile` exists._

New Item → **Multibranch Pipeline** → name `task-api`
→ Branch Sources → GitHub → your repo → Scan.

Jenkins finds the `Jenkinsfile`, discovers `main` and every `feature/**` branch,
and builds each one. Multibranch is what real teams use — go straight to it
rather than a plain Pipeline job.

## Trigger builds automatically

> _When: **Day 13.**_

Locally, GitHub cannot reach your laptop, so either:

- **Poll:** job → Configure → Scan Repository Triggers → every 2 minutes. Crude but
  works offline.
- **Webhook via ngrok:** `ngrok http 8090`, then add the public URL +
  `/github-webhook/` to your repo's GitHub webhook settings. This is the real
  mechanism — worth doing once so you can describe it.

## Exercises

> _When: **Day 12** for #1; **Day 14** for #2–#5._

1. Break a test on purpose, push, watch the pipeline go red, read the JUnit report.
2. Add a `Quality Gate` stage that fails the build if JaCoCo coverage < 50%.
3. Add `input message: 'Deploy to prod?'` before the deploy stage — manual approval gates.
4. Make the deploy stage run only on `main` (already done via `when { branch 'main' }`)
   and prove it by pushing to a feature branch.
5. Add a `post { failure { ... } }` notification — email or a Slack webhook.
