Plugin of Intellij IDEA for export java project's class, resource, source file to jar, like Eclipse's action of export to jar.

## features
- Supports export java, class, resource file in java project classpath and their compiled output directories
- Supports single file or multi-files selection for export
- Supports different scopes export: class, package, module, project
- Supports cross modules export (no duplications selected)
- Supports export files in test directory
- Supports custom setting export file type

## require
- Intellij Idea 2018.2 and later

## usage
- select files
- right click mouse or click Build menu
- select "Export Jar Select Files..." to perform export

##screenshot
![From Build Menu](https://raw.githubusercontent.com/zhyhang/export-jar/master/image/export-jar-menu.png)
![From Right Click](https://raw.githubusercontent.com/zhyhang/export-jar/master/image/export-jar-right-click.png)
![Setting Dialog](https://raw.githubusercontent.com/zhyhang/export-jar/master/image/export-jar-pop.png)
![Export Status](https://raw.githubusercontent.com/zhyhang/export-jar/master/image/export-jar-result.png)

## TODO 
- [OK]support inner and anonymous class export
- [OK]support large batch classes  export
- [OK]support multi module export, exit when encounter duplicate files
- [OK]fix the issue: export all when select resource folder
- [OK]support messages log levels
- [OK]prompt when exporting jar exists
- [OK]show successfully complete hint
- show sucessfully complete hint and link to export jar
- select path textfield to list all selected history
- [OK]using sdk api to lookup nest class compiled files
- [OK]exclude test files (supports already)
- [OK]when export whole module, export files out scope in sources?
- add export action to Build menu
- write document
- register in Build Menu
- register key-map and short-cut 