# syntax=docker/dockerfile:1.7
#
# GBL-013 US-2 — cccp-ci-base, the portable runtime of the workspace CI.
#
# The image provides the TOOLCHAIN only. It never contains Ollama (binary or
# models), secrets, or a Gradle wrapper: the dedicated `unit-ollama` job keeps its
# own cached image and model (S-278 -> S-280), secrets live in the runner, and the
# wrapper stays versioned per repository (verify-gradle-wrapper).
#
# Two publication channels, same image:
#   public  -> docker.io/cccpeducation/cccp-ci-base   (public boroughs)
#   private -> docker.io/cheroliv/cccp-ci-base        (private units and office)
#
# Consumers pin the published manifest by digest (rule R4), never by tag.
#
# Playwright: the system dependencies are baked; the browser itself is installed
# by the job at its own Playwright version (PLAYWRIGHT_BROWSERS_PATH is a cache
# location). Baking a browser would be wasted bytes for any other version.

FROM eclipse-temurin:25-jdk

LABEL org.opencontainers.image.title="cccp-ci-base" \
      org.opencontainers.image.description="Portable runtime for the CCCP workspace CI (toolchain only)" \
      org.opencontainers.image.source="https://github.com/cccp-education/conventions-gradle" \
      org.opencontainers.image.licenses="Apache-2.0"

ARG DEBIAN_FRONTEND=noninteractive
ARG TARGETARCH=amd64

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    GRADLE_USER_HOME=/root/.gradle \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Base system tools, media binaries and font stack.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        ca-certificates curl git jq unzip xz-utils tar shellcheck \
        ffmpeg espeak-ng \
        fonts-dejavu-core fonts-liberation; \
    rm -rf /var/lib/apt/lists/*

# Node 22 (the NodeSource script is downloaded first so a network failure cannot
# be swallowed by a `curl | bash` pipeline).
RUN set -eux; \
    curl -fsSL https://deb.nodesource.com/setup_22.x -o /tmp/nodesource.sh; \
    bash /tmp/nodesource.sh; \
    apt-get install -y --no-install-recommends nodejs; \
    rm -f /tmp/nodesource.sh; \
    rm -rf /var/lib/apt/lists/*

# Playwright system dependencies for Chromium (browser installed by the job).
RUN set -eux; \
    npm install -g playwright; \
    npx playwright install-deps chromium; \
    rm -rf /var/lib/apt/lists/* /root/.npm

# CI runs as root: the mounted Docker socket (testcontainers) and the Gradle
# cache are then usable without group/GID gymnastics. The container is ephemeral.
RUN mkdir -p /ms-playwright /workspace

WORKDIR /workspace

CMD ["bash"]
