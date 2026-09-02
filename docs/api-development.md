# API development with OpenAPI and Swagger UI

CupQueue uses a code-first API workflow. Spring MVC controllers and DTOs are
the source of truth. `springdoc-openapi` converts them into an OpenAPI document,
and Swagger UI renders the document for browsing and simple requests.

## Open the generated documentation

1. Start PostgreSQL from the repository root:

   ```powershell
   docker compose up -d --wait
   ```

2. Start the backend:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

3. Open one of the generated resources:

   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - OpenAPI JSON: <http://localhost:8080/v3/api-docs>
   - OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

## Document a controller

Springdoc understands Spring MVC and Jakarta Validation annotations. Add
OpenAPI annotations only for information that cannot be inferred from the code:

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Customer orders and pickup status")
class OrderController {

    @GetMapping("/{orderId}")
    @Operation(summary = "Get an order")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    OrderResponse getOrder(
            @Parameter(description = "Public order identifier", example = "ord_01JXYZ")
            @PathVariable String orderId) {
        // ...
    }
}
```

Document DTO examples and constraints at the model level:

```java
record CreateOrderRequest(
        @Schema(example = "store_01JXYZ") @NotBlank String storeId,
        @Schema(minimum = "1", example = "2") @Min(1) int quantity
) {}
```

Do not repeat facts already expressed by `@GetMapping`, Java types, or validation
annotations. Descriptions should explain business meaning and important rules.

## Try an endpoint

In Swagger UI, expand an operation, select **Try it out**, enter its parameters,
and select **Execute**. Swagger UI sends the request to the running backend and
shows the request URL, status, headers, and body.

When JWT authentication is implemented, mark a protected operation with:

```java
@SecurityRequirement(name = "bearerAuth")
```

Then select **Authorize** in Swagger UI and paste the JWT without the `Bearer`
prefix. The current development security configuration still permits all API
requests, so authorization is not required yet.

Swagger UI is for exploration and quick checks. Repeatable behavior belongs in
automated controller and integration tests.

## Import into Apifox

Import or bind Apifox to <http://localhost:8080/v3/api-docs>. Keep the backend
code as the source of truth and refresh Apifox from the generated specification;
do not manually maintain a second copy of the contract.

## Disable documentation in an environment

Set the following environment variable in any environment where the generated
documentation should not be exposed:

```text
SPRINGDOC_ENABLED=false
```

Disabling documentation does not replace API authentication or authorization.
