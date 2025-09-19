<h1>Redis Server in Java</h1>

<p>
This project is a Redis server implementation in <strong>Java</strong>, built from scratch as part of the 
<a href="https://codecrafters.io/">Codecrafters Redis Challenge</a>.  
It follows the <strong>client–server architecture</strong> and can accept requests from the Redis CLI over TCP sockets. 
Communication between client and server is handled using the <strong>RESP protocol</strong>.
</p>

<hr />

<h2>RESP Protocol</h2>

<p>
The Redis Serialization Protocol (<strong>RESP</strong>) is the core communication protocol between Redis clients and servers.
</p>

<p>References:</p>
<ul>
  <li><a href="https://redis.io/docs/latest/develop/reference/protocol-spec/">Redis Protocol Specification</a></li>
  <li><a href="https://lethain.com/redis-protocol/">Lethain’s Redis Protocol Guide</a></li>
</ul>

<p><strong>Key properties of RESP:</strong></p>
<ul>
  <li>Binary-safe: Works with arbitrary byte sequences.</li>
  <li>Length-prefixed: Encodes data with size information for efficient parsing.</li>
  <li>Simple &amp; human-readable: Easy for developers to debug.</li>
  <li>Fast to parse: Optimized for high-performance servers.</li>
</ul>

<hr />

<h2>Redis Use Cases</h2>

<p>
Redis is widely used in distributed systems due to its simplicity and speed. Some common patterns:
</p>

<ol>
  <li><strong>Distributed Locks</strong><br />
      Implemented with atomic commands like <code>INCR</code> and <code>DEL</code>, often wrapped in Lua scripts for transactional guarantees.
  </li>

  <li><strong>Pub/Sub (Lightweight Event Streaming)</strong><br />
      Similar in spirit to Kafka streams.  
      Events are appended to logs and consumed by <strong>consumer groups</strong>, which distribute messages across workers in parallel.
  </li>

  <li><strong>Rate Limiting</strong><br />
      Sliding window algorithms using atomic commands (<code>INCR</code>, <code>EXPIRE</code>) can enforce request limits efficiently.
  </li>

  <li><strong>Caching</strong><br />
      Fast in-memory key–value store for reducing database load.
  </li>

  <li><strong>Real-time Dashboards</strong><br />
      Using sorted sets (<code>ZSET</code>) for leaderboards, ranking systems, or priority queues.
  </li>
</ol>

<hr />

<h2>Common Challenges in Redis</h2>

<p>
Like any distributed system, Redis comes with design tradeoffs and operational challenges:
</p>

<h3>1. Consistent Hashing</h3>
<ul>
  <li>Redis keys are mapped to <strong>hash slots</strong>, which are distributed across nodes.</li>
  <li>If a node fails, consistent hashing allows reassigning only a small portion of slots to other healthy nodes instead of recomputing all keys.</li>
  <li>This reduces <strong>rehashing overhead</strong> and helps maintain cluster stability.</li>
</ul>

<h3>2. Hot-Key Problem</h3>
<ul>
  <li>A <strong>hot key</strong> is a key that receives disproportionately high traffic.</li>
  <li>If all requests hit the same node, it creates an <strong>unbalanced workload</strong>.</li>
  <li><strong>Solution:</strong> Replicate or shard the hot key into multiple keys, allowing load to spread across nodes.</li>
</ul>

<hr />

<h2>Repo Overview</h2>

<ul>
  <li><code>src/</code> → Core Java implementation of the Redis server.</li>
  <li><code>tests/</code> → Validation against Redis client commands.</li>
  <li>Implements RESP parsing, command handling, and basic Redis features.</li>
</ul>

<hr />

<p><em>Built to learn low-level system design, networking, and distributed caching concepts — one command at a time.</em></p>
