#!/bin/bash
# Script to change SonarQube admin password on first run

SONAR_URL="http://sonarqube:9000"
OLD_PASSWORD="admin"
NEW_PASSWORD="${SONAR_ADMIN_PASSWORD:-admin}"

echo "Waiting for SonarQube to be ready..."
until curl -s -f -u admin:admin "${SONAR_URL}/api/system/status" | grep -q '"status":"UP"'; do
  echo "SonarQube not ready yet, waiting 10 seconds..."
  sleep 10
done

echo "SonarQube is UP!"

# Check if password needs to be changed (if default password still works)
if curl -s -f -u admin:${OLD_PASSWORD} "${SONAR_URL}/api/authentication/validate" | grep -q '"valid":true'; then
  echo "Default password detected, changing to new password..."
  
  # Change password using the API
  curl -X POST -u admin:${OLD_PASSWORD} \
    "${SONAR_URL}/api/users/change_password" \
    -d "login=admin" \
    -d "previousPassword=${OLD_PASSWORD}" \
    -d "password=${NEW_PASSWORD}"
  
  echo ""
  echo "Password changed successfully!"
else
  echo "Password already changed or SonarQube already configured."
fi

echo ""
echo "SonarQube initialization complete!"
