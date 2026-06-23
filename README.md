# WiFi Battle Platform

A high-reusability, high-extensibility Android WiFi LAN multiplayer battle platform framework that supports various game types (chess, shooters, tower defense, MOBA, casual competitive, etc.) through a unified network infrastructure.

## Features

- WiFi LAN multiplayer with auto-discovery (UDP broadcast + NSD/mDNS)
- 2-16 players per room
- Reconnection, heartbeat, ping
- State sync + Lockstep command sync + RPC
- Pluggable GameAdapter for new games
- Material Design 3 dark theme, Compose UI
- MVVM + Clean Architecture + Hilt + Room + Coroutine + Flow
- Docker & GitHub Actions cloud build

## Quick Start

```bash
# Build with Docker
docker-compose up --build

# Or build locally
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Documentation

See `docs/` directory:
- `ARCHITECTURE.md` - Architecture design
- `API.md` - API reference
- `INTEGRATION.md` - Game integration guide
- `DEVELOPMENT.md` - Secondary development guide
- `DEPLOYMENT.md` - Cloud build & deployment
