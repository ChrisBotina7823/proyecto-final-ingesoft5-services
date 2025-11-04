/**
 * E2E Test: Order History and Analytics
 * 
 * This test validates read-only operations across multiple services:
 * 1. View all orders
 * 2. View all payments
 * 3. View all shipping items
 * 4. Cross-reference data between services
 * 
 * Tests inter-service data consistency and relationship integrity
 */

describe('E2E: Order History and Analytics', () => {
  let sampleOrder
  let samplePayment
  let sampleShipping
  let sampleCart
  let sampleUser
  let sampleProduct

  before(() => {
    cy.log('Starting Order History and Analytics Test')
  })

  after(() => {
    cy.log('Order History Test Finished')
  })

  it('Step 1: Should retrieve all orders', () => {
    cy.log('Retrieving all orders from Order Service')
    
    cy.apiRequest('GET', '/order-service/api/orders')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const orders = response.body.collection
        
        // STRICT VALIDATION - Must have at least one order
        expect(orders.length, 'System must have at least one order (Test 01 should have created orders)').to.be.greaterThan(0)
        
        sampleOrder = orders[0]
        
        // Validate order DTO structure
        expect(sampleOrder).to.have.property('orderId')
        expect(sampleOrder).to.have.property('orderDate')
        expect(sampleOrder).to.have.property('orderFee')
        
        cy.log(`Retrieved ${orders.length} orders`)
        cy.log(`Sample order ID: ${sampleOrder.orderId}`)
        cy.log(`Order fee: $${sampleOrder.orderFee}`)
        
        // STRICT VALIDATION - Cart must be populated
        const cartData = sampleOrder.cartDto || sampleOrder.cart
        expect(cartData, 'Order must include cart information').to.exist
        expect(cartData).to.have.property('cartId')
        
        cy.log('Order includes cart information')
      })
  })

  it('Step 2: Should retrieve all carts', () => {
    cy.log('Retrieving all carts from Order Service')
    
    cy.apiRequest('GET', '/order-service/api/carts')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const carts = response.body.collection
        
        // STRICT VALIDATION - Must have at least one cart
        expect(carts.length, 'System must have at least one cart (Test 01 should have created carts)').to.be.greaterThan(0)
        
        sampleCart = carts[0]
        
        // Validate cart DTO structure
        expect(sampleCart).to.have.property('cartId')
        expect(sampleCart).to.have.property('userId')
        
        cy.log(`Retrieved ${carts.length} carts`)
        cy.log(`Sample cart ID: ${sampleCart.cartId}`)
        cy.log(`User ID: ${sampleCart.userId}`)
        
        // STRICT VALIDATION - User must be populated via inter-service call
        const userData = sampleCart.userDto || sampleCart.user
        expect(userData, 'CartService MUST populate user information via inter-service call').to.exist
        expect(userData).to.have.property('userId')
        expect(userData.userId).to.equal(sampleCart.userId, 
          'CartService must fetch correct user from UserService')
        
        cy.log('Cart includes user information (inter-service call working)')
      })
  })

  it('Step 3: Should retrieve all payments', () => {
    cy.log('Retrieving all payments from Payment Service')
    
    cy.apiRequest('GET', '/payment-service/api/payments')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const payments = response.body.collection
        
        // STRICT VALIDATION - Must have at least one payment
        expect(payments.length, 'System must have at least one payment (Test 01 should have created payments)').to.be.greaterThan(0)
        
        samplePayment = payments[0]
        
        // Validate payment DTO structure
        expect(samplePayment).to.have.property('paymentId')
        expect(samplePayment).to.have.property('isPayed')
        expect(samplePayment).to.have.property('paymentStatus')
        
        cy.log(`Retrieved ${payments.length} payments`)
        cy.log(`Sample payment ID: ${samplePayment.paymentId}`)
        cy.log(`Payment status: ${samplePayment.paymentStatus}`)
        cy.log(`Is payed: ${samplePayment.isPayed}`)
        
        // STRICT VALIDATION - Order must be populated via inter-service call
        const orderData = samplePayment.orderDto || samplePayment.order
        expect(orderData, 'PaymentService MUST populate order information via inter-service call').to.exist
        expect(orderData).to.have.property('orderId')
        
        cy.log('Payment includes order information (inter-service call working)')
        
        // Check nested cart information
        const cartData = orderData.cartDto || orderData.cart
        if (cartData) {
          cy.log('Nested DTO: Order includes cart information')
          expect(cartData).to.have.property('cartId')
          
          // Check double-nested user information
          const userData = cartData.userDto || cartData.user
          if (userData) {
            cy.log('Double-nested DTO: Cart includes user information')
            expect(userData).to.have.property('userId')
          }
        }
      })
  })

  it('Step 4: Should retrieve sample shipping and verify inter-service communication', () => {
    cy.log('Testing Shipping Service inter-service calls')
    
    // Get just a few shipping items to verify structure (limit check)
    cy.apiRequest('GET', '/shipping-service/api/shippings')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const shippings = response.body.collection
        
        // STRICT VALIDATION - Must have at least one shipping item
        expect(shippings.length, 'System must have at least one shipping item (Test 01 should have created shippings)').to.be.greaterThan(0)
        
        // Just check the first one for inter-service communication
        sampleShipping = shippings[0]
        
        // Validate basic structure
        expect(sampleShipping).to.have.property('productId')
        expect(sampleShipping).to.have.property('orderId')
        expect(sampleShipping).to.have.property('orderedQuantity')
        
        cy.log(`Retrieved ${shippings.length} shipping items`)
        cy.log(`Sample shipping: Product ${sampleShipping.productId}, Order ${sampleShipping.orderId}`)
        
        // STRICT VALIDATION - ProductDto must be populated
        const productData = sampleShipping.productDto || sampleShipping.product
        expect(productData, 'ShippingService MUST call ProductService to populate productDto').to.exist
        expect(productData.productId).to.equal(sampleShipping.productId, 
          'ShippingService must fetch correct product from ProductService')
        
        cy.log('ShippingService to ProductService communication works')
        
        // STRICT VALIDATION - OrderDto must be populated
        const orderData = sampleShipping.orderDto || sampleShipping.order
        expect(orderData, 'ShippingService MUST call OrderService to populate orderDto').to.exist
        expect(orderData.orderId).to.equal(sampleShipping.orderId, 
          'ShippingService must fetch correct order from OrderService')
        
        cy.log('ShippingService to OrderService communication works')
      })
  })

  it('Step 5: Should retrieve all users', () => {
    cy.log('Retrieving all users from User Service')
    
    cy.apiRequest('GET', '/user-service/api/users')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const users = response.body.collection
        
        // STRICT VALIDATION - Must have at least one user
        expect(users.length, 'System must have at least one user (Test 01 or Test 03 should have created users)').to.be.greaterThan(0)
        
        sampleUser = users[0]
        
        // Validate user DTO structure
        expect(sampleUser).to.have.property('userId')
        expect(sampleUser).to.have.property('firstName')
        expect(sampleUser).to.have.property('email')
        
        cy.log(`Retrieved ${users.length} users`)
        cy.log(`Sample user: ${sampleUser.firstName} (${sampleUser.email})`)
      })
  })

  it('Step 6: Should retrieve all products', () => {
    cy.log('Retrieving all products from Product Service')
    
    cy.apiRequest('GET', '/product-service/api/products')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const products = response.body.collection
        
        // STRICT VALIDATION - Must have at least one product
        expect(products.length, 'System must have at least one product (Test 04 should have created products)').to.be.greaterThan(0)
        
        sampleProduct = products[0]
        
        // Validate product DTO structure
        expect(sampleProduct).to.have.property('productId')
        expect(sampleProduct).to.have.property('productTitle')
        expect(sampleProduct).to.have.property('priceUnit')
        
        cy.log(`Retrieved ${products.length} products`)
        cy.log(`Sample product: ${sampleProduct.productTitle} - $${sampleProduct.priceUnit}`)
      })
  })

  it('Step 7: Should verify cart-user data consistency', () => {
    cy.log('Testing data consistency: Cart <-> User')
    
    // NO SKIP - Test must FAIL if no cart data
    expect(sampleCart, 'Cart must exist from Step 2').to.exist
    expect(sampleCart.userId, 'Cart must have userId').to.be.a('number')
    
    // Get user by ID and verify it matches cart's userId
    cy.apiRequest('GET', `/user-service/api/users/${sampleCart.userId}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Cart references user ${sampleCart.userId} but user not found (404) - DATA INTEGRITY BUG`)
        
        const user = response.body
        expect(user.userId).to.equal(sampleCart.userId)
        cy.log('Cart-User relationship is consistent')
      })
  })

  it('Step 8: Should verify order-cart data consistency', () => {
    cy.log('Testing data consistency: Order <-> Cart')
    
    // NO SKIP - Test must FAIL if no order-cart data
    expect(sampleOrder, 'Order must exist from Step 1').to.exist
    
    const cartData = sampleOrder.cartDto || sampleOrder.cart
    expect(cartData, 'Order must have cart information from Step 1').to.exist
    
    const cartId = cartData.cartId
    expect(cartId, 'Cart must have cartId').to.be.a('number')
    
    // Get cart by ID and verify relationship
    cy.apiRequest('GET', `/order-service/api/carts/${cartId}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Order references cart ${cartId} but cart not found (404) - DATA INTEGRITY BUG`)
        
        const cart = response.body
        expect(cart.cartId).to.equal(cartId)
        cy.log('Order-Cart relationship is consistent')
      })
  })

  it('Step 9: Should verify payment-order data consistency', () => {
    cy.log('Testing data consistency: Payment <-> Order')
    
    // NO SKIP - Test must FAIL if no payment-order data
    expect(samplePayment, 'Payment must exist from Step 3').to.exist
    
    const orderData = samplePayment.orderDto || samplePayment.order
    expect(orderData, 'Payment must have order information from Step 3').to.exist
    
    const orderId = orderData.orderId
    expect(orderId, 'Order must have orderId').to.be.a('number')
    
    // Get order by ID and verify relationship
    cy.apiRequest('GET', `/order-service/api/orders/${orderId}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Payment references order ${orderId} but order not found (404) - DATA INTEGRITY BUG`)
        
        const order = response.body
        expect(order.orderId).to.equal(orderId)
        cy.log('Payment-Order relationship is consistent')
      })
  })

  it('Step 10: Should verify shipping-product data consistency', () => {
    cy.log('Testing data consistency: Shipping <-> Product')
    
    // NO SKIP - Test must FAIL if no shipping data
    expect(sampleShipping, 'Shipping must exist from Step 4').to.exist
    expect(sampleShipping.productId, 'Shipping must have productId').to.be.a('number')
    
    // Get product by ID and verify relationship
    cy.apiRequest('GET', `/product-service/api/products/${sampleShipping.productId}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Shipping references product ${sampleShipping.productId} but product not found (404) - DATA INTEGRITY BUG`)
        
        const product = response.body
        expect(product.productId).to.equal(sampleShipping.productId)
        cy.log('Shipping-Product relationship is consistent')
      })
  })

  it('Step 11: Should verify shipping-order data consistency', () => {
    cy.log('Testing data consistency: Shipping <-> Order')
    
    // NO SKIP - Test must FAIL if no shipping data
    expect(sampleShipping, 'Shipping must exist from Step 4').to.exist
    expect(sampleShipping.orderId, 'Shipping must have orderId').to.be.a('number')
    
    // Get order by ID and verify relationship
    cy.apiRequest('GET', `/order-service/api/orders/${sampleShipping.orderId}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Shipping references order ${sampleShipping.orderId} but order not found (404) - DATA INTEGRITY BUG`)
        
        const order = response.body
        expect(order.orderId).to.equal(sampleShipping.orderId)
        cy.log('Shipping-Order relationship is consistent')
      })
  })

  it('Step 12: Should calculate order analytics', () => {
    cy.log('Calculating order analytics')
    
    cy.apiRequest('GET', '/order-service/api/orders')
      .then((ordersResponse) => {
        const orders = ordersResponse.body.collection
        
        cy.apiRequest('GET', '/payment-service/api/payments')
          .then((paymentsResponse) => {
            const payments = paymentsResponse.body.collection
            
            // Calculate statistics
            const totalOrders = orders.length
            const totalPayments = payments.length
            const completedPayments = payments.filter(p => p.paymentStatus === 'COMPLETED').length
            const pendingPayments = payments.filter(p => p.paymentStatus === 'NOT_STARTED' || p.paymentStatus === 'IN_PROGRESS').length
            
            const totalRevenue = orders.reduce((sum, order) => sum + (order.orderFee || 0), 0)
            const averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0
            
            cy.log('=' .repeat(70))
            cy.log('ORDER ANALYTICS')
            cy.log('=' .repeat(70))
            cy.log(`Total Orders: ${totalOrders}`)
            cy.log(`Total Payments: ${totalPayments}`)
            cy.log(`Completed Payments: ${completedPayments}`)
            cy.log(`Pending Payments: ${pendingPayments}`)
            cy.log(`Total Revenue: $${totalRevenue.toFixed(2)}`)
            cy.log(`Average Order Value: $${averageOrderValue.toFixed(2)}`)
            cy.log('=' .repeat(70))
            
            // Data integrity check
            if (totalOrders > 0 && totalPayments === 0) {
              cy.log('WARNING: Orders exist but no payments found')
              cy.log('This might be normal if payment processing is asynchronous')
            }
          })
      })
  })

  it('Summary: Order history and analytics executed', () => {
    cy.log('=' .repeat(70))
    cy.log('ORDER HISTORY & ANALYTICS - SUMMARY')
    cy.log('=' .repeat(70))
    cy.log('All services queried successfully')
    cy.log('Inter-service relationships validated')
    cy.log('Data consistency checks performed')
    cy.log('Analytics calculated')
    cy.log('=' .repeat(70))
    cy.log('KEY FINDINGS:')
    cy.log('  - Cart to User inter-service communication')
    cy.log('  - Payment to Order inter-service communication')
    cy.log('  - Shipping to Product + Order inter-service communication')
    cy.log('  - Nested DTOs (Payment to Order to Cart to User)')
    cy.log('  - Data integrity and referential consistency')
    cy.log('=' .repeat(70))
    cy.log('POTENTIAL ISSUES TO MONITOR:')
    cy.log('  - Check if RestTemplate calls are working correctly')
    cy.log('  - Verify DTOs are populated in GET operations')
    cy.log('  - Monitor orphaned records (e.g., cart with deleted user)')
    cy.log('  - Ensure foreign key constraints are enforced')
    cy.log('=' .repeat(70))
  })
})
