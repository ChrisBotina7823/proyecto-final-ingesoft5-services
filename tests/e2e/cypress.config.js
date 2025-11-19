const { defineConfig } = require('cypress')

module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    supportFile: 'cypress/support/e2e.js',
    specPattern: 'cypress/e2e/**/*.cy.js',
    setupNodeEvents(on, config) {
      // Allow overriding baseUrl via environment variable or CLI
      // Usage: npx cypress run --env baseUrl=http://other-host:port
      if (config.env.baseUrl) {
        config.baseUrl = config.env.baseUrl
      }
      return config
    },
    viewportWidth: 1280,
    viewportHeight: 720,
    video: true, // Enable video for CI
    screenshotOnRunFailure: true, // Enable screenshots on failure
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    retries: {
      runMode: 2, // Retry failed tests in CI
      openMode: 0
    },
    env: {
      apiUrl: 'http://localhost:8080'
    }
  },
  // Reporter configuration
  reporter: 'mochawesome',
  reporterOptions: {
    reportDir: 'cypress/reports',
    overwrite: false,
    html: true,
    json: true,
    timestamp: 'mmddyyyy_HHMMss',
    reportFilename: '[status]_[datetime]-report'
  }
})
