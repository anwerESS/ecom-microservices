# 1) Start Zookeeper & Kafka (single-broker)

```bash
docker run -d --name zookeeper -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:7.5.0

docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  --link zookeeper \
  confluentinc/cp-kafka:7.5.0
```

* **Zookeeper** keeps cluster metadata (you used the classic ZK mode; new Kafka can also use KRaft).
* `KAFKA_ADVERTISED_LISTENERS` tells clients how to reach the broker from your machine.
* Single broker ⇒ replication factor must be 1.

# 2) Create and list topics

Inside the container:

```bash
kafka-topics --create --topic my-topic \
  --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

kafka-topics --list --bootstrap-server localhost:9092
kafka-topics --describe --topic my-topic --bootstrap-server localhost:9092
```

* **Topic** = named stream.
* **Partitions** = parallel lanes of a topic. Your `my-topic` has **3** partitions (0,1,2).
* **Replication factor** = copies for fault tolerance (1 here because one broker).

# 3) Produce and consume

Producer:

```bash
kafka-console-producer --topic my-topic --bootstrap-server localhost:9092
# type messages: "Hey Kafka", "Hello Consumer", ...
```

Consumer (read from the start of the log):

```bash
kafka-console-consumer --topic my-topic \
  --bootstrap-server localhost:9092 --from-beginning
```

# 4) Consumer **groups** (how Kafka scales and balances)

Start a consumer **in a group**:

```bash
kafka-console-consumer --topic my-topic \
  --bootstrap-server localhost:9092 --group my-group --from-beginning
```

Then inspect the group:

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group my-group
```

In your screenshot you see, per partition:

* **CURRENT-OFFSET** – last committed position for the group.
* **LOG-END-OFFSET** – latest message index on the broker.
* **LAG** – how many messages the group still has to process.
* **CONSUMER-ID/HOST/CLIENT-ID** – which consumer instance is currently assigned.

Conceptually:

* A **group** is a named set of consumers that together read a topic.
* Kafka **assigns partitions** to consumers within the same group (each partition is read by at most one group member).
* Scaling: add consumers to the group (up to the number of partitions) to parallelize.
* Fan-out: create **another group** name; each group gets its own full copy of the stream.

# 5) “Routing keys” in Kafka (message **keys** & partitioning)

Kafka doesn’t have AMQP “routing keys” or exchanges like RabbitMQ. The closest concept is the **message key**:

* When you send a record, you can include a **key** (bytes or string).
* The **partitioner** uses the key to pick a partition (default: hash(key) % partitions).
* All messages with the **same key** go to the **same partition**, preserving order for that key.
* Use cases: keep all events for `orderId=123` ordered by landing them in one partition.

Example with the console producer:

```bash
kafka-console-producer --topic my-topic --bootstrap-server localhost:9092 \
  --property "parse.key=true" --property "key.separator=:"
# Then type: user-42:hello
# Key = "user-42", Value = "hello"
```

# 6) “Bookmarks” in Kafka (offset commits & resume)

Your “bookmark” idea maps to **offsets**:

* Each message in a partition has a sequential **offset**.
* A consumer **commits** the last processed offset to Kafka (by default, periodic auto-commit).
* On restart, the consumer reads from its **committed offset** (its **bookmark**) and resumes.
* `--from-beginning` ignores existing bookmarks for the first run and starts at offset 0 (or earliest), but once offsets are committed, subsequent runs without `--from-beginning` resume from the bookmark.

Things to watch:

* If consumers are **too slow**, **LAG** grows—use `kafka-consumer-groups --describe` to monitor.
* Manual commit gives tighter control for “at-least-once” processing and retries.

# 7) Putting it together (what your run demonstrated)

1. You stood up a single-broker Kafka with Zookeeper.
2. You created `my-topic` (3 partitions) and `new-topic` (2 partitions).
3. You produced messages into `my-topic`.
4. You consumed them:

   * once as a simple, stateless console consumer (`--from-beginning`);
   * once as part of **group `my-group`**, which created/updated **bookmarked offsets**;
     `kafka-consumer-groups --describe` showed per-partition offsets and lag.

# 8) Small improvements (optional)

* Prefer a user-defined Docker **network** instead of `--link` (deprecated).
* For external clients, set `KAFKA_ADVERTISED_LISTENERS` appropriately (e.g., your host IP).
* If you later add more brokers, increase **replication factor** and use **min.insync.replicas**.

---

**Quick mental model**

* **Topic** = stream; **Partition** = parallel lane; **Key** = partitioning/routing hint;
* **Group** = competing consumers for scaling; **Offset commit** = bookmark to resume;
* **Lag** = work left for the group; **Describe** = see health & assignments.
