BASE_DIR=$(pwd)

set -e

mvn clean && \
mvn -e install -Dcheckstyle.skip -Dmaven.test.skip=true
