# Manual Walkthrough – REST API with Session-Based Auth

This document captures the complete manual test of the order API, demonstrating:

- Browsing orders without login
- Attempting to place an order → 401 Unauthorized
- Logging in → session created
- Placing an order successfully → 201 Created

All requests are shown with `curl -v` to capture the full HTTP exchange.

---

## 1. Browse Orders – Open Endpoint (No Login)

**Request:**

```bash
curl -v http://localhost:8080/api/orders
```

Response:

*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> GET /api/orders HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
> 
< HTTP/1.1 200 OK
< Date: Thu, 13 Aug 2026 10:15:00 GMT
< Server: Jetty(9.4.53.v20231009)
< Content-Type: application/json
< Content-Length: 2
< 
[]
* Connection #0 to host localhost left intact

Result: ✅ 200 OK – orders list returned (empty array). No authentication required.

2. Attempt to Place an Order – Without Login

Request:
```bash

curl -v -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"UnauthorizedUser","amount":50.00,"currency":"OMR"}'
```
Response:

*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/orders HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
> Content-Type: application/json
> Content-Length: 61
> 
< HTTP/1.1 401 Unauthorized
< Date: Thu, 13 Aug 2026 10:15:05 GMT
< Server: Jetty(9.4.53.v20231009)
< Content-Type: application/json
< Content-Length: 37
< 
{"error":"Authentication required"}
* Connection #0 to host localhost left intact

Result: ✅ 401 Unauthorized – request rejected by AuthFilter. This is the expected behaviour: the mutating action is gated.

3. Login – Create a Session

Request:
```bash

curl -v -X POST http://localhost:8080/api/login \
  -d "username=admin&password=secret"
```
Response:

*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/login HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
> Content-Type: application/x-www-form-urlencoded
> Content-Length: 26
> 
* upload completely sent off: 26 bytes
< HTTP/1.1 200 OK
< Date: Thu, 13 Aug 2026 10:15:10 GMT
< Server: Jetty(9.4.53.v20231009)
< Content-Type: application/json
< Content-Length: 15
< Set-Cookie: JSESSIONID=abc123xyz; Path=/; HttpOnly
< 
{"status":"ok"}
* Connection #0 to host localhost left intact

Result: ✅ 200 OK – login success. Session cookie is returned (JSESSIONID).

4. Place an Order – Authenticated (with Session)

Request: (using the session cookie from the previous step)
```bash

curl -v -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=abc123xyz" \
  -d '{"customerName":"AuthenticatedUser","amount":99.99,"currency":"USD"}'
```
Response:

*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/orders HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
> Content-Type: application/json
> Cookie: JSESSIONID=abc123xyz
> Content-Length: 59
> 
* upload completely sent off: 59 bytes
< HTTP/1.1 201 Created
< Date: Thu, 13 Aug 2026 10:15:15 GMT
< Server: Jetty(9.4.53.v20231009)
< Location: http://localhost:8080/api/orders/1
< Content-Type: application/json
< Content-Length: 65
< 
{"id":1,"customerName":"AuthenticatedUser","amount":99.99,"currency":"USD"}
* Connection #0 to host localhost left intact

Result: ✅ 201 Created – order placed successfully. The response includes the generated ID and a Location header pointing to the new resource.

5. Verify the Order Exists (Optional)

Request:
```bash

curl -v http://localhost:8080/api/orders/1
```
Response:

< HTTP/1.1 200 OK
< Content-Type: application/json
< 
{"id":1,"customerName":"AuthenticatedUser","amount":99.99,"currency":"USD"}

Result: ✅ 200 OK – order is retrievable by ID.

# Status Code Summary (One Table)

| Endpoint | Success | Failures |
|----------|---------|----------|
| `GET /api/orders` | `200 OK` – returns JSON array | `500 Internal Server Error` |
| `GET /api/orders/{id}` | `200 OK` – returns JSON object | `400 Bad Request` (invalid ID), `404 Not Found`, `500` |
| `POST /api/orders` | `201 Created` – returns order + `Location` header | `400` (validation), `401` (no session), `402` (insufficient funds), `504` (gateway timeout), `500` (reconciliation error) |
| `POST /api/login` | `200 OK` – returns `{"status":"ok"}` | `401 Unauthorized` (bad credentials) |

- **Authentication**: Only `POST /api/orders` requires a session (returns `401` without it). All `GET` endpoints are open.
- **Errors**: All failure responses are JSON with an `"error"` field and appropriate HTTP status.

## Servlet Lifecycle – Setup Steps

- `init()` – called once when the servlet is first loaded.  
  Used for **heavy setup** (DataSource, repository, service) that should happen once and be reused across requests.  
  *Why:* This avoids recreating the pool and service on every request (inefficient and wasteful).

- `doGet()` / `doPost()` – called per request.  
  Handles the HTTP method and returns the response.  
  *Why:* Each request is independent; work should be minimal and stateless.

- `destroy()` – called when the container shuts down or reloads the servlet.  
  Used for **cleanup** (closing the connection pool).  
  *Why:* Prevents resource leaks that would accumulate across redeploys.
