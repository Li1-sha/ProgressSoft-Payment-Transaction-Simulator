# Order Platform

The system is split into four Maven modules that follow a strict layered architecture.  
**`order-domain`** is the foundation – it contains domain entities and exceptions, and has no external dependencies.  
**`order-persistence`** depends on `order-domain` and provides repository implementations (in‑memory and JDBC).  
**`order-service`** depends on both `order-domain` and `order-persistence` – it holds the business logic, validation, file import, and cross‑cutting proxy.  
**`order-cli`** is the top module, depending on `order-service` (transitively pulling all others) to provide the command‑line interface.

## Module Dependency Graph
order-cli → order-service → order-persistence → order-domain

## Module Dependency List

| Module | Depends On |
|--------|------------|
| **order-domain** | (none) |
| **order-persistence** | `order-domain` |
| **order-service** | `order-domain`, `order-persistence` |
| **order-cli** | `order-service` (transitive: `order-persistence`, `order-domain`) |

