FROM eclipse-temurin:21-jre-alpine

ARG USER=app
ENV HOME=/home/$USER

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN adduser -D $USER

USER $USER
WORKDIR $HOME

COPY --chown=$USER:$USER /target/app.jar app.jar

CMD ["sh", "-c", "java -jar $JAVA_OPTS app.jar"]
