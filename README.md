# package.sh 
mvn clean && \
mvn -e install -Dcheckstyle.skip -Dmaven.test.skip=true
