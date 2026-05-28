# HELP_INTERNAL

Internal runbook for STA Cloud operators.

## Goal

Allow a VPS running Pterodactyl Wings to pull the internal/private GHCR package:

```text
ghcr.io/sta-cloud-dev/prx-mc:latest
```

This image is used by the `STAShield.json` egg.

## Pterodactyl port model

STAShield runs as its own Pterodactyl server.

There are two different ports:

- Proxy port: the primary allocation of the STAShield server. Players connect to this port.
- Backend port: the allocation of the real Minecraft server that STAShield protects.

Example:

```text
STAShield server allocation: 30001
Minecraft backend allocation: 30002
```

Players connect to:

```text
node.example.com:30001
```

STAShield forwards allowed traffic to:

```text
BACKEND_HOST:30002
```

In the egg, `bind_port` is set automatically from Pterodactyl's primary allocation for the STAShield server. Do not manually set it to `25565`.

Set `Backend Port` to the allocation port of the Minecraft backend server for that user/server. This value is different per Pterodactyl allocation.

`Backend Host` defaults to:

```text
AUTO
```

At startup, `AUTO` is resolved to Pterodactyl's `SERVER_IP`, which is the primary allocation IP for the STAShield server. For same-node backend servers, this should normally be the node/allocation IP. If the backend Minecraft server is on another node, change `Backend Host` to that backend node's reachable public/private IP.

Avoid `127.0.0.1` for `Backend Host` when the backend is another Pterodactyl server/container. Inside Docker, `127.0.0.1` means the STAShield container itself.

## Required GitHub token

Use a GitHub personal access token classic from an account or bot that is a member of the `STA-Cloud-Dev` organization.

Minimum scope:

```text
read:packages
```

If the organization requires SSO or token approval, authorize the token for `STA-Cloud-Dev` before testing on the VPS.

Do not put this token in:

- Egg variables
- Startup commands
- Server environment variables
- Public docs
- Git commits

The token should only live in the Wings node config or a secured secret manager.

## Test GHCR pull on the VPS

SSH into the VPS that runs Wings, then run:

```bash
echo 'GH_PAT_CLASSIC' | docker login ghcr.io -u GITHUB_USERNAME --password-stdin
docker pull ghcr.io/sta-cloud-dev/prx-mc:latest
```

Replace:

- `GH_PAT_CLASSIC` with the GitHub PAT classic
- `GITHUB_USERNAME` with the GitHub username that owns the token

Expected result:

```text
Status: Downloaded newer image for ghcr.io/sta-cloud-dev/prx-mc:latest
```

## Configure Wings registry credentials

Edit Wings config:

```bash
nano /etc/pterodactyl/config.yml
```

Find the existing `docker:` block. Add `registries:` under it.

Example:

```yaml
docker:
  registries:
    ghcr.io:
      username: "GITHUB_USERNAME"
      password: "GH_PAT_CLASSIC"
```

If the `docker:` block already has other settings, merge `registries:` into the existing block. Do not create a second `docker:` block.

Restart Wings:

```bash
systemctl restart wings
systemctl status wings --no-pager
```

Then create or restart a STAShield server from the panel. Wings should pull:

```text
ghcr.io/sta-cloud-dev/prx-mc:latest
```

## Package visibility checklist

In GitHub:

1. Go to `STA-Cloud-Dev` organization packages.
2. Open package `prx-mc`.
3. Confirm package visibility is `Internal` or `Private`, not `Public`.
4. Confirm the account used by the PAT has access to the organization/package.

Internal visibility allows organization members to access the package, but the VPS still needs authenticated GHCR credentials.

## Common errors

### denied: denied

Usually means the VPS is not authenticated or the token cannot read the package.

Check:

- Token has `read:packages`
- Token owner is in `STA-Cloud-Dev`
- Token is authorized for org SSO if required
- Package is actually `ghcr.io/sta-cloud-dev/prx-mc`
- Wings config uses registry key `ghcr.io`

### unauthorized: authentication required

Docker/Wings did not send valid credentials.

Run:

```bash
docker logout ghcr.io
echo 'GH_PAT_CLASSIC' | docker login ghcr.io -u GITHUB_USERNAME --password-stdin
docker pull ghcr.io/sta-cloud-dev/prx-mc:latest
```

If manual pull works but Wings fails, re-check `/etc/pterodactyl/config.yml` and restart Wings.

### manifest unknown

The tag does not exist.

Check the pushed tags:

```text
latest
v0.1.0
java_21
java_17
java_11
java_8
```

For release builds, prefer:

```text
ghcr.io/sta-cloud-dev/prx-mc:v0.1.0
```

### pull access denied for ghcr.io/sta-cloud-dev/prx-mc

This can happen when the package is private/internal and the token account cannot access the org package.

Fix by using a bot/service account that is a member of `STA-Cloud-Dev`, or grant package access to the needed team/account.

## Security notes

- Prefer a dedicated GitHub bot/service account for Wings pulls.
- Give the token only `read:packages`.
- Rotate the token periodically.
- Remove old tokens after staff changes.
- Do not paste tokens into Discord, panel notes, eggs, README, or commits.

## Quick copy block

```bash
echo 'GH_PAT_CLASSIC' | docker login ghcr.io -u GITHUB_USERNAME --password-stdin
docker pull ghcr.io/sta-cloud-dev/prx-mc:latest
nano /etc/pterodactyl/config.yml
systemctl restart wings
systemctl status wings --no-pager
```
