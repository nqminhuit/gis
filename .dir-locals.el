;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((nil . (
   (compile-command . "mvn -f /opt/minh/tools/gis/pom.xml clean package")
   (projectile-project-compilation-cmd . "mvn -f /opt/minh/tools/gis/pom.xml clean package")
   (projectile-project-run-cmd . "cd ~/projects/ocm-system/; java -jar /opt/minh/tools/gis/target/gis-1.1.2.jar ")
)))
