"""
E-Commerce Microservices - Performance Testing with Business Metrics

DUAL FOCUS:
-----------
1. HIGH CONCURRENCY: Stress testing with 1000s of concurrent requests (N+1 detection)
2. BUSINESS METRICS: Complete user flows that generate observable metrics

Testing Strategy:
-------------------
1. N+1 problem detection (endpoints with inter-service calls)
2. Complete e-commerce flows (user registration → browse → cart → order → payment)
3. Business metrics activation (orders, payments, users, carts, favourites)
4. CRUD operations under extreme load

N+1 Problem Endpoints (HIGH PRIORITY):
------------------------------------------
- GET /order-service/api/carts          → calls UserService N times
- GET /payment-service/api/payments     → calls OrderService N times (nested: Order→Cart→User)
- GET /shipping-service/api/shippings   → calls ProductService + OrderService N times
- GET /favourite-service/api/favourites → calls UserService + ProductService N times

Business Metrics Generated:
-----------------------------
- orders_created_total           (Order creation counter)
- order_value_dollars            (Order value distribution)
- payments_processed_total       (Payment counter by status)
- payment_volume_dollars         (Revenue tracking)
- users_registered_total         (User registration counter)
- user_logins_total              (Login attempts)
- carts_deleted_total            (Cart deletion/abandonment)
- cart_abandonment_rate          (Calculated metric: carts vs orders)

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

class BusinessMetricsUser(HttpUser):
    """
    User that executes complete e-commerce flows to generate business metrics.
    
    Simulates real user behavior:
    1. Register → Browse products → Add to favourites
    2. Create cart → Create order → Process payment
    3. View order history
    
    These flows activate ALL business metrics in Grafana.
    """
    
    wait_time = between(1, 3)  # Realistic user think time
    
    def on_start(self):
        """Initialize user session with IDs for flow continuity"""
        self.user_id = None
        self.product_id = None
        self.cart_id = None
        self.order_id = None
    
    @task(10)
    def complete_purchase_flow(self):
        """
        FULL E-COMMERCE FLOW - Generates multiple business metrics:
        - users_registered_total
        - orders_created_total
        - order_value_dollars
        - payments_processed_total
        - payment_volume_dollars
        """
        # Step 1: Register user
        user_data = {
            "firstName": f"Buyer{TestDataGenerator.get_timestamp()}",
            "lastName": "Test",
            "email": TestDataGenerator.get_unique_email(),
            "phone": f"+1{random.randint(2000000000, 2999999999)}",
            "imageUrl": "https://via.placeholder.com/150"
        }
        
        response = self.client.post("/user-service/api/users", json=user_data, name="[FLOW] Register User")
        if response.status_code not in [200, 201]:
            return
        
        user = response.json()
        user_id = user.get('userId')
        
        # Step 2: Browse products (get first available)
        response = self.client.get("/product-service/api/products", name="[FLOW] Browse Products")
        if response.status_code != 200:
            return
        
        products_data = response.json()
        products = products_data.get('collection', [])
        if not products:
            return
        
        product = random.choice(products)
        product_id = product.get('productId')
        
        # Step 3: Create cart
        cart_data = {"userId": user_id}
        response = self.client.post("/order-service/api/carts", json=cart_data, name="[FLOW] Create Cart")
        if response.status_code not in [200, 201]:
            return
        
        cart = response.json()
        cart_id = cart.get('cartId')
        
        # Step 4: Create order (ACTIVATES: orders_created_total, order_value_dollars)
        order_fee = round(random.uniform(50.0, 500.0), 2)
        order_data = {
            "orderDate": TestDataGenerator.get_datetime_formatted(),
            "orderDesc": f"Load test order - Cart abandonment tracking",
            "orderFee": order_fee,
            "cart": {"cartId": cart_id}
        }
        
        response = self.client.post("/order-service/api/orders", json=order_data, name="[FLOW] Create Order")
        if response.status_code not in [200, 201]:
            return
        
        order = response.json()
        order_id = order.get('orderId')
        
        # Step 5: Create shipping item
        shipping_data = {
            "product": {"productId": product_id},
            "order": {"orderId": order_id}
        }
        
        self.client.post("/shipping-service/api/shippings", json=shipping_data, name="[FLOW] Create Shipping")
        
        # Step 6: Process payment (ACTIVATES: payments_processed_total, payment_volume_dollars)
        # Randomly succeed or fail to test payment metrics
        payment_status = random.choices(
            ["ACCEPTED", "REJECTED", "PENDING"],
            weights=[85, 10, 5],  # 85% success rate
            k=1
        )[0]
        
        payment_data = {
            "isPayed": payment_status == "ACCEPTED",
            "paymentStatus": payment_status,
            "order": {"orderId": order_id}
        }
        
        self.client.post("/payment-service/api/payments", json=payment_data, name=f"[FLOW] Process Payment ({payment_status})")
    
    @task(5)
    def browse_and_favourite(self):
        """
        Browse products and add to favourites.
        Requires existing user (uses ID 1-10).
        """
        # Get random user ID (assumes users 1-10 exist)
        user_id = random.randint(1, 10)
        
        # Browse products
        response = self.client.get("/product-service/api/products", name="[FLOW] Browse Products")
        if response.status_code != 200:
            return
        
        products_data = response.json()
        products = products_data.get('collection', [])
        if not products:
            return
        
        # Add random product to favourites
        product = random.choice(products)
        product_id = product.get('productId')
        
        favourite_data = {
            "likeDate": TestDataGenerator.get_datetime_formatted(),
            "user": {"userId": user_id},
            "product": {"productId": product_id}
        }
        
        self.client.post("/favourite-service/api/favourites", json=favourite_data, name="[FLOW] Add Favourite")
    
    @task(3)
    def simulate_cart_abandonment(self):
        """
        Create cart but DON'T complete order.
        This increases cart_abandonment_rate metric.
        """
        # Register user
        user_data = {
            "firstName": f"Abandoner{TestDataGenerator.get_timestamp()}",
            "lastName": "Test",
            "email": TestDataGenerator.get_unique_email(),
            "phone": f"+1{random.randint(2000000000, 2999999999)}",
            "imageUrl": "https://via.placeholder.com/150"
        }
        
        response = self.client.post("/user-service/api/users", json=user_data, name="[ABANDON] Register User")
        if response.status_code not in [200, 201]:
            return
        
        user = response.json()
        user_id = user.get('userId')
        
        # Create cart
        cart_data = {"userId": user_id}
        response = self.client.post("/order-service/api/carts", json=cart_data, name="[ABANDON] Create Cart")
        if response.status_code not in [200, 201]:
            return
        
        # DON'T create order - simulate abandonment
        # This increases cart count without increasing order count
        # Result: cart_abandonment_rate goes up


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
    print("PERFORMANCE TEST - N+1 DETECTION + BUSINESS METRICS GENERATION")
    print("=" * 80)
    print(f"Target: {environment.host}")
    print("=" * 80)
    print("Test Objectives:")
    print("  1. N+1 Problem Detection (CartService, PaymentService, ShippingService, FavouriteService)")
    print("  2. Business Metrics Generation (Orders, Payments, Users, Cart Abandonment)")
    print("  3. High Concurrency Stress Testing")
    print("  4. CRUD Operations Under Load")
    print("=" * 80)
    print("User Classes:")
    print("  * BusinessMetricsUser    - Complete e-commerce flows (metrics generation)")
    print("  * MicroserviceStressUser - High-concurrency N+1 detection")
    print("=" * 80)
    print("Business Metrics Being Generated:")
    print("  - orders_created_total")
    print("  - order_value_dollars")
    print("  - payments_processed_total (by status)")
    print("  - payment_volume_dollars")
    print("  - users_registered_total")
    print("  - cart_abandonment_rate (carts without orders)")
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
    
    # Business metrics analysis
    print("\nBUSINESS METRICS GENERATED:")
    print("-" * 80)
    
    business_endpoints = {
        "[FLOW] Register User": "users_registered_total",
        "[FLOW] Create Order": "orders_created_total + order_value_dollars",
        "[FLOW] Process Payment": "payments_processed_total + payment_volume_dollars",
        "[ABANDON] Create Cart": "cart_abandonment_rate (increases numerator)",
        "[FLOW] Add Favourite": "favourites tracking"
    }
    
    total_users = 0
    total_orders = 0
    total_payments = 0
    total_abandoned_carts = 0
    
    for endpoint_name, metric in business_endpoints.items():
        matching_stats = [s for s in environment.stats.entries.values() if endpoint_name in s.name]
        if matching_stats:
            stat = matching_stats[0]
            success_count = stat.num_requests - stat.num_failures
            print(f"{endpoint_name:35s} | Success: {success_count:5,} | Metric: {metric}")
            
            # Track totals for summary
            if "Register User" in endpoint_name:
                total_users += success_count
            elif "Create Order" in endpoint_name:
                total_orders += success_count
            elif "Process Payment" in endpoint_name:
                total_payments += success_count
            elif "ABANDON" in endpoint_name:
                total_abandoned_carts += success_count
    
    print("-" * 80)
    print(f"ESTIMATED METRICS IN PROMETHEUS:")
    print(f"  users_registered_total:    ~{total_users:,} new users")
    print(f"  orders_created_total:      ~{total_orders:,} new orders")
    print(f"  payments_processed_total:  ~{total_payments:,} payments")
    print(f"  Abandoned carts created:   ~{total_abandoned_carts:,} carts without orders")
    if total_orders + total_abandoned_carts > 0:
        abandonment_rate = (total_abandoned_carts / (total_orders + total_abandoned_carts)) * 100
        print(f"  cart_abandonment_rate:     ~{abandonment_rate:.1f}%")
    print("-" * 80)
    
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
                print(f"  ⚠️  WARNING CRITICAL: Avg response time > 1s - SEVERE N+1 PROBLEM!")
            elif stat.avg_response_time > 500:
                print(f"  ⚠️  WARNING: Avg response time > 500ms - N+1 problem detected")
            else:
                print(f"  ✅ OK: Performance acceptable")
            print()