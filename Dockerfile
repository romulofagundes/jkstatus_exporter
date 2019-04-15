FROM openjdk:8-jre-alpine

COPY ./build/libs/jkstatus_exporter.jar /bin/jkstatus_exporter/
COPY config.yml /etc/jkstatus_exporter/

VOLUME ["/etc/jkstatus_exporter/"]

EXPOSE 9573

CMD [ "java","-jar","/bin/jkstatus_exporter/jkstatus_exporter.jar" ]