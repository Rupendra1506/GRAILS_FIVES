FROM tomcat:8.5-jdk8-corretto

ARG build_number

ENV BUILD_NUMBER=$build_number \
    ENVIRONMENT=test \
    DB_HOSTNAME=localhost \
    DB_USERNAME=root \
    DB_PASSWORD="" \
    DB_MAXACTIVE=100 \
    DB_MAXIDLE=100 \
    DB_MINIDLE=10 \
    DB_INITIALSIZE=10 \
    DB_MAXWAIT=10000 \
    JWT_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsTOxmXWiu3Ck/I7uO9AHePvdReMwXVccXmWsLXwYWGjl3Iuasak7g3K11VuQFB1D4zdUEY5EWyOKVMS8a08muSAx5BsiEixowDiP0r47kfLaFNnyjtRKoU84BBKeN2oBklE34dpqiXW6VtHyOuLuLCcHR7vkuqb/zqQoLshKMt2qYW78e4uv7SKWfQLz+1UyGAEwRCjyau43QIDsJb/vP1nfzQVFCzQshKkpLOJZknIRjJNN8OS9ovaAJt4RjbQt21ZZQiT94oQUmCPh74KLooW/QOBjZo2HoPUYQ+dMHP2+ADFKG2IpDPYOZJr/3TPbDz9gC4x4+A8P53ZDZ1zZ4QIDAQAB \
    REDIS_HOSTNAME=localhost \
    RABBIT_HOSTNAME=localhost \
    RABBIT_USERNAME=guest \
    RABBIT_PASSWORD=guest \
    LOG_LEVEL=ERROR \
    CATALINA_OPTS="-Dlocal.config.location=/usr/local/etc/app-config.yml -Dlogging.config=/usr/local/etc/logback.groovy"

# Install curl, bash, and supervisord for setup scripts
RUN yum update -y \
    && yum install -y \
        bash \
        gettext \
        hostname \
        iproute \
        net-tools

# Deploy tomcat webapp
RUN mkdir -p /usr/local/tomcat/webapps/
RUN rm -rf /usr/local/tomcat/webapps/*
WORKDIR /usr/local/tomcat/
COPY . .
COPY build/libs/ROOT.war /usr/local/tomcat/webapps/

# Deploy our specific container mods
COPY docker-app-config.yml /usr/local/etc/
COPY docker-logback.groovy /usr/local/etc/

COPY docker-startup.sh /root/

# Launch our 'init'
CMD ["/root/docker-startup.sh"]
