# Microservices modifications

## Folder Structure and Docker files

- As the repository structure was messy at the beginning, containing workflows that will not be used in this repo, wrappers for the general application and documentation directly in the root folder, some structural changes were made:

1. Separate the microservices in a folder `services`, and remove the maven wrapper at the root
2. Move all the diagrams to the `docs` folder
3. Remove the separate `compose.yml` files for each microservice, as we are going to build them using the Dockerfiles 

- The repository `proyecto-final-ingesoft5-services/compose.yml` was updated to build each microservice from the local Dockerfiles located under `services/<service-name>/Dockerfile` instead of pulling images from Docker Hub.

**Note:** API gateway and User service were trying to copy from the root folder, so the copy steps were adapted so that it works from each microservice folder

- Quick run (from the `proyecto-final-ingesoft5-services` folder):

```powershell
docker compose up --build -d
docker compose logs -f
docker compose down
```

## Service Dependencies and Startup Order

The `compose.yml` has been configured with proper service dependencies using `depends_on` and `restart` policies to ensure services start in the correct order:

- The services Zipkin, Cloud Config Server and Service Discovery are independent from the other microservices, so they should start first and have a proper healthcheck that ensures that they are not only up, but receiving requests

- The api-gateway, favourite-service, order-service, payment-service, product-service, proxy-client, shipping-service and user-service should depend on the config server and service discovery, since they need to fetch their configuration and register in Eureka

### Springboot Actuator

To implement easily the healthcheck for the microservices, the actuator dependency was added in `pom.xml` for each microservice.