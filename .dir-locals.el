;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((nil . (
   (compile-command . "mvn -f ~/projects/gis/pom.xml -T 1C clean package")
   (projectile-project-compilation-cmd . "mvn -T 1C clean package")
   (projectile-project-run-cmd . "cd /home/minh/projects/test/small-git-root-module; java -jar ~/projects/gis/target/gis-1.1.4-dev.jar ")
)))
