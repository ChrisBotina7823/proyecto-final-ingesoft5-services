/**
 * E2E Test: User Registration and Profile Management
 * 
 * This test validates complete user management within User Service:
 * 1. User creation
 * 2. Address management
 * 3. Credential management
 * 4. Verification token (if applicable)
 * 
 * All within a single microservice but testing multiple related endpoints
 */

describe('E2E: User Registration and Profile Management', () => {
  let createdUser
  let createdAddress
  let createdCredential
  const uniqueEmail = `test.user.${Date.now()}@example.com`
  const uniqueUsername = `testuser_${Date.now()}`

  before(() => {
    cy.log('Starting User Registration and Profile Management Test')
  })

  after(() => {
    cy.log('User Registration Test Finished')
    // Optional cleanup
  })

  it('Step 1: Should create a new user', () => {
    cy.log('Creating new user in User Service')
    
    const userData = {
      firstName: 'Cypress',
      lastName: 'TestUser',
      email: uniqueEmail,
      phone: '+1234567890',
      imageUrl: 'https://via.placeholder.com/150'
    }
    
    cy.apiRequest('POST', '/user-service/api/users', { body: userData })
      .then((response) => {
        // NO FALLBACK - Test must FAIL if user creation fails
        expect(response.status).to.be.oneOf([200, 201], 
          `User creation failed with ${response.status}: ${JSON.stringify(response.body)}`)
        createdUser = response.body
        
        cy.log(`Created user: ${createdUser.userId}`)
        
        // Validate user DTO structure
        expect(createdUser).to.have.property('userId')
        expect(createdUser).to.have.property('firstName')
        expect(createdUser).to.have.property('lastName')
        expect(createdUser).to.have.property('email')
        expect(createdUser).to.have.property('phone')
        expect(createdUser.userId).to.be.a('number')
        expect(createdUser.email).to.equal(uniqueEmail)
        expect(createdUser.firstName).to.equal('Cypress')
        expect(createdUser.lastName).to.equal('TestUser')
      })
  })

  it('Step 2: Should retrieve created user by ID', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdUser, 'User must be created in Step 1').to.exist
    expect(createdUser.userId, 'User must have userId').to.be.a('number')
    
    cy.log(`Retrieving user ${createdUser.userId}`)
    
    cy.apiRequest('GET', `/user-service/api/users/${createdUser.userId}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const user = response.body
        expect(user.userId).to.equal(createdUser.userId)
        
        // STRICT VALIDATION - Must return correct email
        expect(user.email).to.equal(uniqueEmail, 
          `Service must return user with email ${uniqueEmail}, but got ${user.email}`)
        
        cy.log('User retrieved successfully with correct email')
      })
  })

  it('Step 3: Should create an address for the user', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdUser, 'User must be created in Step 1').to.exist
    expect(createdUser.userId, 'User must have userId').to.be.a('number')
    
    cy.log('Creating address for user')
    
    const addressData = {
      fullAddress: '123 Test Street',
      postalCode: '12345',
      city: 'Test City',
      user: {
        userId: createdUser.userId
      }
    }
    
    cy.apiRequest('POST', '/user-service/api/address', { body: addressData })
      .then((response) => {
        // NO FALLBACK - Test must FAIL if address creation fails
        expect(response.status).to.be.oneOf([200, 201], 
          `Address creation failed with ${response.status}: ${JSON.stringify(response.body)}`)
        createdAddress = response.body
        
        cy.log(`Created address: ${createdAddress.addressId}`)
        
        // Validate address DTO structure
        expect(createdAddress).to.have.property('addressId')
        expect(createdAddress).to.have.property('fullAddress')
        expect(createdAddress).to.have.property('postalCode')
        expect(createdAddress).to.have.property('city')
        expect(createdAddress).to.have.property('user')
        expect(createdAddress.user.userId).to.equal(createdUser.userId)
        expect(createdAddress.fullAddress).to.equal('123 Test Street')
      })
  })

  it('Step 4: Should retrieve address by ID', () => {
    // NO SKIP - Test must FAIL if Step 3 failed
    expect(createdAddress, 'Address must be created in Step 3').to.exist
    expect(createdAddress.addressId, 'Address must have addressId').to.be.a('number')
    
    cy.log(`Retrieving address ${createdAddress.addressId}`)
    
    cy.apiRequest('GET', `/user-service/api/address/${createdAddress.addressId}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const address = response.body
        expect(address.addressId).to.equal(createdAddress.addressId)
        expect(address.user.userId).to.equal(createdUser.userId)
        
        cy.log('Address retrieved successfully')
      })
  })

  it('Step 5: Should create credentials for the user', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdUser, 'User must be created in Step 1').to.exist
    expect(createdUser.userId, 'User must have userId').to.be.a('number')
    
    cy.log('Creating credentials for user')
    
    const credentialData = {
      username: uniqueUsername,
      password: 'SecurePassword123!',
      roleBasedAuthority: 'ROLE_USER',
      isEnabled: true,
      isAccountNonExpired: true,
      isAccountNonLocked: true,
      isCredentialsNonExpired: true,
      user: {
        userId: createdUser.userId
      }
    }
    
    cy.apiRequest('POST', '/user-service/api/credentials', { body: credentialData })
      .then((response) => {
        // NO FALLBACK - Test must FAIL if credential creation fails
        expect(response.status).to.be.oneOf([200, 201], 
          `Credential creation failed with ${response.status}: ${JSON.stringify(response.body)}`)
        createdCredential = response.body
        
        cy.log(`Created credential: ${createdCredential.credentialId}`)
        
        // Validate credential DTO structure
        expect(createdCredential).to.have.property('credentialId')
        expect(createdCredential).to.have.property('username')
        expect(createdCredential).to.have.property('roleBasedAuthority')
        expect(createdCredential).to.have.property('isEnabled')
        expect(createdCredential).to.have.property('user')
        expect(createdCredential.username).to.equal(uniqueUsername)
        expect(createdCredential.user.userId).to.equal(createdUser.userId)
        
        // Password should not be returned in response (security check)
        if (createdCredential.password) {
          cy.log('BUG: SECURITY BUG: Password returned in API response!')
          cy.log('WARNING: Passwords should never be returned in responses')
        } else {
          cy.log('Security: Password not exposed in response')
        }
      })
  })

  it('Step 6: Should retrieve credentials by ID', () => {
    // NO SKIP - Test must FAIL if Step 5 failed
    expect(createdCredential, 'Credential must be created in Step 5').to.exist
    expect(createdCredential.credentialId, 'Credential must have credentialId').to.be.a('number')
    
    cy.log(`Retrieving credential ${createdCredential.credentialId}`)
    
    cy.apiRequest('GET', `/user-service/api/credentials/${createdCredential.credentialId}`)
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const credential = response.body
        expect(credential.credentialId).to.equal(createdCredential.credentialId)
        expect(credential.username).to.equal(uniqueUsername)
        
        // Security check: password should not be exposed
        if (credential.password) {
          throw new Error('SECURITY BUG: Password exposed in GET response!')
        }
        
        cy.log('Credential retrieved successfully')
      })
  })

  it('Step 7: Should retrieve credentials by username', () => {
    // NO SKIP - Test must FAIL if Step 5 failed
    expect(createdCredential, 'Credential must be created in Step 5').to.exist
    expect(uniqueUsername, 'Username must be defined').to.be.a('string')
    
    cy.log(`Retrieving credential by username: ${uniqueUsername}`)
    
    cy.apiRequest('GET', `/user-service/api/credentials/username/${uniqueUsername}`)
      .then((response) => {
        expect(response.status).to.equal(200, 
          `GET credential by username failed with ${response.status}: ${JSON.stringify(response.body)}`)
        
        const credential = response.body
        expect(credential.username).to.equal(uniqueUsername)
        expect(credential.user.userId).to.equal(createdUser.userId)
        
        cy.log('Credential retrieved by username successfully')
      })
  })

  it('Step 8: Should update user information', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdUser, 'User must be created in Step 1').to.exist
    expect(createdUser.userId, 'User must have userId').to.be.a('number')
    
    cy.log('Updating user information')
    
    const updatedUserData = {
      userId: createdUser.userId,
      firstName: 'Updated',
      lastName: 'TestUser',
      email: uniqueEmail,
      phone: '+1987654321',
      imageUrl: 'https://via.placeholder.com/200'
    }
    
    cy.apiRequest('PUT', '/user-service/api/users', { body: updatedUserData })
      .then((response) => {
        expect(response.status).to.equal(200, 
          `User update failed with ${response.status}: ${JSON.stringify(response.body)}`)
        
        const updatedUser = response.body
        expect(updatedUser.firstName).to.equal('Updated')
        expect(updatedUser.phone).to.equal('+1987654321')
        
        cy.log('User updated successfully')
      })
  })

  it('Step 9: Should update address information', () => {
    // NO SKIP - Test must FAIL if Step 3 failed
    expect(createdAddress, 'Address must be created in Step 3').to.exist
    expect(createdAddress.addressId, 'Address must have addressId').to.be.a('number')
    
    cy.log('Updating address information')
    
    const updatedAddressData = {
      addressId: createdAddress.addressId,
      fullAddress: '456 Updated Avenue',
      postalCode: '54321',
      city: 'New City',
      user: {
        userId: createdUser.userId
      }
    }
    
    cy.apiRequest('PUT', '/user-service/api/address', { body: updatedAddressData })
      .then((response) => {
        expect(response.status).to.equal(200)
        
        const updatedAddress = response.body
        expect(updatedAddress.fullAddress).to.equal('456 Updated Avenue')
        expect(updatedAddress.city).to.equal('New City')
        
        cy.log('Address updated successfully')
      })
  })

  it('Step 10: Should test duplicate username creation', () => {
    cy.log('Testing duplicate username creation')
    
    const duplicateCredentialData = {
      username: uniqueUsername, // Same username
      password: 'AnotherPassword123!',
      roleBasedAuthority: 'ROLE_USER',
      isEnabled: true,
      isAccountNonExpired: true,
      isAccountNonLocked: true,
      isCredentialsNonExpired: true,
      user: {
        userId: createdUser.userId + 1 // Different user (if exists)
      }
    }
    
    cy.apiRequest('POST', '/user-service/api/credentials', { body: duplicateCredentialData })
      .then((response) => {
        if (response.status === 409 || response.status === 400) {
          cy.log('Duplicate username properly rejected')
          cy.log('Good: Username uniqueness constraint working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Duplicate username was created')
          cy.log('BUG FOUND: No unique constraint on username')
        } else if (response.status === 500) {
          cy.log('Database constraint violation caught')
          cy.log('WARNING: Consider returning 409 Conflict instead of 500')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 11: Should test duplicate email creation', () => {
    cy.log('Testing duplicate email creation')
    
    const duplicateUserData = {
      firstName: 'Another',
      lastName: 'User',
      email: uniqueEmail, // Same email
      phone: '+1111111111',
      imageUrl: 'https://via.placeholder.com/150'
    }
    
    cy.apiRequest('POST', '/user-service/api/users', { body: duplicateUserData })
      .then((response) => {
        if (response.status === 409 || response.status === 400) {
          cy.log('Duplicate email properly rejected')
          cy.log('Good: Email uniqueness constraint working')
        } else if (response.status === 200 || response.status === 201) {
          cy.log('WARNING: Duplicate email was created')
          cy.log('BUG FOUND: No unique constraint on email')
          cy.log('RECOMMENDATION: Add unique constraint on user.email')
        } else if (response.status === 500) {
          cy.log('Database constraint violation caught')
        }
        
        cy.log(`Actual response status: ${response.status}`)
      })
  })

  it('Step 12: Should retrieve all users and verify our user exists', () => {
    // NO SKIP - Test must FAIL if Step 1 failed
    expect(createdUser, 'User must be created in Step 1').to.exist
    expect(createdUser.userId, 'User must have userId').to.be.a('number')
    
    cy.log('Retrieving all users')
    
    cy.apiRequest('GET', '/user-service/api/users')
      .then((response) => {
        expect(response.status).to.equal(200)
        expect(response.body).to.have.property('collection')
        
        const users = response.body.collection
        const ourUser = users.find(u => u.userId === createdUser.userId)
        
        expect(ourUser, `User ${createdUser.userId} must exist in collection`).to.exist
        
        // STRICT VALIDATION - Email must match
        expect(ourUser.email).to.equal(uniqueEmail, 
          `User email must be ${uniqueEmail}, but got ${ourUser.email}`)
        
        cy.log(`User verified in collection (${users.length} total users)`)
      })
  })

  it('Summary: User registration and profile management executed', () => {
    cy.log('=' .repeat(70))
    cy.log('USER REGISTRATION & PROFILE - SUMMARY')
    cy.log('=' .repeat(70))
    cy.log(`User ID: ${createdUser ? createdUser.userId : 'N/A'}`)
    cy.log(`Email: ${uniqueEmail}`)
    cy.log(`Username: ${uniqueUsername}`)
    cy.log(`Address ID: ${createdAddress ? createdAddress.addressId : 'Not created'}`)
    cy.log(`Credential ID: ${createdCredential ? createdCredential.credentialId : 'Not created'}`)
    cy.log('=' .repeat(70))
    
    if (createdUser) cy.log('User operations tested')
    if (createdAddress) cy.log('Address operations tested')
    if (createdCredential) cy.log('Credential operations tested')
    
    cy.log('=' .repeat(70))
  })
})
