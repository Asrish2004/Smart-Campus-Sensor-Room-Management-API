# Smart Campus Sensor & Room Management API

A fully-featured RESTful API built with **JAX-RS (Jersey 2.x)** and an embedded **Grizzly HTTP server** for the University of Westminster's Smart Campus initiative. Manages campus rooms and IoT sensors with historical reading logs.

---

## API Design Overview

The API follows REST constraints: stateless communication, a uniform resource interface, and a layered resource hierarchy that mirrors physical campus structure.

```
/api/v1                              → Discovery & metadata (HATEOAS)
/api/v1/rooms                        → Room collection
/api/v1/rooms/{roomId}               → Individual room
/api/v1/sensors                      → Sensor collection (filterable by ?type=)
/api/v1/sensors/{sensorId}           → Individual sensor
/api/v1/sensors/{sensorId}/readings  → Historical readings (sub-resource)
```

**Technology stack:** JAX-RS 2.x · Jersey 2.39.1 · Grizzly2 HTTP · Jackson JSON · Java 11 · Maven  
**Storage:** ConcurrentHashMap (in-memory, no database)

---

## Build & Run

### Prerequisites
- Java 11+
- Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api

# 2. Build the fat JAR (includes all dependencies)
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server starts at **http://localhost:8080** and the API root is **http://localhost:8080/api/v1**

Press `CTRL+C` to stop.

---

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. List all rooms
```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 3. Create a new room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":200}' | python3 -m json.tool
```

### 4. Register a new sensor (linked to an existing room)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":20.0,"roomId":"LIB-301"}' | python3 -m json.tool
```

### 5. Filter sensors by type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2" | python3 -m json.tool
```

### 6. Post a sensor reading (updates currentValue)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.8}' | python3 -m json.tool
```

### 7. Get reading history for a sensor
```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings | python3 -m json.tool
```

### 8. Try to delete a room with sensors (expect 409 Conflict)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 9. Register a sensor with a non-existent roomId (expect 422)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","roomId":"FAKE-999"}' | python3 -m json.tool
```

### 10. Post reading to a MAINTENANCE sensor (expect 403)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":12.0}' | python3 -m json.tool
```

---

## Report — Question Answers

### Part 1 — Service Architecture & Setup

**Q: Explain the default lifecycle of a JAX-RS Resource class.**

By default, JAX-RS creates a **new instance of each Resource class for every incoming HTTP request** (request-scoped lifecycle). This means instance fields are NOT shared between requests. To share mutable state across requests — such as the rooms and sensors collections — the application uses a singleton `DataStore` class backed by `ConcurrentHashMap`. `ConcurrentHashMap` is thread-safe; it allows multiple threads (one per request) to read and write concurrently without corrupting data or causing race conditions. Alternatives such as `@Singleton` scope exist in Jersey, but using a dedicated store class is cleaner and avoids making resource classes responsible for state management.

---

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design?**

HATEOAS (Hypermedia as the Engine of Application State) means that each API response includes links that tell the client what actions are available next. Instead of relying on static documentation or hard-coded URLs, clients discover and navigate the API dynamically by following links in responses — just as a human follows hyperlinks in a browser. Benefits include reduced client-server coupling (the server can change URL structures without breaking clients that follow links), self-documentation (clients can explore the API without reading a manual), and the ability to communicate available state transitions (e.g., a "delete" link only appears if deletion is currently permitted).

---

### Part 2 — Room Management

**Q: Returning full room objects vs. only IDs — implications?**

Returning **full objects** costs more network bandwidth per request but eliminates the need for clients to make follow-up GET requests to fetch detail. This is ideal when clients consistently need all fields (e.g., a facilities dashboard). Returning **only IDs** minimises initial payload size and is efficient when clients lazily load details or only need to check for a room's existence. For this API, full objects are returned because rooms are lightweight POJOs and campus management tools typically display all metadata at once, making the bandwidth tradeoff acceptable.

---

**Q: Is DELETE idempotent in your implementation?**

Yes. RFC 9110 defines an operation as idempotent when repeated identical requests produce the same server-side effect. In this implementation, the first `DELETE /api/v1/rooms/{roomId}` for an existing room removes it and returns `200 OK`. Any subsequent identical request finds no room in the map and returns `404 Not Found`. The response code differs, but the **server state is identical after each call** — the room remains absent. No additional side effects occur. Therefore DELETE is idempotent here.

---

### Part 3 — Sensor Operations & Linking

**Q: Consequences of a client sending wrong Content-Type to a @Consumes(APPLICATION_JSON) endpoint?**

JAX-RS inspects the `Content-Type` request header before invoking any method. If a client sends `text/plain` or `application/xml` to a method annotated `@Consumes(MediaType.APPLICATION_JSON)`, the runtime finds no matching consumer and automatically returns **HTTP 415 Unsupported Media Type**. The resource method body is never executed, protecting the application from unexpected deserialization errors.

---

**Q: Why is @QueryParam preferred over path-segment filtering?**

Path segments (e.g., `/sensors/type/CO2`) imply that `CO2` is a first-class child resource of `sensors`, which is semantically incorrect — "CO2 sensors" is a filtered view of the sensor collection, not a distinct resource hierarchy. Query parameters (`?type=CO2`) are the HTTP-idiomatic mechanism for filtering, sorting, and searching within a collection. They are optional, composable (e.g., `?type=CO2&status=ACTIVE`), and have no impact on the resource identity of `/api/v1/sensors`. This keeps URLs clean and RESTfully accurate.

---

### Part 4 — Sub-Resources

**Q: Architectural benefits of the Sub-Resource Locator pattern?**

The sub-resource locator (`@Path("/{sensorId}/readings")` returning a `SensorReadingResource` instance) provides several advantages:

1. **Separation of concerns** — Sensor registration logic (in `SensorResource`) is cleanly separated from reading history management (in `SensorReadingResource`). Each class has a single responsibility.
2. **Scalability** — As reading functionality grows (pagination, aggregation endpoints, CSV export), `SensorReadingResource` evolves independently without modifying sensor code.
3. **Testability** — Each class can be unit-tested in isolation.
4. **Reduced complexity** — A single monolithic resource class with all nested paths becomes hard to navigate and maintain. Delegation keeps line counts manageable.

---

### Part 5 — Error Handling

**Q: Why is HTTP 422 more semantically accurate than 404 for a missing referenced resource?**

`404 Not Found` means the **requested URL** does not exist on the server. In the scenario where a client POSTs a new sensor with a non-existent `roomId`, the URL `/api/v1/sensors` is perfectly valid and was found. The problem is **inside the request body** — a foreign-key reference is broken. HTTP `422 Unprocessable Entity` was designed precisely for this: the request is syntactically correct (well-formed JSON), the endpoint exists, but the server cannot process the content because it violates a semantic business rule. Using 422 gives the client precise, actionable diagnostic information.

---

**Q: Cybersecurity risks of exposing Java stack traces?**

Exposing raw stack traces to external API clients is dangerous for several reasons:

1. **Architecture disclosure** — Package names, class names, and method signatures reveal the internal software structure, lowering the effort required for targeted attacks.
2. **Dependency fingerprinting** — Library names and versions in traces (e.g., `jersey-server-2.x`, `jackson-databind-2.x`) allow attackers to cross-reference known CVEs for those exact versions.
3. **Path and schema leakage** — File system paths or SQL fragments that appear in exception messages expose server configuration and database schema layout.
4. **Business logic exposure** — Stack frames reveal execution flow and variable names, helping attackers understand application logic and find exploitable edge cases.

The `GlobalExceptionMapper` intercepts all unhandled exceptions, logs the full trace server-side for developers, and returns only a generic `500 Internal Server Error` JSON body to the client.

---

**Q: Why use JAX-RS filters for logging rather than per-method Logger calls?**

Logging is a **cross-cutting concern** — it applies uniformly to every endpoint regardless of business logic. Inserting `Logger.info()` into every resource method violates the DRY (Don't Repeat Yourself) principle, risks inconsistency (developers forget to add it to new methods), and pollutes business logic code with infrastructure concerns. A single `ContainerRequestFilter`/`ContainerResponseFilter` implementation intercepts 100% of traffic automatically, is registered once, and can be toggled or replaced without touching any resource class. This is the standard JAX-RS pattern for authentication, CORS, compression, and observability concerns.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                              # Embedded Grizzly server entry point
    ├── SmartCampusApplication.java            # JAX-RS @ApplicationPath config
    ├── DataStore.java                         # Singleton thread-safe in-memory store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ApiError.java
    ├── resource/
    │   ├── DiscoveryResource.java             # GET /api/v1
    │   ├── RoomResource.java                  # /api/v1/rooms
    │   ├── SensorResource.java                # /api/v1/sensors
    │   └── SensorReadingResource.java         # sub-resource /readings
    ├── exception/
    │   ├── RoomNotEmptyException.java         # 409
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundException.java # 422
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableException.java    # 403
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java         # 500 catch-all
    └── filter/
        └── LoggingFilter.java                 # Request & response logging
```
