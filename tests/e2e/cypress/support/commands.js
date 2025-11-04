// ***********************************************
// Custom Cypress commands for E2E testing
// ***********************************************

/**
 * Generates a timestamp in the format: dd-MM-yyyy__HH:mm:ss:SSSSSS
 * This is the format required by the microservices
 */
const generateTimestamp = () => {
  const now = new Date()
  
  const pad = (num, size) => String(num).padStart(size, '0')
  
  const day = pad(now.getDate(), 2)
  const month = pad(now.getMonth() + 1, 2)
  const year = now.getFullYear()
  const hours = pad(now.getHours(), 2)
  const minutes = pad(now.getMinutes(), 2)
  const seconds = pad(now.getSeconds(), 2)
  const microseconds = pad(now.getMilliseconds() * 1000, 6)
  
  return `${day}-${month}-${year}__${hours}:${minutes}:${seconds}:${microseconds}`
}

/**
 * Generates a random email for testing
 */
const generateEmail = () => {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 10000)
  return `test.user.${timestamp}.${random}@example.com`
}

// Export utility functions to global scope
global.generateTimestamp = generateTimestamp
global.generateEmail = generateEmail

/**
 * API request wrapper with better error handling and timeout support
 */
Cypress.Commands.add('apiRequest', (method, url, options = {}) => {
  return cy.request({
    method,
    url,
    failOnStatusCode: false,
    timeout: options.timeout || 15000,
    ...options
  }).then((response) => {
    cy.log(`${method} ${url}`)
    cy.log(`Status: ${response.status}`)
    if (response.status >= 400) {
      cy.log(`Error: ${JSON.stringify(response.body)}`)
    }
    return cy.wrap(response)
  })
})

