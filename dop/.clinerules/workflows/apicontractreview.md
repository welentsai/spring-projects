# API Contract Review Workflow

## Description
This workflow compares a Java Spring Boot Controller implementation against an OpenAPI specification to ensure the development is aligned with the API design contract.

## Usage
```
/apicontractreview <controller_class_path> <method_name> <openapi_yaml_path>
```

Example:
```
/apicontractreview src/main/java/com/example/dop/controller/CityController.java getAllCities openapi.yaml
```

## Workflow Steps

### 1. Parse Input Parameters
- Extract the controller class path
- Extract the specific method name to analyze
- Extract the OpenAPI YAML specification path
- Validate all inputs exist and are accessible

### 2. Analyze Java Controller Method
Examine the specified method in the controller class for:

#### HTTP Method & Path Analysis
- Extract `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` annotations
- Identify the HTTP method (GET, POST, PUT, DELETE, PATCH)
- Extract the request path/URL pattern
- Note path parameters using `@PathVariable`
- Note query parameters using `@RequestParam`

#### Request Analysis
- Analyze `@RequestBody` parameters and their types
- Check `@Valid` annotations for validation
- Examine custom validation annotations
- Note required vs optional parameters
- Extract content type expectations (consumes attribute)

#### Response Analysis
- Examine return type (ResponseEntity, direct objects, etc.)
- Note different response status codes used
- Check response body types
- Extract content type declarations (produces attribute)
- Identify error handling patterns

#### Security & Headers
- Look for security annotations (`@PreAuthorize`, `@Secured`, etc.)
- Check for custom header handling
- Note authentication/authorization requirements

### 3. Parse OpenAPI Specification
From the provided YAML file, extract for the matching endpoint:

#### Path & Operation Details
- Find the matching path in the OpenAPI spec
- Extract the HTTP method definition
- Get operation ID, summary, and description
- Note deprecated status if any

#### Parameters Specification
- Extract path parameters with types and constraints
- Extract query parameters with types, required status, and defaults
- Extract header parameters
- Note parameter validation rules (format, pattern, min, max, etc.)

#### Request Body Specification
- Extract request body schema
- Note required properties
- Check content types (application/json, multipart/form-data, etc.)
- Examine nested object structures
- Note validation constraints

#### Response Specification
- Extract all defined response status codes
- Get response schema for each status code
- Note response content types
- Check error response structures
- Examine response headers

#### Security Requirements
- Extract security schemes required
- Note OAuth scopes if applicable
- Check API key requirements

### 4. Perform Detailed Comparison

#### Path & Method Alignment
- ✅ **PASS** / ❌ **FAIL**: HTTP method matches
- ✅ **PASS** / ❌ **FAIL**: URL path pattern matches
- ⚠️ **WARNING**: Path parameter names differ but types match
- ❌ **FAIL**: Missing path parameters in implementation

#### Request Validation
- ✅ **PASS** / ❌ **FAIL**: Request body structure matches schema
- ✅ **PASS** / ❌ **FAIL**: Required parameters are enforced
- ⚠️ **WARNING**: Optional parameters missing in implementation
- ❌ **FAIL**: Parameter types don't match
- ⚠️ **WARNING**: Missing validation annotations for constrained fields
- ✅ **PASS** / ❌ **FAIL**: Content type alignment

#### Response Validation
- ✅ **PASS** / ❌ **FAIL**: Response object structure matches schema
- ✅ **PASS** / ❌ **FAIL**: Success status codes match
- ⚠️ **WARNING**: Missing error status codes in implementation
- ❌ **FAIL**: Response content type doesn't match
- ⚠️ **WARNING**: Additional fields in response not in schema

#### Security Alignment
- ✅ **PASS** / ❌ **FAIL**: Security requirements implemented
- ⚠️ **WARNING**: More restrictive security than specified
- ❌ **FAIL**: Missing required authentication

### 5. Generate Detailed Report

#### Summary Section
```
📊 API Contract Review Summary
Controller: {controller_class}#{method_name}
OpenAPI Spec: {openapi_yaml_path}
Overall Alignment: ✅ ALIGNED / ⚠️ PARTIALLY ALIGNED / ❌ NOT ALIGNED

Issues Found: {count}
- Critical: {critical_count}
- Warnings: {warning_count}
```

#### Detailed Findings
For each category, provide specific findings:

```
🔍 HTTP Method & Path
✅ PASS: HTTP method GET matches specification
❌ FAIL: Path parameter 'cityId' expected but 'id' found in implementation
   - Spec: /api/v1/cities/{cityId}
   - Impl: /api/v1/cities/{id}

📥 Request Analysis
✅ PASS: Request body structure matches City schema
⚠️ WARNING: Missing validation for 'name' field (minLength: 1)
   - Add @NotBlank annotation to name field

📤 Response Analysis  
✅ PASS: Success response (200) structure matches
❌ FAIL: Missing 404 Not Found response handling
   - Add explicit 404 response for invalid city ID

🔒 Security Analysis
⚠️ WARNING: No authentication required but spec defines security
   - Consider adding @PreAuthorize or similar security annotation
```

#### Recommendations
```
🔧 Recommended Actions:

1. HIGH PRIORITY:
   - Fix path parameter name from 'id' to 'cityId'
   - Add 404 Not Found response handling
   - Implement missing error responses

2. MEDIUM PRIORITY:
   - Add @NotBlank validation to required fields
   - Consider adding security annotations

3. LOW PRIORITY:
   - Add OpenAPI annotations for better documentation
   - Consider using @Valid for request body validation
```

#### Code Suggestions
Provide specific code snippets for fixes:

```java
// Current implementation:
@GetMapping("/{id}")
public ResponseEntity<CityEntity> getCityById(@PathVariable String id) {
    // ...
}

// Suggested fix:
@GetMapping("/{cityId}")
@ApiResponse(responseCode = "200", description = "City found")
@ApiResponse(responseCode = "404", description = "City not found")
public ResponseEntity<CityEntity> getCityById(@PathVariable String cityId) {
    try {
        CityEntity city = cityService.getCityById(cityId);
        return ResponseEntity.ok(city);
    } catch (CityNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
```

### 6. Additional Checks

#### Best Practices Validation
- Check for proper HTTP status code usage
- Validate REST naming conventions
- Ensure proper exception handling
- Check for consistent response patterns

#### Documentation Alignment
- Compare method documentation with OpenAPI descriptions
- Check if OpenAPI annotations are present and accurate
- Validate example values alignment

#### Type Safety Analysis
- Check for proper generic types usage
- Validate request/response DTO completeness
- Ensure proper serialization/deserialization

## Implementation Details

### Required Tools & Libraries
- Jackson for YAML parsing
- Spring Boot reflection for annotation analysis
- OpenAPI parser library
- Java AST parsing capabilities

### Error Handling
- Handle missing files gracefully
- Provide clear error messages for invalid inputs
- Continue analysis even if some checks fail

### Performance Considerations
- Cache parsed OpenAPI specs for repeated use
- Limit analysis to specified method only
- Provide progress indicators for large specs

## Example Usage Scenarios

### Scenario 1: New API Implementation
```
/apicontractreview src/main/java/com/example/dop/controller/OrderController.java createOrder api-spec.yaml
```

### Scenario 2: Existing API Validation
```
/apicontractreview src/main/java/com/example/dop/controller/CityController.java getAllCities openapi.yaml
```

### Scenario 3: API Version Migration
```
/apicontractreview src/main/java/com/example/dop/controller/UserController.java updateUser openapi-v2.yaml
```

## Integration with Development Workflow

### Pre-commit Hooks
- Run contract validation before commits
- Block commits that fail critical alignment checks
- Generate reports for code review

### CI/CD Integration
- Include contract validation in build pipeline
- Generate compliance reports
- Fail builds on critical misalignments

### IDE Integration
- Provide real-time contract validation
- Show inline warnings and suggestions
- Quick-fix actions for common issues
