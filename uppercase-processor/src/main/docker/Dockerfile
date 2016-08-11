FROM java:8

VOLUME /tmp
ADD entrypoint.sh /entrypoint.sh
RUN bash -c 'chmod a+rx /entrypoint.sh'

ENTRYPOINT ["/entrypoint.sh"]

RUN wget --quiet --user ${repo_auth_username} \
  --password ${repo_auth_password} \
  --content-disposition \
  -O /app.jar \
  "http://nexus.vagrant.f8/service/local/artifact/maven/redirect?r=public&g=${app_groupId}&a=${app_artifactId}&v=${app_version}"

RUN bash -c 'chmod a+rx /app.jar'
