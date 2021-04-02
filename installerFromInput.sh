#!/bin/bash
#author: Christophe De Backer
#created: 30/03/2021

# Running this probably requires WiX install. This can be found online quite easily but you'll
# need to edit the path variables to be able to run those commands...

# Also, this cannot be run without a jdk >= 14

#enable --win-console to debug this if needed!
# --input folder_where_main_jar_and_other_resources_are_located
# --name name_of_Application -- choice!
# --win-dir-chooser -> let person installing choose where to install
# --win-shortcut -> add a shortcut of the install to the Desktop
# --main-jar -> name of the main jar to package, if input is specified this has to be at ./input/main_jar.jar
# --main-class -> main class to execute
# --icon -> relative path to the icon! (has to include --input for some reason)
# --module-path ---> paths to the jmods required for the build. call this however many times needed
# --add-modules --> add all the module names to include in the build here!
# --type app-image voor tests

# --license-file 'BuildMap\inputs\License\GNU AGPLv3.txt'
# --win-dir-chooser
# --win-shortcut
jpackage \
	--verbose \
	--win-dir-chooser \
	--win-shortcut \
	--vendor 'CKobalt, inc.' \
	--input 'BuildMap\inputs\' \
	--dest 'BuildMap\outputs\' \
	--license-file 'BuildMap\inputs\License\GNU AGPLv3.txt' \
	--name ARKServer \
	--main-jar Bridger.jar \
	--main-class application.Main \
	--icon 'BuildMap\inputs\Featherlight_Squared.ico' \
	--module-path 'C:\Program Files\Java\JavaFX\javafx-jmods-16' \
	--module-path 'C:\Users\Beheerder\Documents\ARKServer\BuildMap\modules\jsch.jmod' \
	--add-modules javafx.fxml,javafx.controls,jsch
