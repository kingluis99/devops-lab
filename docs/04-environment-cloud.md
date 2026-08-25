# Environment 3 — cloud free tier (week 4, optional but interview-valuable)

> **When to read this doc**
> **Day 19**, and optional — skip the whole doc if the Pi cluster took the day.
> Read "Cost safety" *before* you create a single resource, not after.
> The Terraform section is post-Day-20 territory.
> Day-by-day map for the whole lab: `00-START-HERE.md`.

The Pi teaches you Linux and Kubernetes. The cloud teaches you the vocabulary UK
job adverts are written in: VPC, security group, IAM, load balancer, managed
service.

## Which free tier

> _When: **Day 19.** Decide before you start; switching later costs an evening._

| Provider | What you get free | Verdict for this course |
|---|---|---|
| **Oracle Cloud Always Free** | 4 ARM cores + 24GB RAM, **forever** | Best value by far. Big enough for a real 2-node k3s cluster. ARM64 — same architecture as your Pi, so your multi-arch images carry over. |
| **AWS Free Tier** | 750 hrs/month t2.micro, 12 months | Weakest hardware, strongest CV signal. Use it to learn EC2 + VPC + IAM vocabulary. EKS is **not** free (~$0.10/hr for the control plane). |
| **GCP** | $300 credit, 90 days + e2-micro always-free | GKE Autopilot is the gentlest managed Kubernetes. Good for one week of "what does managed k8s feel like". |

Recommended: **Oracle Always Free** for the cluster, **AWS free tier** for one
week of EC2/VPC/IAM literacy.

## Cost safety — do this before you launch anything

> _When: **Day 19, first.** Before any resource exists. This is the section people skip and regret._

1. Set a **billing alarm at £1** (AWS: Billing → Budgets; GCP: Billing → Budgets & alerts).
2. Write down every resource you create in a `TEARDOWN.md` in this repo.
3. Managed Kubernetes control planes (EKS, AKS) and idle **load balancers / elastic
   IPs** are the classic surprise charges. NAT gateways too.
4. End of each session: `terraform destroy`, or manually delete and re-check the
   console the next morning.

## Minimum useful lab

> _When: **Day 19.** Two hours, not a whole day — the Pi cluster is the priority._

```bash
# 1. Create one small VM (Ubuntu 22.04/24.04), open ports 22 and 80 only
# 2. Harden it exactly like the Pi: key-only SSH, no root login, ufw
ssh -i ~/.ssh/cloud_key ubuntu@<public-ip>
sudo apt update && sudo apt install -y docker.io
sudo usermod -aG docker $USER   # log out and back in

# 3. Pull the image your pipeline pushed and run it
docker run -d -p 80:8080 ghcr.io/<you>/devops-lab:latest
curl http://<public-ip>/api/info

# 4. Then the same thing on k3s
curl -sfL https://get.k3s.io | sh -
kubectl apply -f k8s/
```

Understand *why* step 3 works from your browser and step 2 does not work on port
8080: that is the security group / firewall rule, and it is the single most
common "why can't I reach my app" cause in real jobs.

## Bonus: Terraform (only if week 4 goes smoothly)

> _When: **After Day 20.** Next-chapter material, not part of the 20 days._

Infrastructure as Code is the natural next chapter after this course. A 30-line
`main.tf` that creates one VM, one security group and one key pair, then
`terraform destroy`, is enough to put "Terraform (foundational)" on a CV honestly.

```hcl
resource "aws_instance" "lab" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t2.micro"
  key_name      = aws_key_pair.lab.key_name
  vpc_security_group_ids = [aws_security_group.lab.id]
  tags = { Name = "devops-lab" }
}
```
