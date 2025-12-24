# Summary

## Introduction

This project is a Redis server implementation in **Java**, built from scratch as part of the [Codecrafters Redis Challenge](https://codecrafters.io/). It follows the **client–server architecture** and can accept requests from the Redis CLI over TCP sockets. Communication between client and server is handled using the **RESP protocol**.

---

## Repo Overview

- `src/` → Core Java implementation of the Redis server (for subsequent modules, prefix = `main/java`)
  - `/comparator` → Comparators for custom sorting logic
  - `/constants` → Public constants shared across the repository
  - `/domain` → DTO shared across the repository
  - `/enums` → Enums shared across the repository
  - `/handler` → List of command handlers that follow `register-handler` design patterns
    - `/command`
      - `/impl` → List of command-handler implementations
        - `/auth` → AuthHandler, AclHandler
        - `/core` → ConfigHandler, EchoHandler, GetHandler, IncrHandler, InfoHandler, KeysHandler, PingHandler, SetHandler, TypeHandler
        - `/geospatial` → GeoAddHandler, GeoDistHandler, GeoPosHandler, GeoSearchHandler
        - `/list` → BLPopHandler, LLenHandler, LPopHandler, LPushHandler, LRangeHandler, RPushHandler
        - `/pubsub` → PublishHandler, SubscribeHandler, UnsubscribeHandler
        - `/replication` → PsyncHandler, ReplConfigHandler, SaveHandler, WaitHandler
        - `/sortedset` → ZAddHandler, ZCardHandler, ZRangeHandler, ZRankHandler, ZRemHandler, ZScoreHandler
        - `/stream` → XAddHandler, XRangeHandler, XReadHandler
        - `/transaction` → DiscardHandler, ExecHandler, MultiHandler
      - `CommandHandler` interface → follow `register-handler` design patterns
    - `/job`
      - `/impl` → List of job implementations to handle busy-loading job queues
        - HandshakeHandler
        - PropagateHandler
        - RespHandler
      - `JobHandler` interface → follow steps: register job → listen task → process task
  - `/replication` → Follow these logics
    - MasterManager
    - ReplicaClient
  - `/service` → Several util classes
    - GeoUtils, HashUtils, RandomUtils, RDBLoaderUtils, RDBParser, RDBParserUtils, RedisLocalMap, RESPParser, RESPParserUtils, RESPUtils, ServerUtils, StreamUtils, StringUtils, SystemPropHelper
  - `/stream` → `RedisInputStream` that wraps around `FilterInputStream`
- `tests/` → Validation against Redis client commands

---

## Architecture

1. **Command:** follow register-handler design patterns
2. **Replication architecture**
   - Replication configuration
   - Replication handshake

---

## Commands

1. Since your program acts as a Redis server, we can start with command

   ```bash
   $ ./your_program.sh
   ```

2. As Redis follows client-server architecture, there are **2 ways** to send requests to the Redis server

   a. Per session: start a redis-client session to send CMD requests to the Redis server

   ```bash
   $ redis-cli
   ```

   b. Per request (**2 ways**):
      - echo the CMD requests to `redis-cli` - e.g. `$ echo -e "PING" | redis-cli`
      - write command directly - e.g. `$ redis-cli -p SET foo bar PX 100`

3. Command examples

   ```bash
   # send 1 command PING in 1 request
   $ echo -e "PING" | redis-cli
   PONG

   # send multiple commands PING in 1 request
   $ echo -e "PING\nPING" | redis-cli
   PONG
   PONG

   # send 1 command PING in 1 request and concurrently
   $ echo -e "PING" | redis-cli; echo -e "PING" | redis-cli
   PONG
   PONG

   # send 1 command ECHO in 1 request
   $ echo -e "ECHO hey" | redis-cli

   # send 1 command SET in 1 request
   # then send 1 command GET in another request
   $ echo -e "SET foo bar" | redis-cli
   OK
   $ echo -e "GET foo" | redis-cli
   bar
   $ echo -e "GET hello" | redis-cli
   (nil)

   # send 1 command SET in 1 request with expiry
   # then immediately send 1 command GET in another request
   $ echo -e "SET foo bar PX 100" | redis-cli; echo -e "GET foo" | redis-cli
   OK
   "bar"

   # send 1 command SET in 1 request with expiry
   # then sleep for 0.2 sec then send 1 command GET in another request
   $ echo -e "SET foo bar PX 100" | redis-cli; sleep 0.2; echo -e "GET foo" | redis-cli
   OK
   (nil)

   # send 1 command INFO in 1 request to a Master node about its replication setup
   $ echo -e "info replication" | redis-cli
   role:master
   master_replid:qk7ah4jae1nyyyq979mbsgbta09rierunnq74158
   master_repl_offset:0%

   # set up 1 replica node and 1 master node
   # send 1 command INFO in 1 request to a Replica node about its replication setup
   $ ./your_program.sh --port 6379 # master node
   $ ./your_program.sh --port 6380 --replicaof "localhost 6379"
   $ redis-cli -p 6380 info replication
   role:slave
   master_replid:sou6zwzlo3ixngivzijxwb023ju8ratd53xy5heg
   master_repl_offset:0

   # set up 1 replica node and 1 master node
   # send 1 command SET in 1 request to a Master node
   # then send 1 command GET in 1 request to a Replica node
   $ ./your_program.sh --port 6379 # master node
   $ ./your_program.sh --port 6380 --replicaof "localhost 6379"
   $ redis-cli -p 6379 SET foo bar
   OK
   $ redis-cli -p 6380 GET foo
   "bar"

   # set up 2 replicas node and 1 master node
   # send 1 command SET in 1 request to a Master node
   # then send 1 command GET in 1 request to 1st Replica node
   # then send 1 command GET in 1 request to 2nd Replica node
   $ ./your_program.sh --port 6379 # master node
   $ ./your_program.sh --port 6380 --replicaof "localhost 6379"
   $ ./your_program.sh --port 6381 --replicaof "localhost 6379"
   $ redis-cli -p 6379 SET foo bar
   OK
   $ redis-cli -p 6380 GET foo
   "bar"
   $ redis-cli -p 6381 GET foo
   "bar"

   # set up 2 replicas node and 1 master node
   # send WAIT command in 1 request to a Master node
   $ ./your_program.sh --port 6379 # master node
   $ ./your_program.sh --port 6380 --replicaof "localhost 6379"
   $ ./your_program.sh --port 6381 --replicaof "localhost 6379"
   $ redis-cli WAIT 3 2000 # wait up to 2s then return the total number of replicas
   (integer) 2

   # set up 2 replicas node and 1 master node
   # send SET command then WAIT 2nd command in 1 request to a Master node
   $ ./your_program.sh --port 6379 # master node
   $ ./your_program.sh --port 6380 --replicaof "localhost 6379"
   $ ./your_program.sh --port 6381 --replicaof "localhost 6379"
   $ redis-cli SET foo 123; redis-cli WAIT 1 500
   # must wait until either 1 replica has processed previous commands or 500ms have passed

   $ redis-cli SET bar 456; redis-cli WAIT 2 500
   # must wait until either 1 replica has processed previous commands or 500ms have passed

   # send 1 command SET in 1 request then send TYPE in another request
   $ redis-cli SET some_key "foo"
   OK
   $ redis-cli TYPE some_key
   "string"

   # send 1 command XADD in 1 request then send TYPE in another request
   $ redis-cli XADD stream_key 0-1 foo bar
   "0-1"
   $ redis-cli TYPE stream_key
   "stream"

   # send a sequence of commands: SET, INCR, INCR for number type
   $ redis-cli SET foo 5
   "OK"
   $ redis-cli INCR foo
   (integer) 6
   $ redis-cli INCR foo
   (integer) 7

   # send a sequence of commands: SET, INCR, INCR for non-number type
   $ redis-cli SET foo bar
   "OK"
   $ redis-cli INCR foo
   (error) ERR value is not an integer or out of range

   # send a sequence of commands within 1 redis-session: MULTI, QUEUED, EXEC
   $ redis-cli
   > MULTI
   OK
   > SET foo 41
   QUEUED
   > INCR foo
   QUEUED
   > EXEC
   1) OK
   2) (integer) 42

   # send a sequence of commands within 1 redis-session: MULTI, EXEC
   $ redis-cli
   > MULTI
   OK
   > EXEC
   (empty array)

   # send a sequence of commands within 1 redis-session: MULTI, SET, DISCARD
   $ redis-cli
   > MULTI
   OK
   > SET foo 41
   QUEUED
   > DISCARD
   OK
   > DISCARD
   (error) ERR DISCARD without MULTI

   # send a sequence of commands within 1 redis-session: MULTI, SET, INCR, SET, EXEC
   # failures within transactions
   $ redis-cli
   > MULTI
   OK
   > SET foo xyz
   QUEUED
   > INCR foo
   QUEUED
   > SET bar 7
   > EXEC
   1) OK
   2) (error) ERR value is not an integer or out of range
   3) OK
   ```

---

## Testing

1. Unit Tests
   - Command to run tests is `$ mvn test -Dtest="handler.command.impl.**" -q 2>&1`
   - Expected result is

     ```
     [INFO]
     [INFO] Results:
     [INFO]
     [INFO] Tests run: 284, Failures: 0, Errors: 0, Skipped: 0
     [INFO]
     ```

2. Stress Test

3. Analysis

---

## RESP Protocol

The Redis Serialization Protocol (**RESP**) is the core communication protocol between Redis clients and servers.

References:
- [Redis Protocol Specification](https://redis.io/docs/latest/develop/reference/protocol-spec/)
- [Lethain's Redis Protocol Guide](https://lethain.com/redis-protocol/)

**Key properties of RESP:**
- `Binary-safe`: Works with arbitrary byte sequences.
- `Length-prefixed`: Encodes data with size information for efficient parsing.
- `Simple & human-readable`: Easy for developers to debug.
- `Fast to parse`: Optimized for high-performance servers.

---

## Replication 101

Redis replication is the foundation for high availability and failover.

1. A **Master node** handles both read and write requests, while **Replica nodes** serve read-only requests.

2. When clients send writes to the Master, it propagates the changes to all connected Replicas as a stream of bytes.

There are two main aspects of replication:

1. **Full sync vs. Partial sync:** - After a disconnection or crash, a Replica may request a *partial sync* if its local state is still within the Master's backlog buffer. - If not possible, the Replica requests a *full sync*, where the Master generates an RDB snapshot and transfers it as a byte stream.

2. **Asynchronous vs. Synchronous syncing:** - In *asynchronous* replication, the Master sends updates in the background without waiting for Replica acknowledgments. - In *synchronous* replication, the Master waits for acknowledgments from *N* Replicas before confirming a write.

---

## Replication Mechanism

Redis replication relies on a well-defined ID and offset system to track dataset versions and synchronize efficiently.

- **Replication ID and Offset:** Each Master maintains a unique `replication ID` (40-character string) and an `offset`. Every client write increases the Master's offset, which is used to determine how Replicas catch up.

- **PSYNC Command:** Replicas use `PSYNC` to request synchronization. If their offset falls within the Master's backlog buffer, the Master sends only the missing byte stream. Otherwise, a full sync is performed.

- **Replication Steps (Master → Replica):**
  1. The Master spawns a background process to create an RDB snapshot, while buffering client commands in parallel.
  2. The snapshot (RDB file) is transferred to the Replica and loaded into memory.
  3. The buffered command stream is sent over (using RESP protocol) to bring the Replica fully up to date.

- **Efficiency:** If multiple Replicas reconnect at once, the Master reuses a single background save to serve them all, reducing overhead.

- **Auto-reconnect:** Replicas can automatically `reconnect` and `resynchronize` when the Master–Replica link is interrupted.

---

## Redis Use Cases

Redis is widely used in distributed systems due to its simplicity and speed. Some common patterns:

1. **Distributed Locks:** Implemented with atomic commands like `INCR` and `DEL`, often wrapped in Lua scripts for transactional guarantees.

2. **Pub/Sub (Lightweight Event Streaming):** Similar in spirit to Kafka streams. Events are appended to logs and consumed by **consumer groups**, which distribute messages across workers in parallel.

3. **Rate Limiting:** Sliding window algorithms using atomic commands (`INCR`, `EXPIRE`) can enforce request limits efficiently.

4. **Caching:** Fast in-memory key–value store for reducing database load.

5. **Real-time Dashboards:** Using sorted sets (`ZSET`) for leaderboards, ranking systems, or priority queues.

---

## Common Challenges in Redis

Like any distributed system, Redis comes with design tradeoffs and operational challenges:

### 1. Consistent Hashing

- Redis keys are mapped to **hash slots**, which are distributed across nodes.
- If a node fails, consistent hashing allows reassigning only a small portion of slots to other healthy nodes instead of recomputing all keys.
- This reduces **rehashing overhead** and helps maintain cluster stability.

### 2. Hot-Key Problem

- A **hot key** is a key that receives disproportionately high traffic.
- If all requests hit the same node, it creates an **unbalanced workload**.
- **Solution:** Replicate or shard the hot key into multiple keys, allowing load to spread across nodes.

---

*Built to learn low-level system design, networking, and distributed caching concepts — one command at a time.*
