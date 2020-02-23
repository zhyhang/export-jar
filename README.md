Plugin of Intellij IDEA for quickly export java project's class, resource, source file to jar, like Eclipse's action of export to jar. 

## features
- Quick and Handy export
- Supports export java, class, resource file in java project classpath and their compiled output directories
- Supports single file or multi-files selection for export
- Supports different scopes export: class, package, module, project
- Supports cross modules export (no duplication selected)
- Supports export files in test directory
- Supports custom setting export file type
- Supports export from vcs commits (local changes) as well as intrinsic "create patch" action 

## runtime require
- Intellij Idea (U & C) 2017.3 and later

## usage
- select files
- right click mouse or click Build menu
- select "Export Jar..." to perform export

## plugin developing require
- when developing with 192+, please add following jars to SDK classpath:  
**installed root path/plugin/java/lib/\*.jar**  
**installed root path/lib/platform-core-ui.jar**  

## screenshot
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
- [OK]show successfully complete hint and link to export jar(no utility)
- [OK]select path textfield to list all selected history
- [OK]using sdk api to lookup nest class compiled files
- [OK]exclude test files (supports already)
- [OK]when export whole module, export files out scope in sources?
- [OK]add export action to Build menu
- [OK]write document
- [OK]register in Build Menu
- [OK]register key-map and shortcut (shortcut is OK)
- action can be perform from vcs menu as well as create patch
- action can be perform from local history dialog
- multi-language
- help docs
- throw swing context event exception when trigger by first-keystroke (key-map)
- button component mnemonic not working
## Note
- **导出时注意文件状态，删除，变换目录的等，注意FileStatus类**
- **com.intellij.ide.actions.RevealFileAction用于在系统上打开文件（show file in system explorer)**
- **DialogWrapper，ModalityState这个类来分析如果让对话框不关闭**