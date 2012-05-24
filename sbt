java -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512m -Xmx512M -Xss2M -jar `dirname $0`/sbt-launch.jar "$@"
