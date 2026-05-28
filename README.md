# STAShield

STAShield is STA Cloud's private Minecraft Java protection proxy. It is designed as a clean-room project: the repository does not contain `VeloFlame.jar`, does not rename or repackage Velocity/VeloFlame, and does not copy proprietary code.

The initial core is a standalone Java TCP proxy compatible with the Minecraft Java proxy flow:

- Minecraft handshake listener
- Ping/status passthrough to backend
- Login connection tracking
- Per-IP rate limit and burst protection
- Basic bot-score engine
- IP whitelist/blacklist
- Backend forwarding config
- STA Cloud formatted logs
- Basic metrics: connections, blocked, allowed, status, login, backend failures, current CPS, peak CPS

## Java support

Default build/runtime target is Java 25 in CI and Docker. The current core is compiled with Java 8 bytecode compatibility (`source`/`target` 1.8), so it is intended to run on Java 8, 11, 17, 21, and 25.

Future modules such as advanced challenge mode, packet decompression inspection, or newer platform integrations may raise the minimum runtime. If that happens, the core compatibility matrix must be updated here.

## Build jar

```bash
mvn -B test package
```

Output:

```text
target/STAShield.jar
```

Run locally:

```bash
java -jar target/STAShield.jar --config config/stashield.example.toml
```

By default the proxy listens on `0.0.0.0:25565` and forwards to `127.0.0.1:25566`.

## Configuration

Primary config file:

```text
stashield.toml
```

Example config:

```text
config/stashield.example.toml
```

Common settings:

- `[server]`: bind address, listener port, timeout, max packet size, max active connections
- `[backend]`: Minecraft backend host and port
- `[protection]`: rate limit, burst window, bot-score threshold, active login limit
- `[access]`: exact IP and IPv4 CIDR whitelist/blacklist
- `[metrics]`: periodic metrics logging

## Docker

Build the default Java 25 runtime image:

```bash
docker build -t ghcr.io/sta-cloud-dev/prx-mc:latest -t ghcr.io/sta-cloud-dev/prx-mc:v0.1.0 .
```

Run:

```bash
docker run --rm -p 25565:25565 ghcr.io/sta-cloud-dev/prx-mc:latest
```

Runtime image default:

```text
ghcr.io/sta-cloud-dev/deverlopment:java_25
```

Optional runtime variants can be built with:

```bash
docker build --build-arg RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_21 -t ghcr.io/sta-cloud-dev/prx-mc:java_21 .
docker build --build-arg RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_17 -t ghcr.io/sta-cloud-dev/prx-mc:java_17 .
docker build --build-arg RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_11 -t ghcr.io/sta-cloud-dev/prx-mc:java_11 .
docker build --build-arg RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_8 -t ghcr.io/sta-cloud-dev/prx-mc:java_8 .
```

Keep GHCR package visibility private/internal in the GitHub organization settings. Do not publish `ghcr.io/sta-cloud-dev/prx-mc` publicly.

## Pterodactyl egg

Import the ready egg file:

```text
STAShield.json
```

The egg uses `PTDL_v2`, generates `stashield.toml`, exposes backend/protection variables, and defaults to the private/internal GHCR image:

```text
ghcr.io/sta-cloud-dev/prx-mc:latest
```

## GitHub Actions

Workflow:

```text
.github/workflows/build.yml
```

It only runs manually through `workflow_dispatch`. There are no `push` or `pull_request` triggers.

The workflow:

1. Builds `STAShield.jar`
2. Runs tests
3. Builds Docker image
4. Pushes `ghcr.io/sta-cloud-dev/prx-mc:latest`
5. Pushes `ghcr.io/sta-cloud-dev/prx-mc:vX.Y.Z`

## Clean-room policy

STAShield may be inspired by public anti-bot and Minecraft proxy concepts, including the protection goals of projects such as VeloFlame LITE and Velocity-CTD. Implementation must remain independent.

Do not:

- Use `VeloFlame.jar` as a binary base
- Decompile third-party jars and copy code
- Rename Velocity, VeloFlame, or another project as STAShield
- Bundle proprietary code without permission
- Add automatic GitHub Actions triggers
- Make GHCR packages public

If GPL/public projects or public documentation are used for implementation details, add attribution in `NOTICE.md` and comply with their licenses.

## Roadmap

- Advanced anti-bot engine
- Firewall rules
- Packet limiter
- Decompression and packet size protection
- ASN/Geo/IP reputation hooks
- Challenge mode
- Backend health checks
- Hot reload config
- Internal web/API metrics
