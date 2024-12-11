# System Architecture Documentation

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph Publishers
        P1[Publisher 1]
        P2[Publisher 2]
        P3[Publisher N]
    end

    subgraph Kafka Cluster
        K1[Kafka Broker 1]
        K2[Kafka Broker 2]
        K3[Kafka Broker 3]
        Z1[ZooKeeper]
        K1 <--> K2
        K2 <--> K3
        K3 <--> K1
        Z1 --> K1
        Z1 --> K2
        Z1 --> K3
    end

    subgraph Application Services
        CP[ContentPublisher Service]
        CS[ContentSubscriber Service]
        SS[SubscriptionService]
        ES[EmailService]
        US[UserService]
        KE[KeywordExtractor]
    end

    subgraph Subscribers
        S1[Subscriber 1]
        S2[Subscriber 2]
        S3[Subscriber N]
    end

    subgraph Storage
        DB[(H2 Database)]
    end

    P1 & P2 & P3 --> CP
    CP --> K1 & K2 & K3
    K1 & K2 & K3 --> CS
    CS --> SS
    SS --> ES
    ES --> S1 & S2 & S3
    US --> DB
    SS --> DB
    KE --> CS
```

## 2. Component Interaction Flow

```mermaid
sequenceDiagram
    participant Publisher
    participant ContentPublisher
    participant Kafka
    participant ContentSubscriber
    participant KeywordExtractor
    participant SubscriptionService
    participant EmailService
    participant Subscriber

    Publisher->>ContentPublisher: Submit Content
    ContentPublisher->>Kafka: Publish Content
    Kafka->>ContentSubscriber: Consume Content
    ContentSubscriber->>KeywordExtractor: Extract Keywords
    KeywordExtractor->>SubscriptionService: Match Keywords
    SubscriptionService->>EmailService: Notify Matching Subscribers
    EmailService->>Subscriber: Send Email Notification
```

## 3. Deployment Architecture

```mermaid
graph TB
    subgraph Docker Environment
        subgraph Kafka Cluster
            K1[Kafka Broker 1<br/>Port: 9092]
            K2[Kafka Broker 2<br/>Port: 9093]
            K3[Kafka Broker 3<br/>Port: 9094]
            ZK[ZooKeeper<br/>Port: 2181]
        end

        subgraph Application Container
            APP[Spring Boot Application<br/>Port: 8080]
            H2[H2 Database]
            APP --> H2
        end

        K1 & K2 & K3 <--> APP
        ZK --> K1 & K2 & K3
    end

    subgraph External
        Client[Client Applications]
        SMTP[SMTP Server]
    end

    Client --> APP
    APP --> SMTP
```

## 4. Data Flow Architecture

```mermaid
graph LR
    subgraph Input
        PC[Published Content]
        KW[Keywords]
        SUB[Subscriptions]
    end

    subgraph Processing
        CP[Content Processing]
        KE[Keyword Extraction]
        KM[Keyword Matching]
    end

    subgraph Storage
        CT[Content Topics]
        SD[Subscriber Data]
        KD[Keyword Data]
    end

    subgraph Output
        EN[Email Notifications]
        CN[CLI Notifications]
    end

    PC --> CP
    KW --> KE
    SUB --> KM
    CP --> CT
    KE --> KD
    KM --> SD
    CT & SD & KD --> EN
    CT & SD & KD --> CN
```

## 5. Fault Tolerance Design

```mermaid
graph TB
    subgraph Kafka Fault Tolerance
        B1[Broker 1] <--> B2[Broker 2]
        B2 <--> B3[Broker 3]
        B3 <--> B1
        
        subgraph Replication
            P1[Partition 1]
            P2[Partition 2]
            P3[Partition 3]
        end
        
        B1 --> P1
        B2 --> P2
        B3 --> P3
    end

    subgraph Application Resilience
        LB[Load Balancer]
        A1[App Instance 1]
        A2[App Instance 2]
        
        LB --> A1
        LB --> A2
    end

    subgraph Data Persistence
        DB1[(Primary DB)]
        DB2[(Backup DB)]
        
        DB1 <--> DB2
    end
