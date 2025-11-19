# Testing Strategy

Comprehensive testing approach covering unit, integration, end-to-end, and performance testing.

## Unit Tests

**Coverage**: 5 unit tests per microservice.

**Framework**: JUnit 5 with Mockito.

**Focus**: Individual service methods with mocked dependencies.

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void testCreateUser_Success() {
        // Arrange
        UserRequest request = new UserRequest("test@example.com", "password");
        User user = new User(1L, "test@example.com");
        when(userRepository.save(any())).thenReturn(user);
        
        // Act
        User result = userService.createUser(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }
}
```

**Metrics Testing**: Tests use `SimpleMeterRegistry` instead of mocks to validate business metrics increment correctly.

**Execution**:
```bash
mvn test
```

## Integration Tests

**Coverage**: 5 integration tests per microservice.

**Approach**: Mocked external services, real database interactions.

**Purpose**: Verify service layer integration with repositories and internal components before deployment.

**Framework**: Spring Boot Test with `@SpringBootTest`.

**Example**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserServiceClient userServiceClient;
    
    @Test
    void testCreateOrder_IntegrationSuccess() throws Exception {
        // Mock external user service
        when(userServiceClient.getUser(1L))
            .thenReturn(new User(1L, "test@example.com"));
        
        // Test order creation
        mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":1,\"productId\":100}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(1));
    }
}
```

**Execution**:
```bash
mvn verify
```

Note: Integration tests run during Maven's `verify` phase, after unit tests.

## End-to-End Tests

**Framework**: Cypress.

**Focus**: N+1 query validation and complete user workflows.

**Test Cases**:
- User registration and authentication flow
- Product browsing and search
- Cart management and checkout
- Order creation and payment processing
- Data fetching optimization verification

**N+1 Query Tests**: Validate that list endpoints return data efficiently without triggering multiple database queries per item.

**Setup**:
```bash
cd tests/e2e
npm install
```

**Execution**:
```bash
# Interactive mode
npm run cy:open

# Headless mode
npm run cy:run

# Generate HTML report
npm run cy:run -- --reporter mochawesome
```

**Example Test**:
```javascript
describe('Order List N+1 Prevention', () => {
  it('should load orders with user data efficiently', () => {
    cy.intercept('GET', '/api/orders').as('getOrders');
    
    cy.visit('/orders');
    cy.wait('@getOrders');
    
    // Verify only one request was made
    cy.get('@getOrders.all').should('have.length', 1);
    
    // Verify data is displayed
    cy.get('.order-item').should('have.length.gt', 0);
    cy.get('.order-user-name').should('be.visible');
  });
});
```

**Reports**: HTML reports with screenshots and videos generated in `tests/e2e/cypress/reports/`.

## Performance Tests

**Framework**: Locust (Python-based load testing).

**Focus**: 
- N+1 query scenarios under load
- Business metric generation (cart abandonment)
- Service response times and throughput

**Test Scenarios**:

1. **Order Creation Load**: Simulate multiple users creating orders simultaneously.
2. **Cart Abandonment**: Create carts without completing purchase to trigger abandonment metrics.
3. **Product Listing**: Load product catalog pages to validate query optimization.

**Setup**:
```bash
cd tests/performance
pip install -r requirements.txt
```

**Execution**:
```bash
# Web UI mode
locust -f locustfile.py --host=http://api-gateway:8080

# Headless mode
locust -f locustfile.py --host=http://api-gateway:8080 \
  --users 100 --spawn-rate 10 --run-time 5m --headless

# With HTML report
locust -f locustfile.py --host=http://api-gateway:8080 \
  --users 100 --spawn-rate 10 --run-time 5m --headless \
  --html reports/locust-report.html --csv reports/locust-stats
```

**Example Test**:
```python
from locust import HttpUser, task, between

class EcommerceUser(HttpUser):
    wait_time = between(1, 3)
    
    @task(3)
    def browse_products(self):
        self.client.get("/api/products")
    
    @task(2)
    def view_product(self):
        self.client.get("/api/products/1")
    
    @task(1)
    def create_order(self):
        self.client.post("/api/orders", json={
            "userId": 1,
            "productId": 1,
            "quantity": 1
        })
```

**Metrics Collected**:
- Response times (min, max, average, percentiles)
- Requests per second
- Failure rate
- Resource utilization

**Reports**: HTML reports and CSV files in `tests/performance/reports/`.

## Continuous Integration

All tests run automatically in Jenkins pipeline:

1. **Unit Tests**: Run during Maven `test` phase
2. **Integration Tests**: Run during Maven `verify` phase
3. **Code Coverage**: JaCoCo reports generated
4. **SonarQube Analysis**: Quality gate validation
5. **E2E Tests**: Run after deployment to dev environment
6. **Performance Tests**: Run after E2E tests pass

Test reports archived as Jenkins artifacts for review.

## Test Data Management

**Unit/Integration Tests**: Use in-memory H2 database with test fixtures.

**E2E Tests**: Use seeded dev database with consistent test data.

**Performance Tests**: Use dedicated load testing environment with production-like data volume.

## Best Practices

- Mock external dependencies in unit tests
- Use test containers for integration tests requiring databases
- Clean up test data after E2E tests
- Monitor performance test trends over time
- Fail builds on quality gate violations
- Generate reports for all test types
