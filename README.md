# surf-odyssey

## Development stack

The development stack runs the whole network locally in Docker:

| Service             | What it is                                                                                 |
|---------------------|--------------------------------------------------------------------------------------------|
| `proxy`             | Velocity proxy, the only server players connect to                                         |
| `hub-lobby-01` to `hub-lobby-03` | Paper servers of the lobby hub world                                          |
| `hub-north-01`, `hub-north-02` | Paper servers of the north hub world                                            |
| `hub-south-01`      | Paper server of the south hub world                                                        |
| `home`              | Paper server of the player homes                                                           |
| `pack-01`           | Paper server that builds the CraftEngine resource pack and uploads it to SeaweedFS         |
| `seaweedfs`, `seaweedfs-admin` | S3 storage the resource pack is downloaded from, and its admin UI               |
| `odyssey`           | This repository's microservice, built from its Dockerfile                                  |
| `core`              | surf-core microservice, pulled from `ghcr.io/slne-development/surf-core-microservice`      |
| `polar-paper`       | surf-polar-paper microservice, pulled from `ghcr.io/slne-development/surf-polar-paper-microservice` |
| `postgres`          | One database (`surf`) for everything, with a schema per microservice and for LuckPerms     |
| `rabbitmq`, `valkey` | Messaging between the plugins and the microservices                                      |

### Requirements

- Java 25
- Docker with Docker Compose. The ghcr images are public, no login needed.
- Free ports 25565, 5432, 5672, 6379 and 15672. Stop other development stacks first, or change the ports in `.env`.

### First start

1. **Put the server plugins into the run folders.** The stack deploys the odyssey plugins itself, everything else has to
   be put into the `plugins` folders below once. The folders are gitignored.

   | Server         | Plugins folder                                                                                                     |
   |----------------|--------------------------------------------------------------------------------------------------------------------|
   | `proxy`        | `surf-odyssey-runtime/surf-odyssey-runtime-velocity/run/plugins`                                                   |
   | `hub-lobby-01` | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-lobby/run/hub-lobby-01/plugins` |
   | `hub-north-01` | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-north/run/hub-north-01/plugins` |
   | `hub-north-02` | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-north/run/hub-north-02/plugins` |
   | `hub-south-01` | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/surf-odyssey-runtime-paper-hub-south/run/hub-south-01/plugins` |
   | `home`         | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run/plugins`                      |
   | `pack-01`      | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-pack/run/pack-01/plugins`              |

   The hub column stands for each of the four `hub-*` servers.

   | Plugin                         | proxy | hub | home | pack-01 | Download                                                                      |
   |--------------------------------|:-----:|:---:|:----:|:-------:|-------------------------------------------------------------------------------|
   | surf-api                       |   x   |  x  |  x   |    x    | `surf-api-velocity-server` / `surf-api-paper-server` from surf-api            |
   | surf-rabbitmq                  |   x   |  x  |  x   |    x    | `surf-rabbitmq-velocity` / `surf-rabbitmq-paper` from surf-rabbitmq            |
   | surf-redis                     |   x   |  x  |  x   |    x    | [releases](https://github.com/SLNE-Development/surf-redis/releases)           |
   | surf-core                      |   x   |  x  |  x   |    x    | [releases](https://github.com/SLNE-Development/surf-core/releases)            |
   | CommandAPI                     |   x   |  x  |  x   |    x    | [releases](https://github.com/CommandAPI/CommandAPI/releases), Velocity or Paper jar |
   | LuckPerms                      |   x   |  x  |  x   |    x    | [luckperms.net](https://luckperms.net/download), Velocity or Bukkit jar        |
   | packetevents                   |   x   |  x  |  x   |    x    | [modrinth](https://modrinth.com/plugin/packetevents), Velocity or Spigot jar   |
   | HuskSync                       |       |  x  |  x   |         | [modrinth](https://modrinth.com/plugin/husksync)                              |
   | CraftEngine                    |       |  x  |  x   |    x    | `craft-engine-bukkit-plugin` jar                                              |
   | MiniPlaceholders               |       |  x  |      |         | [modrinth](https://modrinth.com/plugin/miniplaceholders)                      |
   | polarpaper                     |       |     |  x   |         | [releases](https://github.com/MinehubMC/polarpaper/releases)                  |
   | surf-polar-paper-runtime-paper |       |     |  x   |         | [releases](https://github.com/SLNE-Development/surf-polar-paper/releases)     |

2. **Start the stack.**

   ```shell
   ./gradlew startDevelopmentStack
   ```

   This builds the plugins and copies them into the run folders, checks that every plugin above is there (and names the
   missing ones), builds the odyssey microservice image, pulls the other microservices and starts everything. The
   first start takes a few minutes; Paper and Velocity are downloaded into the run folders.

3. **Join** `localhost:25565`. You land on the lobby (`hub-lobby-01`).

4. **Give yourself permissions.** LuckPerms of the proxy and every Paper server share their storage, so one command from
   any console applies everywhere:

   ```shell
   docker compose exec -u 1000 hub-lobby-01 mc-send-to-console lp user <name> permission set '*' true
   ```

### Everyday use

```shell
./gradlew startDevelopmentStack    # (re)build and start everything, also picks up changes to compose.yaml
./gradlew deployDevelopmentPlugin  # rebuild the odyssey plugins and restart the running servers with them
./gradlew stopDevelopmentStack     # stop the stack, data is kept
./gradlew cleanDevelopmentStack    # stop the stack and delete the database, RabbitMQ and Valkey data
```

Consoles are attached with `docker compose attach proxy` (or `hub-lobby-01` to `hub-lobby-03`, `hub-north-01`, `hub-north-02`,
`hub-south-01`, `home`, `pack-01`, `odyssey`, `core`, `polar-paper`) and
detached with Ctrl+P, Ctrl+Q. Logs: `docker compose logs -f <service>`. For the microservices, run these through the
Gradle tasks or set `COMPOSE_FILE` as described below, since they live in a generated compose file.

| Service     | Address                |
|-------------|------------------------|
| Proxy       | `localhost:25565`      |
| RabbitMQ UI | http://localhost:15672 |
| PostgreSQL  | `localhost:5432`       |
| Valkey      | `localhost:6379`       |

Users and passwords are `surf` / `surf`. Copy `.env.example` to `.env` to change ports, credentials, memory and the
Velocity forwarding secret.

### How it is put together

- `compose.yaml` holds the infrastructure, the proxy and the servers. The servers run from their `run` folders.
- `build.gradle.kts` configures the `developmentStack` extension of `build-logic`:
    - `server(...)` names a compose service, the odyssey plugin project deployed to it and the plugins it needs.
    - `microservice(...)` adds a microservice. It either runs a published `image`, pulled on every start, or is built
      from a `dockerfile` of a local Gradle build. `schema` gives it a schema in the shared database, `redis = true`
      connects it to Valkey. The microservices are written to `build/development-stack/microservices.compose.yaml`,
      which every Gradle task passes to compose next to `compose.yaml`. To use plain `docker compose` with them, set
      `COMPOSE_FILE=compose.yaml:build/development-stack/microservices.compose.yaml` (`;` instead of `:` on Windows).
    - `schemas` lists further schemas to create, e.g. `luckperms`.
- `docker/` holds config files synced into the run folders on every start, with `${CFG_...}` placeholders filled from
  the container environment:
    - `docker/velocity`: `velocity.toml`, the forwarding secret and the surf-core config of the proxy
    - `docker/paper/config`: `paper-global.yml` of the Paper servers, enabling Velocity forwarding
    - `docker/paper/plugins`: plugin configs of every Paper server, e.g. the surf-core server name per server and the
      CraftEngine resources
    - `docker/paper/hub/plugins`: the CraftEngine config of the hub servers and home, which only sends the pack
    - `docker/paper/pack/plugins`: the CraftEngine config of `pack-01`, which builds and uploads the pack
    - `docker/paper/home/plugins`: plugin configs of home only, e.g. surf-polar-paper with DATABASE storage
- `docker/worlds` holds the hub worlds; every hub server copies its world from there on each start.

  Change these files instead of the copies in the run folders; a copy is only replaced when its source is newer.
- LuckPerms is configured through the `LUCKPERMS_*` variables in `compose.yaml`.
