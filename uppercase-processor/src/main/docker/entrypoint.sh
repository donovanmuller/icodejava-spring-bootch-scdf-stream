#! /bin/sh
set -e

cat << EOF
--> Starting JVM with following arguments:

java
  ${javaagent}
  -Xms${VM_MIN_MEM:=64m}
  -Xmx${VM_MAX_MEM:=64m}
  -Djava.security.egd=file:/dev/./urandom
  -Duser.timezone=UTC+02:00
  -jar /app.jar
  "$@"

<--

EOF

java \
  ${javaagent} \
  -Xms${VM_MIN_MEM:=64m} \
  -Xmx${VM_MAX_MEM:=64m} \
  -Djava.security.egd=file:/dev/./urandom \
  -Duser.timezone=UTC+02:00 \
  -jar /app.jar \
  "$@"
