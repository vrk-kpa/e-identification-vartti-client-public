# Pull base image
FROM dev-docker-registry.kapa.ware.fi/e-identification-tomcat-base-image
COPY target/site /site

COPY conf/tomcat/vartti.xml /usr/share/tomcat/conf/Catalina/localhost/
COPY target/e-identification-vartti-client.war /opt/vartti-client/

# Templates
COPY conf/tomcat/vartti-client.properties.template /data00/templates/store/
COPY conf/tomcat/server.xml.template /data00/templates/store/
COPY conf/tomcat/setenv.sh.template /data00/templates/store/
COPY conf/tomcat/logback.xml.template /data00/templates/store/
COPY conf/ansible /data00/templates/store/ansible

WORKDIR /opt/vartti-client/
RUN mkdir -p /opt/vartti-client-properties && \
    mkdir -p /usr/share/tomcat/conf/ && \
    mkdir -p /usr/share/tomcat/properties && \

    ln -sf /data00/deploy/vartti-client.properties /opt/vartti-client-properties/vartti-client.properties && \
    ln -sf /data00/deploy/server.xml /usr/share/tomcat/conf/server.xml && \
    ln -sf /data00/deploy/setenv.sh /usr/share/tomcat/bin/setenv.sh && \
    ln -sf /data00/deploy/tomcat_keystore /usr/share/tomcat/properties/tomcat_keystore && \
    ln -sf /data00/deploy/kapa-ca /opt/kapa-ca && \

    chown -R tomcat:tomcat /usr/share/tomcat && \
    rm -fr /usr/share/tomcat/webapps/* && \
    rm -fr /usr/share/tomcat/server/webapps/* && \
    rm -fr /usr/share/tomcat/conf/Catalina/localhost/host-manager.xml && \
    rm -fr /usr/share/tomcat/conf/Catalina/localhost/manager.xml

CMD \
    mkdir -p /data00/logs && \
    chown -R tomcat:tomcat /data00/logs && \
    sudo -u tomcat /usr/share/tomcat/bin/catalina.sh run
