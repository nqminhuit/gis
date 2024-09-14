;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((nil . (
   (compile-command . "mvn -f /opt/minh/tools/gis/pom.xml -T 1C clean package -DskipTests; cd /home/minh/projects/test/small-git-root-module; java -jar /opt/minh/tools/gis/target/gis-2.0.0-dev.jar ")
   (projectile-project-compilation-cmd . "mvn -T 1C -DskipTests clean package")
   (projectile-project-run-cmd . "cd /home/minh/projects/test/small-git-root-module; java -jar /opt/minh/tools/gis/target/gis-2.0.0-dev.jar ")
)))
