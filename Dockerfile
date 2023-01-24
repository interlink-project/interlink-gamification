# syntax=docker/dockerfile:experimental
FROM maven:3-openjdk-11 as mvn
COPY ./ /tmp/interlink-gamification
WORKDIR /tmp/interlink-gamification
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-alpine
ARG VER=3.0.0
ARG USER=interlink-gamification
ARG USER_ID=1005
ARG USER_GROUP=interlink-gamification
ARG USER_GROUP_ID=1005
ARG USER_HOME=/home/${USER}
ENV FOLDER=/tmp/target
ENV LOG_FOLDER=/home/${USER}/logs
ENV APP=gamification-0.1.0-SNAPSHOT
ENV VER=${VER}
# create a user group and a user
RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
     adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

WORKDIR ${USER_HOME}
COPY --chown=interlink-gamification:interlink-gamification --from=mvn /tmp/interlink-gamification/target/${APP}.jar ${USER_HOME}
COPY --chown=interlink-gamification:interlink-gamification --from=mvn /tmp/interlink-gamification/game_1.json ${USER_HOME}
USER interlink-gamification
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -DlogFolder=${LOG_FOLDER} -jar ${APP}.jar --spring.profiles.active=sec"]
