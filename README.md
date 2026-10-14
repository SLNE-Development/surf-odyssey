# surf-odyssey

## Development

Requires Java 25 and Docker.

```shell
./gradlew build                    # build and run all tests
./gradlew startDevelopmentStack    # start the infrastructure, the microservices, the proxy and the hub and home servers
./gradlew deployDevelopmentPlugin  # rebuild the plugins and restart the running servers with them
./gradlew stopDevelopmentStack     # stop the stack
./gradlew cleanDevelopmentStack    # stop the stack and delete its volumes
```

The stack is defined by `compose.yaml` and the `developmentStack` block in `build.gradle.kts`:

- `server(...)` entries name a compose service of `compose.yaml` and the plugin project deployed into its `run/plugins`
  folder. The other plugins a server needs have to be put there manually; `startDevelopmentStack` names any that are
  missing.
- `microservice(...)` entries are added to the stack through a generated compose file. A microservice either runs a
  published `image`, pulled on every start, or is built from a `dockerfile` of a local Gradle build, like this
  repository's own microservice. All microservices share one PostgreSQL database, each with its own `schema`, and
  `redis = true` connects one to Valkey.

| Service      | Folder                                                                     | Plugins                                                                          |
|--------------|----------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `proxy`      | `surf-odyssey-runtime/surf-odyssey-runtime-velocity/run`                   | surf-api-velocity, surf-rabbitmq-velocity, surf-redis-velocity, surf-core-velocity, CommandAPI, LuckPerms, packetevents |
| `hub`        | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-hub/run`  | surf-api-paper-server, surf-rabbitmq-paper, surf-redis-paper, surf-core-paper, CommandAPI, LuckPerms, packetevents |
| `home`       | `surf-odyssey-runtime/surf-odyssey-runtime-paper/surf-odyssey-runtime-paper-home/run` | as `hub`                                                                         |

LuckPerms on the proxy and both servers is configured through `LUCKPERMS_*` variables in `compose.yaml` and shares
its storage in the `luckperms` schema, so permissions set on one apply everywhere, e.g. from a server console:
`docker compose exec -u 1000 hub mc-send-to-console lp user <name> permission set '*' true`.

Players join through the proxy, which forwards them to the servers with Velocity modern forwarding. `docker/velocity`,
`docker/paper/config` and `docker/paper/plugins` (e.g. the surf-core server config) are synced into the proxy and
server folders on every start. Copy `.env.example` to `.env`
to change ports, credentials and the forwarding secret.

| Service     | Address                |
|-------------|------------------------|
| Proxy       | `localhost:25565`      |
| RabbitMQ UI | http://localhost:15672 |
| PostgreSQL  | `localhost:5432`       |
| Valkey      | `localhost:6379`       |

Consoles can be attached with `docker compose attach proxy` (or `hub`, `home`), detach with Ctrl+P, Ctrl+Q.
