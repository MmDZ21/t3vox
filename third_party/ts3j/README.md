# Patched ts3j provenance

This repository vendors a locally patched build of [Manevolent/ts3j](https://github.com/Manevolent/ts3j).

## Upstream

- Source: https://github.com/Manevolent/ts3j
- Commit: `db57d60c989e399626aa16d921390f5033e6cdeb`
- Artifact URL: https://jitpack.io/com/github/manevolent/ts3j/db57d60/ts3j-db57d60.jar
- Upstream JAR SHA-256: `50515411094C5872FD8B62BDFB6C14AC6C5F6C5347CC4FD2A086327EFD7EC294`
- License: Apache License 2.0 (see [LICENSE](LICENSE) adjacent to this file)

## Local artifact

- Path: `app/libs/ts3j-db57d60-patched.jar`
- Patched JAR SHA-256: `D360466A61B92A131EF2CEF0CFC81D6DD5035FD23082B7E6D63CF69242C8E004`

## Patch

The only JAR entry removed from upstream is:

`com/github/manevolent/ts3j/protocol/packet/handler/client/LocalClientHandlerConnecting.class`

Its source replacement is:

`app/src/main/java/com/github/manevolent/ts3j/protocol/packet/handler/client/LocalClientHandlerConnecting.java`

The functional change is at lines 109–116: Init1 puzzle solutions shorter than 64 bytes are left-padded into a 64-byte buffer so the client does not crash with `ArrayIndexOutOfBoundsException` on servers that return trivial puzzles.

## Rebuild

```powershell
.\scripts\rebuild-ts3j.ps1
```

The script rejects the build when:

1. The downloaded upstream JAR SHA-256 does not match `50515411094C5872FD8B62BDFB6C14AC6C5F6C5347CC4FD2A086327EFD7EC294`.
2. The patched JAR SHA-256 does not match `D360466A61B92A131EF2CEF0CFC81D6DD5035FD23082B7E6D63CF69242C8E004`.
