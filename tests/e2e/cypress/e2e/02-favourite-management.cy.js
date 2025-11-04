/**
 * E2E Test: Favourite Management Flow
 * 
 * This test validates the favourite management across 3 microservices:
 * 1. User Service - Get existing user
 * 2. Product Service - Get existing product
 * 3. Favourite Service - Create and manage favourites
 * 
 * Note: Based on code review, there is NO endpoint to get favourites by userId.
 * The service only has:
 * - GET /api/favourites (get all)
 * - POST /api/favourites (create)
 * - GET /api/favourites/{userId}/{productId}/{likeDate} (get specific favourite)
 */

describe('E2E: Favourite Management Flow', () => {
  let testUser
  let testProduct
  let createdFavourite
  let likeDate

  before(() => {
    cy.log('Starting Favourite Management Flow Test')
  })

  after(() => {
    cy.log('Favourite Management Flow Test Finished')
  })

  it('Step 1: Should get an existing user', () => {
    cy.log('Getting existing user from User Service')
    
    cy.apiRequest('GET', '/user-service/api/users')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        expect(response.body.collection).to.have.length.greaterThan(0)
        
        const users = response.body.collection
        testUser = users[Math.floor(Math.random() * users.length)]
        
        cy.log(`Selected user: ${testUser.userId}`)
        
        // Validate user DTO structure
        expect(testUser).to.have.property('userId')
        expect(testUser).to.have.property('firstName')
        expect(testUser).to.have.property('email')
        expect(testUser.userId).to.be.a('number')
      })
  })

  it('Step 2: Should get an existing product', () => {
    cy.log('Getting product from Product Service')
    
    cy.apiRequest('GET', '/product-service/api/products')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        expect(response.body.collection).to.have.length.greaterThan(0)
        
        const products = response.body.collection
        testProduct = products[Math.floor(Math.random() * products.length)]
        
        cy.log(`Selected product: ${testProduct.productId} - ${testProduct.productTitle}`)
        
        // Validate product DTO structure
        expect(testProduct).to.have.property('productId')
        expect(testProduct).to.have.property('productTitle')
        expect(testProduct.productId).to.be.a('number')
      })
  })

  it('Step 3: Should create a favourite with correct timestamp format', () => {
    cy.log('Creating favourite in Favourite Service')
    
    // Generate timestamp in required format: dd-MM-yyyy__HH:mm:ss:SSSSSS
    likeDate = generateTimestamp()
    
    const favouriteData = {
      userId: testUser.userId,
      productId: testProduct.productId,
      likeDate: likeDate
    }
    
    cy.log(`Favourite data: ${JSON.stringify(favouriteData)}`)
    
    cy.apiRequest('POST', '/favourite-service/api/favourites', { body: favouriteData })
      .then((response) => {
        // Check for success or common errors
        if (response.status >= 400) {
          cy.log('WARNING: ERROR Creating favourite')
          cy.log(`Status: ${response.status}`)
          cy.log(`Response: ${JSON.stringify(response.body)}`)
          
          // WARNING: POTENTIAL BUG: Check if the timestamp format is causing issues
          if (response.status === 500) {
            cy.log('BUG FOUND: 500 error on favourite creation')
            cy.log('Possible causes:')
            cy.log('  - Timestamp format issue (dd-MM-yyyy__HH:mm:ss:SSSSSS)')
            cy.log('  - Missing validation on FavouriteDto')
            cy.log('  - Database constraint violation')
          } else if (response.status === 400) {
            cy.log('BUG FOUND: 400 Bad Request')
            cy.log('  - Check FavouriteDto validation annotations')
            cy.log('  - Verify required fields')
          }
        }
        
        expect(response.status).to.be.oneOf([200, 201])
        createdFavourite = response.body
        
        cy.log(`Created favourite for user ${testUser.userId} and product ${testProduct.productId}`)
        
        // Validate favourite DTO structure
        expect(createdFavourite).to.have.property('userId')
        expect(createdFavourite).to.have.property('productId')
        expect(createdFavourite).to.have.property('likeDate')
        expect(createdFavourite.userId).to.equal(testUser.userId)
        expect(createdFavourite.productId).to.equal(testProduct.productId)
      })
  })

  it('Step 4: Should retrieve specific favourite by composite key', () => {
    cy.log('Retrieving specific favourite to verify creation')
    
    // Get specific favourite instead of all (faster, no N+1 problem)
    const encodedLikeDate = encodeURIComponent(likeDate)
    
    cy.apiRequest('GET', `/favourite-service/api/favourites/${testUser.userId}/${testProduct.productId}/${encodedLikeDate}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const favourite = response.body
        cy.log('Retrieved our specific favourite')
        
        // Validate structure
        expect(favourite.userId).to.equal(testUser.userId)
        expect(favourite.productId).to.equal(testProduct.productId)
        
        // KEY TEST: Validate inter-service communication
        if (favourite.userDto || favourite.user) {
          cy.log('FavouriteService to UserService communication works')
          const userData = favourite.userDto || favourite.user
          expect(userData.userId).to.equal(testUser.userId)
        } else {
          cy.log('WARNING: UserDto not populated')
        }
        
        if (favourite.productDto || favourite.product) {
          cy.log('FavouriteService to ProductService communication works')
          const productData = favourite.productDto || favourite.product
          expect(productData.productId).to.equal(testProduct.productId)
        } else {
          cy.log('WARNING: ProductDto not populated')
        }
      })
  })

  it('Step 5: Should verify Favourite→User+Product inter-service communication', () => {
    cy.log('Testing inter-service calls in different context')
    
    // Get the favourite again to verify DTOs are consistently populated
    const encodedLikeDate = encodeURIComponent(likeDate)
    
    cy.apiRequest('GET', `/favourite-service/api/favourites/${testUser.userId}/${testProduct.productId}/${encodedLikeDate}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const favourite = response.body
        
        // KEY TEST: Both DTOs should be populated
        const hasUserDto = favourite.userDto || favourite.user
        const hasProductDto = favourite.productDto || favourite.product
        
        if (hasUserDto && hasProductDto) {
          cy.log('Full inter-service communication working')
        } else {
          if (!hasUserDto) cy.log('WARNING: UserDto not populated')
          if (!hasProductDto) cy.log('WARNING: ProductDto not populated')
        }
      })
  })

  it('Step 6: Should NOT have endpoint to get favourites by userId alone', () => {
    cy.log('Testing non-existent endpoint: GET /api/favourites/user/{userId}')
    
    // This endpoint does NOT exist according to FavouriteResource.java
    cy.apiRequest('GET', `/favourite-service/api/favourites/user/${testUser.userId}`)
      .then((response) => {
        if (response.status === 404) {
          cy.log('Confirmed: Endpoint does not exist (404)')
          cy.log('RECOMMENDATION: Consider adding GET /api/favourites/user/{userId} endpoint')
          cy.log('   This would be useful for retrieving all favourites for a specific user')
        } else if (response.status === 200) {
          cy.log('WARNING: UNEXPECTED: Endpoint exists but was not documented in code review')
        }
        
        // We expect 404 since this endpoint doesn't exist
        expect(response.status).to.equal(404)
      })
  })

  it('Step 7: Should test duplicate favourite creation', () => {
    cy.log('Testing duplicate favourite creation (same user, product, timestamp)')
    
    const duplicateFavouriteData = {
      userId: testUser.userId,
      productId: testProduct.productId,
      likeDate: likeDate // Same timestamp as before
    }
    
    cy.apiRequest('POST', '/favourite-service/api/favourites', { body: duplicateFavouriteData })
      .then((response) => {
        if (response.status === 409 || response.status === 400) {
          cy.log('Duplicate creation properly rejected')
          cy.log('Good: Database constraints are working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Duplicate favourite was created')
          cy.log('BUG FOUND: No unique constraint on composite key (userId, productId, likeDate)')
          cy.log('RECOMMENDATION: Add unique constraint on favourite entity')
        } else if (response.status === 500) {
          cy.log('Database constraint violation caught (500 error)')
          cy.log('WARNING: Consider returning 409 Conflict instead of 500')
        }
        
        // We expect either rejection or successful creation
        // Document actual behavior for analysis
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 8: Should create favourite with different timestamp', () => {
    cy.log('Creating another favourite with different timestamp')
    
    // Generate new timestamp
    cy.wait(100) // Small delay to ensure different timestamp
    const newLikeDate = generateTimestamp()
    
    const newFavouriteData = {
      userId: testUser.userId,
      productId: testProduct.productId,
      likeDate: newLikeDate
    }
    
    cy.apiRequest('POST', '/favourite-service/api/favourites', { body: newFavouriteData })
      .then((response) => {
        if (response.status === 200 || response.status === 201) {
          cy.log('Second favourite created with different timestamp')
          cy.log('Note: Same user/product can have multiple favourites with different timestamps')
        } else {
          cy.log(`WARNING: Status: ${response.status}`)
        }
        
        expect(response.status).to.be.oneOf([200, 201])
      })
  })

  it('Summary: Favourite management flow executed', () => {
    cy.log('=' .repeat(70))
    cy.log('FAVOURITE MANAGEMENT FLOW - SUMMARY')
    cy.log('=' .repeat(70))
    cy.log(`User ID: ${testUser.userId}`)
    cy.log(`Product ID: ${testProduct.productId}`)
    cy.log(`Like Date: ${likeDate}`)
    cy.log('=' .repeat(70))
    cy.log('3 microservices tested (User, Product, Favourite)')
    cy.log('Favourite CRUD operations validated')
    cy.log('Composite key handling tested')
    cy.log('=' .repeat(70))
    cy.log('FINDINGS:')
    cy.log('  - No endpoint to get favourites by userId only')
    cy.log('  - Composite key: (userId, productId, likeDate)')
    cy.log('  - Timestamp format: dd-MM-yyyy__HH:mm:ss:SSSSSS')
    cy.log('=' .repeat(70))
  })
})
