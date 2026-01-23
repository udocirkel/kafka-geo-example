# Kafka Geo-Replication – Learnings and Experiments

This document summarizes the experiments, validations, and learnings gathered while testing Apache Kafka in a geo-replicated, multi-cluster setup.  
The goal was to understand Kafka’s behavior under failure scenarios, replication, rebalancing, and transactional semantics, and to derive practical conclusions for production-ready architectures.

---

## 1. Initial Setup and Environment

### 1.1 Multi-Cluster Setup

Two Kafka clusters were set up to simulate a multi–data center environment:

- **DC1** (source cluster)
- **DC2** (target cluster)

Each cluster was configured with:

- 3 Kafka brokers per cluster
- Replication factor: **3**
- Minimum in-sync replicas (min ISR): **2**
- Volume mounts for persistent local storage
- Network configuration via **Docker Compose**

**Purpose:**  
This setup represents a realistic baseline for fault-tolerant Kafka clusters while remaining small enough for experimentation and failure testing.

### 1.2 ZooKeeper Setup (Demo Scope)

For simplicity of the demo environment, **each Kafka cluster runs with a single ZooKeeper instance**.

- This is **not production-ready**
- ZooKeeper high availability was not part of the experiment
- Kafka data-plane behavior remains valid, but ZooKeeper outages were not tested

### 1.3 Replication and Rebalancing Configuration

- One-way data replication was configured from **DC1 → DC2**
- Leader rebalancing was enabled to observe leader movement after broker recovery

**Purpose:**  
This allows testing geo-replication behavior, leader election, and recovery without introducing bi-directional complexity upfront.

---

## 2. Validation and Failure Testing

### 2.1 Geo-Replication Validation

- Data replication from DC1 to DC2 was verified
- Messages produced in DC1 appeared in DC2 via MirrorMaker 2

**Purpose:**  
To confirm correct baseline replication before introducing failures.

### 2.2 Broker Failure and Recovery Scenarios

Broker failures were simulated in both clusters:

- Controller failure
- Leader failure
- Follower failure

After failures, brokers were restarted to observe recovery behavior.

**Purpose:**  
To understand Kafka’s fault tolerance, leader election behavior, and the impact on availability and performance.

### 2.3 Leader Election

- When a leader broker failed, a new leader was elected from the ISR
- Leader election happened automatically without data loss

**Purpose:**  
To validate Kafka’s guarantee that only in-sync replicas are eligible for leadership, preserving data consistency.

### 2.4 Rebalancing After Recovery

- When the original leader came back online, leader rebalancing occurred
- Leadership could move back to the original broker

**Purpose:**  
To understand how Kafka redistributes leadership and how this impacts performance and latency.

---

## 3. Core Learnings: Storage, Partitions, and Performance

### 3.1 Local Storage and Performance

Kafka performance is strongly influenced by local storage characteristics:

- Kafka relies on **append-only log writes**
- Low write latency is critical
- Block-device–backed local storage significantly improves performance

**Learning:**  
Kafka is disk-latency sensitive. Slow storage directly increases producer latency.

### 3.2 Partitions and Log Files

- Each partition maps to log files on disk
- More partitions mean more files and higher I/O pressure

**Learning:**  
Partition count directly affects storage layout and operational complexity.

### 3.3 Leader Election, Rebalancing, and Performance

- Leader elections and rebalancing introduce latency spikes
- Network latency directly affects replication and ISR stability

**Learning:**  
Kafka requires low-latency, reliable networking to maintain ISR and stable performance.

---

## 4. Replication Semantics and In-Sync Replicas

### 4.1 In-Sync Replicas (ISR)

- Only replicas fully caught up with the leader belong to the ISR
- Only ISR replicas can become leaders

**Learning:**  
ISR size is the key mechanism protecting against data loss.

### 4.2 Broker Defaults and Misconfiguration Risks

- Default broker settings:
    - Replication factor: **1**
    - min ISR: **1**
- These defaults are not production-ready

**Learning:**  
Broker defaults must be overridden via environment variables:
- replicas = 3
- min.insync.replicas = 2 (quorum)

### 4.3 Automatic Topic Creation

- Topics created on first access use broker defaults
- This can silently create misconfigured topics

**Learning:**  
Automatic topic creation can lead to unsafe replication settings if broker defaults are not hardened.

---

## 5. MirrorMaker 2 and Geo-Replication

### 5.1 MirrorMaker 2 Configuration Modes

- Flow-based replication
- Single-direction (legacy-style) replication

**Learning:**  
Flow-based replication is more flexible but also more complex.

### 5.2 Replication Loops

Misconfiguration can cause replication loops, resulting in topic names like:

- `dc1.xyz`
- `dc1.dc1.xyz`
- `dc1.dc1.dc1.xyz`

**Learning:**  
Careful configuration is required to avoid infinite replication chains.

### 5.3 Topic Naming in Geo-Replication

- By default, replicated topics are prefixed with the source cluster name
- Naming can be customized via MirrorMaker 2 policies

**Learning:**  
Clear naming strategies are essential for operational clarity.

### 5.4 Changing Replication Direction

- MirrorMaker always replicates **all records it sees**
- It cannot detect whether records already exist in the target topic
- Switching replication direction causes at least **duplicate records**

**Learning:**  
To achieve a clean state after switching direction, the target topic must be rebuilt from scratch.

### 5.5 Offset Limitations

Offsets:
- Are cluster-local
- Are log-specific
- Are not globally unique

Offsets do **not** indicate:
- Which records already existed
- Which records are original
- Which records were replicated

Kafka records:
- Have no global message ID
- Contain key, value, headers, offset, timestamp

**Learning:**  
MirrorMaker is record-based, not origin-aware.

---

## 6. Transactions and Exactly-Once Semantics

### 6.1 Transaction Scope

- A single Kafka transaction can span:
    - Multiple topics
    - Multiple partitions

Kafka guarantees:
> “Either all partitions see the data, or none.”

**Learning:**  
This is the foundation of exactly-once semantics across partitions.

### &.2 Transaction Internals

- Transactions do not change partitioning
- They affect visibility and commit logic
- Additional control records and commit markers are written
- Each partition gets two additional log entries (visible in counters only)

**Learning:**  
Transactions increase log complexity but enable atomic multi-partition writes.

---

## 7. Consumer Commit Semantics

### 7.1 Auto Commit Enabled (`enable.auto.commit=true`)

Process:
1. Consumer calls `poll()` and receives messages N1, N2, N3
2. `auto.commit.interval.ms = 5000`
3. Offsets are committed after up to 5 seconds, regardless of processing outcome

Consequences:
- At-most-once behavior in failure scenarios
- Messages can be lost if the consumer crashes
- Only guarantee: offsets are periodically committed

**Key Takeaways:**
- Auto-commit is convenient but unsafe
- Auto-commit is periodic, not immediate

### 7.2 Auto Commit Disabled (`enable.auto.commit=false`)

Used when:
- Processing is critical
- No data loss or duplication is acceptable

Behavior:
- Offsets are committed manually via acknowledgment
- On crash, messages are redelivered
- Batch commit improves efficiency

**Learning:**  
Manual commit provides full control at the cost of higher complexity.

---

## 8. Transactions with Spring Kafka

### 8.1 Mandatory Rules

- `enable.auto.commit` must be **false**
- Auto-commit would break transactional semantics
- Offsets must not be written outside the transaction

### 8.2 Spring Kafka Behavior

- Spring internally calls:
    - `sendOffsetsToTransaction()`
    - `commitTransaction()`
- No explicit acknowledgment is required in listener code
- Spring does **not** automatically disable auto-commit

**Reason:**  
Spring cannot know whether:
- The consumer is truly transactional
- Offsets should always be committed transactionally
- Different listeners use different semantics

### 8.3 When a Consumer Is Truly Transactional

All conditions must be met:
- `enable.auto.commit = false`
- `transaction-id-prefix` configured
- `@KafkaListener` runs within a transaction
- Offsets committed via `sendOffsetsToTransaction()`

Without `@Transactional`, the consumer is **not transactional**.

---

## 9. Best Practices

- Topics represent business event streams
    - Examples: `order-events`, `payment-status`, `customer-changes`
- Events requiring ordering must share the same key
    - Good keys: orderId, paymentId, productId, sessionId
    - Bad keys: null, timestamp, country, type
- Choose a large keyspace for even partition distribution
- Never rely on default retention or compaction
    - Events: retention.ms (e.g. 7–30 days)
    - State: cleanup.policy=compact
- Use replication factor ≥ 3, min ISR = 2, acks=all
- Use self-explanatory topic names:
    - `<domain>.<entity>.<type>`

---

## 10. Concepts and Terminology

This section consolidates Kafka concepts such as:
- Kafka Cluster
- Bootstrap Servers
- Controller
- Partition
- Replication Factor
- Leader and Follower
- ISR
- Leader Epoch
- Follower Fetching
- High Watermark
- Leader Election
- Geo-Replication

(Definitions align with official Kafka and Confluent documentation.)
