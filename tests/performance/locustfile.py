"""
E-Commerce Microservices - High Concurrency & N+1 Performance Testing

FOCUS: Stress testing with high concurrent loads, NOT sequential user flows.

Testing Strategy:
-------------------
1. CRUD operations under extreme load (1000s of concurrent requests)
2. N+1 problem detection (endpoints with inter-service calls)
3. Independent requests (no dependencies between tasks)
4. Simple operations at maximum throughput

N+1 Problem Endpoints (HIGH PRIORITY):
------------------------------------------
These endpoints call other services for EACH item in a collection:

- GET /order-service/api/carts          → calls UserService N times
- GET /payment-service/api/payments     → calls OrderService N times (nested: Order→Cart→User)
- GET /shipping-service/api/shippings   → calls ProductService + OrderService N times
- GET /favourite-service/api/favourites → calls UserService + ProductService N times


Configuration:
--------------
See locust.conf for default parameters.
"""

import random
import time
from datetime import datetime
from locust import HttpUser, task, between


# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def get_error_message(response):
    """Extract error message from response (API Gateway or ExceptionMsg format)"""
    try:
        error_data = response.json()
        if 'msg' in error_data:
            return error_data['msg']
        error_msg = error_data.get('error', 'Unknown error')
        if 'message' in error_data:
            error_msg += f": {error_data['message']}"
        return error_msg
    except:
        return response.text[:200]


class TestDataGenerator:
    """Generates unique test data for concurrent request creation"""
    
    _counter = 0
    
    @classmethod
    def get_timestamp(cls):
        """Get unique timestamp"""
        cls._counter += 1
        return int(time.time() * 1000) + cls._counter
    
    @classmethod
    def get_unique_email(cls):
        """Generate unique email for user creation"""
        ts = cls.get_timestamp()
        return f"load_{ts}@test.com"
    
    @classmethod
    def get_unique_username(cls):
        """Generate unique username for credential creation"""
        ts = cls.get_timestamp()
        return f"user_{ts}"
    
    @classmethod
    def get_unique_sku(cls):
        """Generate unique SKU for product creation"""
        ts = cls.get_timestamp()
        return f"SKU-{ts}"
    
    @classmethod
    def get_datetime_formatted(cls):
        """Get current datetime in dd-MM-yyyy__HH:mm:ss:SSSSSS format"""
        now = datetime.now()
        return now.strftime("%d-%m-%Y__%H:%M:%S:") + str(now.microsecond).zfill(6)


# ============================================================================
# MAIN USER CLASS - All tasks are independent and concurrent
# ============================================================================

class MicroserviceStressUser(HttpUser):
    """
    High-concurrency stress testing user.
    
    All tasks execute independently with NO dependencies.
    Tasks are weighted by:
    - N+1 problem severity (higher weight = more critical to test)
    - Expected frequency in production
    """
    
    wait_time = between(0.1, 0.5)  # Very short wait = maximum concurrency
    
    
    # ========================================================================
    # CRITICAL: N+1 PROBLEM ENDPOINTS (Highest Priority)
    # ========================================================================
    
    @task(50)  # HIGHEST WEIGHT - Most critical N+1 problem
    def get_all_carts_with_users(self):
        """
        N+1 PROBLEM: CartService calls UserService for EACH cart.
        
        CartServiceImpl.findAll() iterates through all carts and calls:
        restTemplate.getForObject(userServiceUrl + userId)
        
        With 100 carts = 100+ HTTP calls to UserService!
        Under high concurrency, this becomes exponentially worse.
        """
        with self.client.get(
            "/order-service/api/carts",
            catch_response=True,
            name="[N+1] GET All Carts (calls UserService per cart)"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                carts = data.get('collection', [])
                # Log performance warning if response is slow
                if response.elapsed.total_seconds() > 2.0:
                    print(f"WARNING SLOW N+1: GET /carts took {response.elapsed.total_seconds():.2f}s for {len(carts)} carts")
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(40)  # VERY HIGH WEIGHT - Nested N+1 problem
    def get_all_payments_with_orders(self):
        """
        N+1 PROBLEM: PaymentService calls OrderService for EACH payment.
        NESTED N+1: OrderService calls CartService, which calls UserService.
        
        PaymentServiceImpl.findAll() -> Order -> Cart -> User
        = 3 levels of nested RestTemplate calls!
        
        With 50 payments, this can trigger 150+ inter-service HTTP calls.
        """
        with self.client.get(
            "/payment-service/api/payments",
            catch_response=True,
            name="[N+1 NESTED] GET All Payments (calls Order-Cart-User)"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                payments = data.get('collection', [])
                if response.elapsed.total_seconds() > 3.0:
                    print(f"WARNING SLOW NESTED N+1: GET /payments took {response.elapsed.total_seconds():.2f}s for {len(payments)} payments")
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(35)  # HIGH WEIGHT - Dual N+1 problem
    def get_all_shippings_with_products_and_orders(self):
        """
        DUAL N+1 PROBLEM: ShippingService calls both ProductService AND OrderService.
        
        ShippingServiceImpl.findAll() calls:
        - restTemplate.getForObject(productServiceUrl + productId) 
        - restTemplate.getForObject(orderServiceUrl + orderId)
        
        For EACH shipping item! With 200 items = 400 HTTP calls.
        """
        with self.client.get(
            "/shipping-service/api/shippings",
            catch_response=True,
            name="[N+1 DUAL] GET All Shippings (calls Product+Order per item)"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                shippings = data.get('collection', [])
                if response.elapsed.total_seconds() > 3.0:
                    print(f"WARNING SLOW DUAL N+1: GET /shippings took {response.elapsed.total_seconds():.2f}s for {len(shippings)} items")
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(30)  # HIGH WEIGHT - Dual N+1 problem
    def get_all_favourites_with_users_and_products(self):
        """
        DUAL N+1 PROBLEM: FavouriteService calls UserService AND ProductService.
        
        FavouriteServiceImpl.findAll() calls both services for EACH favourite.
        With 300 favourites = 600 HTTP calls.
        """
        with self.client.get(
            "/favourite-service/api/favourites",
            catch_response=True,
            name="[N+1 DUAL] GET All Favourites (calls User+Product per fav)"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                favourites = data.get('collection', [])
                if response.elapsed.total_seconds() > 2.5:
                    print(f"WARNING SLOW DUAL N+1: GET /favourites took {response.elapsed.total_seconds():.2f}s for {len(favourites)} favs")
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    # ========================================================================
    # BASELINE: Simple GET Collections (No N+1 problem)
    # ========================================================================
    
    @task(20)
    def get_all_users(self):
        """Baseline: Simple query, no inter-service calls"""
        with self.client.get(
            "/user-service/api/users",
            catch_response=True,
            name="[BASELINE] GET All Users (no N+1)"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(20)
    def get_all_products(self):
        """Baseline: Simple query, no inter-service calls"""
        with self.client.get(
            "/product-service/api/products",
            catch_response=True,
            name="[BASELINE] GET All Products (no N+1)"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(15)
    def get_all_orders(self):
        """
        Potential N+1: OrderService may populate cartDto via RestTemplate.
        Less severe than others but still worth monitoring.
        """
        with self.client.get(
            "/order-service/api/orders",
            catch_response=True,
            name="[POTENTIAL N+1] GET All Orders (calls Cart per order)"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                orders = data.get('collection', [])
                if response.elapsed.total_seconds() > 2.0:
                    print(f"WARNING SLOW: GET /orders took {response.elapsed.total_seconds():.2f}s for {len(orders)} orders")
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(10)
    def get_all_categories(self):
        """Baseline: Simple query, no inter-service calls"""
        with self.client.get(
            "/product-service/api/categories",
            catch_response=True,
            name="[BASELINE] GET All Categories (no N+1)"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    # ========================================================================
    # CREATE OPERATIONS - Reduced weight to focus on N+1 detection
    # For: User, Category, Product, Cart (baseline write operations)
    # ========================================================================
    
    @task(5)
    def create_user(self):
        """
        Create user with embedded credential.
        Tests: User creation + Credential creation in single transaction.
        """
        user_data = {
            "firstName": f"Load{TestDataGenerator.get_timestamp()}",
            "lastName": "Test",
            "email": TestDataGenerator.get_unique_email(),
            "phone": f"+1{random.randint(2000000000, 2999999999)}",
            "imageUrl": "https://via.placeholder.com/150"
        }
        
        with self.client.post(
            "/user-service/api/users",
            json=user_data,
            catch_response=True,
            name="[CREATE] POST User"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(3)
    def create_category(self):
        """Create product category - simple write operation"""
        category_data = {
            "categoryTitle": f"LoadTest_{TestDataGenerator.get_timestamp()}",
            "imageUrl": "https://via.placeholder.com/300"
        }
        
        with self.client.post(
            "/product-service/api/categories",
            json=category_data,
            catch_response=True,
            name="[CREATE] POST Category"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(4)
    def create_product(self):
        """
        Create product - requires existing category.
        Uses categoryId=1 (assumes it exists - idempotent for read operations).
        """
        product_data = {
            "productTitle": f"LoadTest Product {TestDataGenerator.get_timestamp()}",
            "imageUrl": "https://via.placeholder.com/400",
            "sku": TestDataGenerator.get_unique_sku(),
            "priceUnit": round(random.uniform(10.0, 500.0), 2),
            "quantity": random.randint(10, 1000),
            "category": {
                "categoryId": 1
            }
        }
        
        with self.client.post(
            "/product-service/api/products",
            json=product_data,
            catch_response=True,
            name="[CREATE] POST Product"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    
    
    @task(3)
    def create_cart(self):
        """
        Create cart - requires existing user.
        Uses userId=1 (assumes it exists).
        """
        cart_data = {
            "userId": 1  # Assumes user 1 exists
        }
        
        with self.client.post(
            "/order-service/api/carts",
            json=cart_data,
            catch_response=True,
            name="[CREATE] POST Cart"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            elif response.status_code == 500:
                response.failure(f"FK constraint [{response.status_code}]: User 1 may not exist")
            else:
                response.failure(f"Failed [{response.status_code}]: {get_error_message(response)}")
    

# ============================================================================
# EVENT LISTENERS - Performance monitoring and reporting
# ============================================================================

from locust import events

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Print test configuration on start"""
    print("=" * 80)
    print("HIGH CONCURRENCY & N+1 PERFORMANCE TEST")
    print("=" * 80)
    print(f"Target: {environment.host}")
    print("=" * 80)
    print("Test Focus:")
    print("  * N+1 Problem Detection (CartService, PaymentService, ShippingService, FavouriteService)")
    print("  * High Concurrency Stress Testing")
    print("  * CRUD Operations Under Load")
    print("=" * 80)
    print("CRITICAL: Watch for slow N+1 endpoints in console output")
    print("=" * 80)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Print performance summary on stop"""
    print("\n" + "=" * 80)
    print("PERFORMANCE TEST RESULTS")
    print("=" * 80)
    print(f"Total Requests:       {environment.stats.total.num_requests:,}")
    print(f"Total Failures:       {environment.stats.total.num_failures:,}")
    print(f"Failure Rate:         {(environment.stats.total.num_failures / max(environment.stats.total.num_requests, 1) * 100):.2f}%")
    print(f"Avg Response Time:    {environment.stats.total.avg_response_time:.2f} ms")
    print(f"Min Response Time:    {environment.stats.total.min_response_time:.2f} ms")
    print(f"Max Response Time:    {environment.stats.total.max_response_time:.2f} ms")
    print(f"Requests/Second:      {environment.stats.total.total_rps:.2f}")
    print("=" * 80)
    
    # Identify slowest endpoints
    print("\nSLOWEST ENDPOINTS (Potential N+1 Problems):")
    print("-" * 80)
    
    sorted_stats = sorted(
        [s for s in environment.stats.entries.values() if s.num_requests > 0],
        key=lambda x: x.avg_response_time,
        reverse=True
    )[:10]
    
    for stat in sorted_stats:
        print(f"{stat.name:60s} | Avg: {stat.avg_response_time:8.2f}ms | Max: {stat.max_response_time:8.2f}ms")
    
    print("=" * 80)
    
    # N+1 specific analysis
    n1_endpoints = [
        "[N+1] GET All Carts",
        "[N+1 NESTED] GET All Payments",
        "[N+1 DUAL] GET All Shippings",
        "[N+1 DUAL] GET All Favourites"
    ]
    
    print("\nN+1 PROBLEM ANALYSIS:")
    print("-" * 80)
    
    for endpoint_name in n1_endpoints:
        matching_stats = [s for s in environment.stats.entries.values() if endpoint_name in s.name]
        if matching_stats:
            stat = matching_stats[0]
            print(f"{stat.name:60s}")
            print(f"  Requests: {stat.num_requests:,} | Failures: {stat.num_failures:,}")
            print(f"  Avg: {stat.avg_response_time:.2f}ms | P95: {stat.get_response_time_percentile(0.95):.2f}ms | Max: {stat.max_response_time:.2f}ms")
            
            # Performance warning
            if stat.avg_response_time > 1000:
                print(f"  WARNING CRITICAL: Avg response time > 1s - SEVERE N+1 PROBLEM!")
            elif stat.avg_response_time > 500:
                print(f"  WARNING: Avg response time > 500ms - N+1 problem detected")
            else:
                print(f"  OK: Performance acceptable")
            print()
    
    print("=" * 80)
    print("\nRECOMMENDATIONS:")
    print("-" * 80)
    print("* If N+1 endpoints show >500ms avg: Implement batch fetching or caching")
    print("* If CREATE operations fail with FK errors: Ensure base data exists (user/product/category ID=1)")
    print("* If overall failure rate >5%: Check microservice logs for errors")
    print("=" * 80)
