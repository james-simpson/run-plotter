## Stage 1: download packages and build the jar file & javascript
FROM theasp/clojurescript-nodejs:alpine AS build

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN npm install
RUN npx shadow-cljs release app
RUN lein deps
RUN lein uberjar

## Stage 2: run the app on a slimmer image
FROM openjdk:11-jre-slim

COPY --from=build /usr/src/app/target/*-standalone.jar /service.jar
COPY --from=build /usr/src/app/resources ./resources

EXPOSE 3000
CMD ["java", "-jar", "/service.jar"]