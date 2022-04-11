;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((java-mode . (
   (compile-command . "mvn -f ~/projects/gis/pom.xml clean package")
   (projectile-project-compilation-cmd . "mvn -f ~/projects/gis/pom.xml clean package")
   (projectile-project-run-cmd . "cd ~/projects/small-git-root-module/; java -jar ~/projects/gis/target/gis-1.0.0.jar ")
)))
