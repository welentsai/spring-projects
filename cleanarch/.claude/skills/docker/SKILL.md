---
name: docker
description: Run and manage Docker containers, images, volumes, and networks. Use when the user asks to start, stop, inspect, build, or troubleshoot Docker resources.
disable-model-invocation: true
allowed-tools: Bash(docker *)
argument-hint: [ps|build|logs|exec|stop|rm|...]
---

# Docker Management Skill

You are a Docker expert. Use the `docker` CLI to inspect and manage containers, images, volumes, and networks.

## Common Tasks

### Inspect running containers
```bash
docker ps
docker ps -a   # include stopped containers
```

### View logs
```bash
docker logs <container>
docker logs -f <container>   # follow/tail
```

### Execute commands inside a container
```bash
docker exec -it <container> sh
docker exec <container> <command>
```

### Build an image
```bash
docker build -t <name>:<tag> .
docker build -f <Dockerfile> -t <name>:<tag> .
```

### Start / stop containers
```bash
docker start <container>
docker stop <container>
docker restart <container>
```

### Remove containers / images
```bash
docker rm <container>
docker rmi <image>
docker system prune   # clean up all unused resources
```

### Inspect details
```bash
docker inspect <container|image>
docker stats           # live resource usage
docker port <container>
```

### Networks & volumes
```bash
docker network ls
docker volume ls
docker volume inspect <volume>
```

## Guidelines

1. Always run `docker ps` first to confirm the target container is running before acting on it.
2. Prefer non-destructive commands (inspect, logs) before destructive ones (rm, prune).
3. When stopping or removing a container, confirm the container name/ID with the user if not explicitly provided.
4. For `docker exec`, default to `sh` if the image doesn't include `bash`.
5. Surface useful information from `docker inspect` output — IP address, mounts, env vars, health status.
6. If `$ARGUMENTS` is provided, interpret it as the subcommand or target to act on.
