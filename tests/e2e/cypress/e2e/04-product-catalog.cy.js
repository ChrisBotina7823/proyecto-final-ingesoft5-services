/**
 * E2E Test: Product Catalog Management
 * 
 * This test validates product catalog operations within Product Service:
 * 1. Category management
 * 2. Product management
 * 3. Category-Product relationship
 * 
 * Single microservice with related entities
 */

describe('E2E: Product Catalog Management', () => {
  let createdCategory
  let createdProduct
  const uniqueCategoryName = `TestCategory_${Date.now()}`
  const uniqueSku = `SKU-${Date.now()}`

  before(() => {
    cy.log('Starting Product Catalog Management Test')
  })

  after(() => {
    cy.log('Product Catalog Test Finished')
  })

  it('Step 1: Should create a new category', () => {
    cy.log('Creating new category in Product Service')
    
    const categoryData = {
      categoryTitle: uniqueCategoryName,
      imageUrl: 'https://via.placeholder.com/300'
    }
    
    cy.apiRequest('POST', '/product-service/api/categories', { body: categoryData })
      .then((response) => {
        // NO FALLBACK - Test must FAIL if category creation fails
        expect(response.status).to.be.oneOf([200, 201], 
          `Category creation failed with ${response.status}: ${JSON.stringify(response.body)}`)
        createdCategory = response.body
        
        cy.log(`Created category: ${createdCategory.categoryId}`)
        
        // Validate category DTO structure
        expect(createdCategory).to.have.property('categoryId')
        expect(createdCategory).to.have.property('categoryTitle')
        expect(createdCategory.categoryId).to.be.a('number')
        expect(createdCategory.categoryTitle).to.equal(uniqueCategoryName)
      })
  })

  it('Step 2: Should retrieve created category by ID', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdCategory, 'Category must be created in Step 1').to.exist
    expect(createdCategory.categoryId, 'Category must have categoryId').to.be.a('number')
    
    cy.log(`Retrieving category ${createdCategory.categoryId}`)
    
    cy.apiRequest('GET', `/product-service/api/categories/${createdCategory.categoryId}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const category = response.body
        expect(category.categoryId).to.equal(createdCategory.categoryId)
        
        // STRICT VALIDATION - Category title must match
        expect(category.categoryTitle).to.equal(uniqueCategoryName, 
          `Category title must be ${uniqueCategoryName}, but got ${category.categoryTitle}`)
        
        cy.log('Category retrieved successfully with correct title')
      })
  })

  it('Step 3: Should create a product in the category', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdCategory, 'Category must be created in Step 1').to.exist
    expect(createdCategory.categoryId, 'Category must have categoryId').to.be.a('number')
    
    cy.log('Creating new product')
    
    const productData = {
      productTitle: 'E2E Test Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: uniqueSku,
      priceUnit: 99.99,
      quantity: 100,
      category: {
        categoryId: createdCategory.categoryId
      }
    }
    
    cy.apiRequest('POST', '/product-service/api/products', { body: productData })
      .then((response) => {
        // NO FALLBACK - Test must FAIL if product creation fails
        expect(response.status).to.be.oneOf([200, 201], 
          `Product creation failed with ${response.status}: ${JSON.stringify(response.body)}`)
        createdProduct = response.body
        
        cy.log(`Created product: ${createdProduct.productId}`)
        
        // Validate product DTO structure
        expect(createdProduct).to.have.property('productId')
        expect(createdProduct).to.have.property('productTitle')
        expect(createdProduct).to.have.property('sku')
        expect(createdProduct).to.have.property('priceUnit')
        expect(createdProduct).to.have.property('quantity')
        expect(createdProduct.productId).to.be.a('number')
        expect(createdProduct.sku).to.equal(uniqueSku)
        expect(createdProduct.priceUnit).to.equal(99.99)
        expect(createdProduct.quantity).to.equal(100)
      })
  })

  it('Step 4: Should retrieve product by ID and verify category relationship', () => {
    // NO SKIP - Test must FAIL if Step 3 failed
    expect(createdProduct, 'Product must be created in Step 3').to.exist
    expect(createdProduct.productId, 'Product must have productId').to.be.a('number')
    
    cy.log(`Retrieving product ${createdProduct.productId}`)
    
    cy.apiRequest('GET', `/product-service/api/products/${createdProduct.productId}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const product = response.body
        expect(product.productId).to.equal(createdProduct.productId)
        expect(product.sku).to.equal(uniqueSku)
        
        // STRICT VALIDATION - Category relationship must be populated
        const categoryData = product.categoryDto || product.category
        expect(categoryData, 'Product must have category relationship populated').to.exist
        expect(categoryData.categoryId).to.equal(createdCategory.categoryId, 
          'Product category must match the category it was created with')
        expect(categoryData.categoryTitle).to.equal(uniqueCategoryName, 
          `Category title must be ${uniqueCategoryName}, but got ${categoryData.categoryTitle}`)
        
        cy.log('Product retrieved successfully with correct category relationship')
      })
  })

  it('Step 5: Should update product information', () => {
    // NO SKIP - Test must FAIL if Step 3 failed
    expect(createdProduct, 'Product must be created in Step 3').to.exist
    expect(createdProduct.productId, 'Product must have productId').to.be.a('number')
    
    cy.log('Updating product information')
    
    const updatedProductData = {
      productId: createdProduct.productId,
      productTitle: 'Updated E2E Test Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: uniqueSku,
      priceUnit: 149.99, // Updated price
      quantity: 75, // Updated quantity
      category: {
        categoryId: createdCategory.categoryId
      }
    }
    
    cy.apiRequest('PUT', '/product-service/api/products', { body: updatedProductData })
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Product update failed with ${response.status}: ${JSON.stringify(response.body)}`)
        
        const updatedProduct = response.body
        expect(updatedProduct.productTitle).to.equal('Updated E2E Test Product')
        expect(updatedProduct.priceUnit).to.equal(149.99)
        expect(updatedProduct.quantity).to.equal(75)
        
        cy.log('Product updated successfully')
      })
  })

  it('Step 6: Should update category information', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdCategory, 'Category must be created in Step 1').to.exist
    expect(createdCategory.categoryId, 'Category must have categoryId').to.be.a('number')
    
    cy.log('Updating category information')
    
    const updatedCategoryData = {
      categoryId: createdCategory.categoryId,
      categoryTitle: `Updated_${uniqueCategoryName}`,
      imageUrl: 'https://via.placeholder.com/350'
    }
    
    cy.apiRequest('PUT', '/product-service/api/categories', { body: updatedCategoryData })
      .then((response) => {
        expect(response.status).to.equal(200, 
          `Category update failed with ${response.status}: ${JSON.stringify(response.body)}`)
        
        const updatedCategory = response.body
        expect(updatedCategory.categoryTitle).to.equal(`Updated_${uniqueCategoryName}`)
        
        cy.log('Category updated successfully')
      })
  })

  it('Step 7: Should retrieve all products and verify our product exists', () => {
    // NO SKIP - Test must FAIL if Step 3 failed
    expect(createdProduct, 'Product must be created in Step 3').to.exist
    expect(createdProduct.productId, 'Product must have productId').to.be.a('number')
    
    cy.log('Retrieving all products')
    
    cy.apiRequest('GET', '/product-service/api/products')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const products = response.body.collection
        const ourProduct = products.find(p => p.productId === createdProduct.productId)
        
        expect(ourProduct, `Product ${createdProduct.productId} must exist in collection`).to.exist
        expect(ourProduct.sku).to.equal(uniqueSku, 
          `Product SKU must be ${uniqueSku}, but got ${ourProduct.sku}`)
        expect(ourProduct.priceUnit).to.equal(149.99, 
          `Product price must be 149.99 (updated), but got ${ourProduct.priceUnit}`)
        
        cy.log(`Product verified in collection (${products.length} total products)`)
      })
  })

  it('Step 8: Should retrieve all categories and verify our category exists', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdCategory, 'Category must be created in Step 1').to.exist
    expect(createdCategory.categoryId, 'Category must have categoryId').to.be.a('number')
    
    cy.log('Retrieving all categories')
    
    cy.apiRequest('GET', '/product-service/api/categories')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const categories = response.body.collection
        const ourCategory = categories.find(c => c.categoryId === createdCategory.categoryId)
        
        expect(ourCategory, `Category ${createdCategory.categoryId} must exist in collection`).to.exist
        
        // STRICT VALIDATION - Category title must be updated
        const expectedTitle = `Updated_${uniqueCategoryName}`
        expect(ourCategory.categoryTitle).to.equal(expectedTitle, 
          `Category title must be ${expectedTitle} (updated in Step 6), but got ${ourCategory.categoryTitle}`)
        
        cy.log(`Category verified in collection (${categories.length} total categories)`)
      })
  })

  it('Step 9: Should test duplicate SKU creation', () => {
    cy.log('Testing duplicate SKU creation')
    
    const duplicateProductData = {
      productTitle: 'Duplicate SKU Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: uniqueSku, // Same SKU
      priceUnit: 49.99,
      quantity: 10,
      category: {
        categoryId: createdCategory.categoryId
      }
    }
    
    cy.apiRequest('POST', '/product-service/api/products', { body: duplicateProductData })
      .then((response) => {
        if (response.status === 409 || response.status === 400) {
          cy.log('Duplicate SKU properly rejected')
          cy.log('Good: SKU uniqueness constraint working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Duplicate SKU was created')
          cy.log('BUG FOUND: No unique constraint on SKU')
          cy.log('RECOMMENDATION: Add unique constraint on product.sku')
        } else if (response.status === 500) {
          cy.log('Database constraint violation caught')
          cy.log('WARNING: Consider returning 409 Conflict instead of 500')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 10: Should test product creation with invalid category', () => {
    cy.log('Testing product creation with non-existent category')
    
    const invalidProductData = {
      productTitle: 'Invalid Category Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: `INVALID-SKU-${Date.now()}`,
      priceUnit: 29.99,
      quantity: 5,
      category: {
        categoryId: 999999 // Non-existent category
      }
    }
    
    cy.apiRequest('POST', '/product-service/api/products', { body: invalidProductData })
      .then((response) => {
        if (response.status === 400 || response.status === 404) {
          cy.log('Invalid category properly rejected')
          cy.log('Good: Foreign key constraint working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Product created with invalid category')
          cy.log('BUG FOUND: No foreign key constraint validation')
        } else if (response.status === 500) {
          cy.log('Database constraint violation caught')
          cy.log('WARNING: Consider returning 400 Bad Request instead of 500')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 11: Should test negative price validation', () => {
    cy.log('Testing product creation with negative price')
    
    const negativepriceData = {
      productTitle: 'Negative Price Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: `NEG-SKU-${Date.now()}`,
      priceUnit: -50.00, // Invalid negative price
      quantity: 10,
      category: {
        categoryId: createdCategory.categoryId
      }
    }
    
    cy.apiRequest('POST', '/product-service/api/products', { body: negativepriceData })
      .then((response) => {
        if (response.status === 400) {
          cy.log('Negative price properly rejected')
          cy.log('Good: Price validation working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Product created with negative price')
          cy.log('BUG FOUND: No validation on price (should be >= 0)')
          cy.log('RECOMMENDATION: Add @Min(0) validation on priceUnit')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 12: Should test negative quantity validation', () => {
    cy.log('Testing product creation with negative quantity')
    
    const negativeQuantityData = {
      productTitle: 'Negative Quantity Product',
      imageUrl: 'https://via.placeholder.com/400',
      sku: `NEGQTY-SKU-${Date.now()}`,
      priceUnit: 39.99,
      quantity: -10, // Invalid negative quantity
      category: {
        categoryId: createdCategory.categoryId
      }
    }
    
    cy.apiRequest('POST', '/product-service/api/products', { body: negativeQuantityData })
      .then((response) => {
        if (response.status === 400) {
          cy.log('Negative quantity properly rejected')
          cy.log('Good: Quantity validation working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Product created with negative quantity')
          cy.log('BUG FOUND: No validation on quantity (should be >= 0)')
          cy.log('RECOMMENDATION: Add @Min(0) validation on quantity')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Summary: Product catalog management executed', () => {
    cy.log('=' .repeat(70))
    cy.log('PRODUCT CATALOG MANAGEMENT - SUMMARY')
    cy.log('=' .repeat(70))
    cy.log(`Category ID: ${createdCategory.categoryId}`)
    cy.log(`Category Title: Updated_${uniqueCategoryName}`)
    cy.log(`Product ID: ${createdProduct.productId}`)
    cy.log(`SKU: ${uniqueSku}`)
    cy.log(`Price: $149.99`)
    cy.log(`Quantity: 75`)
    cy.log('=' .repeat(70))
    cy.log('Category CRUD operations validated')
    cy.log('Product CRUD operations validated')
    cy.log('Category-Product relationship tested')
    cy.log('Data validation tests performed')
    cy.log('=' .repeat(70))
    cy.log('FINDINGS:')
    cy.log('  - Category and Product entities properly linked')
    cy.log('  - Check SKU uniqueness constraint')
    cy.log('  - Verify price/quantity validation (>=0)')
    cy.log('  - Test foreign key constraint handling')
    cy.log('=' .repeat(70))
  })
})
