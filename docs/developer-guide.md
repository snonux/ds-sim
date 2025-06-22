# DS-Sim Developer Guide

This guide explains how to extend DS-Sim with new protocols, events, and features.

## Table of Contents
1. [Creating a New Protocol](#creating-a-new-protocol)
2. [Creating Custom Events](#creating-custom-events)
3. [Working with Time and Timestamps](#working-with-time-and-timestamps)
4. [Message Passing](#message-passing)
5. [Testing Your Components](#testing-your-components)
6. [Best Practices](#best-practices)

## Creating a New Protocol

### Step 1: Understand the Protocol Framework

All protocols extend `VSAbstractProtocol` and must implement both server and client sides, even if one side does nothing. The framework supports two initialization patterns:

- **Server-initiated**: Protocol starts from server side (`HAS_ON_SERVER_START = true`)
- **Client-initiated**: Protocol starts from client side (`HAS_ON_CLIENT_START = false`)

### Step 2: Create Your Protocol Class

```java
package protocols.implementations;

import core.VSMessage;
import protocols.VSAbstractProtocol;

public class VSMyProtocol extends VSAbstractProtocol {
    
    // Protocol-specific constants
    private static final String MSG_REQUEST = "REQUEST";
    private static final String MSG_RESPONSE = "RESPONSE";
    
    // State variables (separate for client/server)
    private int serverState;
    private int clientState;
    
    public VSMyProtocol() {
        // true = server starts, false = client starts
        super(HAS_ON_SERVER_START);
    }
    
    // Server-side implementation
    @Override
    public void onServerInit() {
        // Initialize server state
        serverState = 0;
        // Set any server preferences
        setBoolean("myprotocol.server.enabled", true);
    }
    
    @Override
    public void onServerStart() {
        // Server initiates the protocol
        VSMessage msg = new VSMessage();
        msg.setString("type", MSG_REQUEST);
        msg.setInteger("value", 42);
        sendMessage(msg); // Broadcasts to all clients
    }
    
    @Override
    public void onServerRecv(VSMessage message) {
        // Handle messages from clients
        String type = message.getString("type");
        if (MSG_RESPONSE.equals(type)) {
            int clientId = message.getSenderNum();
            int value = message.getInteger("value");
            process.log("Received response from client " + clientId + ": " + value);
        }
    }
    
    @Override
    public void onServerSchedule() {
        // Called when a scheduled server event fires
        // Schedule periodic actions using scheduleAt(time)
    }
    
    @Override
    public void onServerReset() {
        // Clean up server state
        serverState = 0;
    }
    
    // Client-side implementation
    @Override
    public void onClientInit() {
        // Initialize client state
        clientState = 0;
        setBoolean("myprotocol.client.autorespond", true);
    }
    
    @Override
    public void onClientStart() {
        // Only called if HAS_ON_CLIENT_START = false
    }
    
    @Override
    public void onClientRecv(VSMessage message) {
        // Handle messages from server
        String type = message.getString("type");
        if (MSG_REQUEST.equals(type)) {
            int value = message.getInteger("value");
            
            // Process the request
            clientState = value * 2;
            
            // Send response
            VSMessage response = new VSMessage();
            response.setString("type", MSG_RESPONSE);
            response.setInteger("value", clientState);
            sendMessage(response);
        }
    }
    
    @Override
    public void onClientSchedule() {
        // Called when a scheduled client event fires
    }
    
    @Override
    public void onClientReset() {
        // Clean up client state
        clientState = 0;
    }
}
```

### Step 3: Register Your Protocol

Add your protocol to `VSRegisteredEvents.init()`:

```java
public static void init(VSPrefs prefs_) {
    // ... existing registrations ...
    registerEvent("protocols.implementations.VSMyProtocol");
}
```

### Step 4: Protocol Communication Patterns

#### Broadcast (Server to All Clients)
```java
// In server code
VSMessage msg = new VSMessage();
sendMessage(msg); // Automatically sent to all clients
```

#### Unicast (Client to Server)
```java
// In client code
VSMessage msg = new VSMessage();
sendMessage(msg); // Automatically sent to server only
```

#### Selective Send (Server to Specific Client)
```java
// In server code - requires custom handling
VSMessage msg = new VSMessage();
msg.setInteger("targetClient", 2); // Custom field
sendMessage(msg); // Clients must filter
```

### Step 5: Using Scheduled Events

```java
// Schedule a task to run at local time 1000
scheduleAt(1000);

// This will trigger onServerSchedule() or onClientSchedule()
@Override
public void onServerSchedule() {
    // Periodic action
    process.log("Scheduled event triggered");
    
    // Reschedule for next interval
    long nextTime = process.getTime() + 500;
    scheduleAt(nextTime);
}
```

## Creating Custom Events

### Basic Event

```java
package events.implementations;

import events.VSAbstractEvent;

public class VSMyEvent extends VSAbstractEvent {
    
    private String eventData;
    
    @Override
    public void onInit() {
        // Called once when event is first created
        setClassname(getClass().getName());
        eventData = getString("myevent.data", "default");
    }
    
    @Override
    public void onStart() {
        // Called when event executes
        process.log("MyEvent executing with data: " + eventData);
        
        // Optionally schedule another event
        VSMyEvent nextEvent = new VSMyEvent();
        VSTask task = new VSTask(
            process.getTime() + 100,  // When to execute
            process,                   // Which process
            nextEvent,                // What event
            VSTask.LOCAL              // LOCAL or GLOBAL time
        );
        // Add task through simulator
    }
    
    @Override
    protected String createShortname(String savedShortname) {
        return "MyEvent";
    }
}
```

### Copyable Event

```java
public class VSCopyableMyEvent extends VSAbstractEvent implements VSCopyableEvent {
    
    private int counter;
    
    @Override
    public void initCopy(VSAbstractEvent copy) {
        if (copy instanceof VSCopyableMyEvent) {
            VSCopyableMyEvent myCopy = (VSCopyableMyEvent) copy;
            myCopy.counter = this.counter;
        }
    }
    
    // ... rest of implementation
}
```

### Timestamp-Triggered Event

```java
public class VSCustomTimestampEvent extends VSTimestampTriggeredEvent {
    
    public VSCustomTimestampEvent() {
        // Trigger when Lamport time >= 50
        super(50, ComparisonOperator.GREATER_EQUAL);
    }
    
    @Override
    protected void onTimestampReached() {
        // This fires when condition is met
        process.log("Lamport time reached 50!");
        
        // Highlight process
        if (process instanceof VSInternalProcess) {
            ((VSInternalProcess) process).highlightOn();
        }
    }
}
```

## Working with Time and Timestamps

### Time Types in DS-Sim

1. **Global Time**: Simulation-wide clock (milliseconds)
2. **Local Time**: Per-process clock with drift
3. **Lamport Time**: Logical timestamp for ordering
4. **Vector Time**: Vector clock for causality

### Accessing Time Values

```java
// In a protocol or event
long globalTime = process.getGlobalTime();
long localTime = process.getTime(); // Local time
long lamportTime = process.getLamportTime();
VSVectorTime vectorTime = process.getVectorTime();

// Increase timestamps (done automatically for messages)
process.increaseLamportTime();
process.increaseVectorTime();
```

### Clock Drift Simulation

```java
// In VSInternalProcess configuration
setFloat("process.clock.variance", 0.1f); // 10% drift
```

## Message Passing

### Message Structure

```java
// Creating a message
VSMessage msg = new VSMessage();

// Set data - supports various types
msg.setString("type", "REQUEST");
msg.setInteger("sequence", 42);
msg.setBoolean("urgent", true);
msg.setFloat("value", 3.14f);
msg.setLong("timestamp", System.currentTimeMillis());

// Get data
String type = msg.getString("type");
int seq = msg.getInteger("sequence", 0); // With default
```

### Message Metadata

```java
// In receiver
int sender = message.getSenderNum();
int receiver = message.getReceiverNum();
VSLamportTime msgLamport = message.getLamportTime();
VSVectorTime msgVector = message.getVectorTime();
```

### Complex Data in Messages

```java
// For complex data, convert to string
JSONObject data = new JSONObject();
data.put("items", new JSONArray(items));
msg.setString("data", data.toString());

// Or implement custom serialization
msg.setInteger("arraySize", array.length);
for (int i = 0; i < array.length; i++) {
    msg.setInteger("array_" + i, array[i]);
}
```

## Testing Your Components

### Unit Testing a Protocol

```java
@Test
public void testProtocolServerInit() {
    // Setup
    VSPrefs prefs = new VSPrefs();
    VSInternalProcess process = mock(VSInternalProcess.class);
    when(process.getNum()).thenReturn(0);
    
    // Create protocol
    VSMyProtocol protocol = new VSMyProtocol();
    protocol.setProcess(process);
    protocol.setPrefs(prefs);
    protocol.isServer(true);
    
    // Test initialization
    protocol.onInit();
    
    // Verify state
    assertTrue(prefs.getBoolean("myprotocol.server.enabled"));
}

@Test 
public void testMessageHandling() {
    // Setup protocol and process
    VSMyProtocol protocol = new VSMyProtocol();
    protocol.setProcess(mockProcess);
    protocol.isClient(true);
    protocol.onInit();
    
    // Create test message
    VSMessage msg = new VSMessage();
    msg.setString("type", "REQUEST");
    msg.setInteger("value", 10);
    
    // Test message handling
    protocol.onClientRecv(msg);
    
    // Verify response was sent
    verify(mockProcess).sendMessage(any(VSMessage.class));
}
```

### Integration Testing

```java
@Test
public void testProtocolIntegration() {
    // Create simulator components
    VSSimulatorVisualization viz = new VSSimulatorVisualization();
    VSTaskManager taskManager = new VSTaskManager(viz);
    
    // Create processes
    VSInternalProcess server = createProcess(0, viz);
    VSInternalProcess client1 = createProcess(1, viz);
    VSInternalProcess client2 = createProcess(2, viz);
    
    // Setup protocol on all processes
    setupProtocol(server, true, false); // server
    setupProtocol(client1, false, true); // client
    setupProtocol(client2, false, true); // client
    
    // Run simulation steps
    for (int i = 0; i < 10; i++) {
        taskManager.runTasks(i * 100);
    }
    
    // Verify expected behavior
    // Check logs, state changes, etc.
}
```

## Best Practices

### 1. State Management

- Keep server and client state separate
- Use preferences (VSPrefs) for configurable values
- Reset state properly in onReset methods

### 2. Error Handling

```java
@Override
public void onServerRecv(VSMessage message) {
    try {
        String type = message.getString("type");
        if (type == null) {
            process.log("Warning: Received message without type");
            return;
        }
        // Process message
    } catch (Exception e) {
        VSErrorHandler.handle(e, "Error processing message");
    }
}
```

### 3. Logging

```java
// Use process.log for important events
process.log("Protocol started");

// Add context to logs
process.log(String.format("Received %s from process %d", 
    msgType, message.getSenderNum()));

// Debug logging
if (getBoolean("debug.verbose")) {
    process.log("Debug: " + detailedInfo);
}
```

### 4. Performance Considerations

- Avoid scheduling too many events
- Use appropriate time types (LOCAL vs GLOBAL)
- Clean up completed tasks and state
- Be mindful of message size

### 5. Protocol Design

- Document message types and formats
- Handle missing or malformed messages gracefully
- Consider network failures and timeouts
- Test with various numbers of processes

### 6. Code Organization

```
protocols/implementations/
├── VSMyProtocol.java          # Main protocol implementation
├── messages/                  # Message type constants
│   └── MyProtocolMessages.java
└── state/                     # Complex state management
    └── MyProtocolState.java
```

## Common Patterns

### Request-Response Pattern

```java
// Server sends request with ID
msg.setInteger("requestId", nextRequestId++);
pendingRequests.put(requestId, new RequestInfo());

// Client echoes ID in response
response.setInteger("requestId", message.getInteger("requestId"));

// Server matches response to request
int requestId = message.getInteger("requestId");
RequestInfo info = pendingRequests.remove(requestId);
```

### Timeout Handling

```java
// Schedule timeout when sending request
long timeout = process.getTime() + 5000;
scheduleAt(timeout);
markTimeout(timeout, requestId);

// In onSchedule, check for timeouts
if (isTimeout(process.getTime())) {
    handleTimeout();
}
```

### State Machine Implementation

```java
enum ProtocolState {
    INIT, WAITING, PROCESSING, DONE
}

private ProtocolState state = ProtocolState.INIT;

@Override
public void onServerRecv(VSMessage message) {
    switch (state) {
        case INIT:
            handleInit(message);
            break;
        case WAITING:
            handleWaiting(message);
            break;
        // etc.
    }
}
```