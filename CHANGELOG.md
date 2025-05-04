<!-- Keep a Changelog guide -> https://keepachangelog.com -->
<!-- Will refresh plugin.xml change-notes section by gradle's building -->

# Changelogs

## [Unreleased]
- Supports export pre-compiled class files that not  just in time compiling 
- Improve compile error message detail display
- Supports view include/exclude dir in pop dialog

## [2.5.4]
- Upgrade IntelliJ Platform Gradle Plugin to 2.5.0
- Fix export now working in IntelliJIdea 2025.1+
- Change certain actions' update and perform thread comply to ActionUpdateThread modal rule
- Fix other issues, e.g. export action exception not show message in message tool window

## [2.5.3]
- Refactor export jar from local changes action
- Export jar from local changes can include un-versioned files export
- Supports Kotlin source and class file export
- Remove not necessary dependency declare in plugin.xml for compatible with coming IDE 2024.2

## [2.5.2]
- Supports (recursively) include/exclude selection by directory as to can export the new files in select directories. 
- Refactor file list tree to customized version
- Refactor template storage model
- Fix some UE issue
- Update some plugin devkit version
- Introduce unit test framework

## [2.5.1]
- Make compatible with coming new IDEA 2024 version
- Upgrade develop tool Gradle IntelliJ Plugin to version 1.17.1
- Upgrade develop tool Gradle to version 8.6

## [2.5.0]
- Add template (select files, jar, options to template) management function.
- Redesign dialog layout.
- Remember dialog size last adjusted. 
- Remove the Created-By entry from MANIFEST.MF.
- Many refactors.

## [2.4.2]
- Make compatible with new version of Intellij IDE

## [2.4.1]
- Fix NullPointException when first use this plugin

## [2.4.0]
- Supports add directory entries option 
- Remember export options

## [2.3.1]
- Compatible with coming version 2022
- Build tool change to gradle

## [2.3.0]
- Fix throw exception when select empty files  
- Disable the OK button when select empty files  

## [2.2.0]
- List the select files in export setting dialog and can re-select them  
- Add copy to clipboard and show jar file in system explorer actions in notifications  
    
## [2.1.0] 
- fix NullPointException in 2021.1  
    
## [2.0.0]
- change open api usage to be compatible with 2020.3 versions  
    
## [1.5.1]
- change open api usage to be compatible with coming 2020 versions  
    
## [1.5.0]
- supports export jar from local changes like create patch  
- **add extent action in VCS menu, Version Control Local Changes pop menu and Commit Changes dialog commit button group<**  
- improve compatibility with new version IDEA  
- **NOTES: export local changes not include un-version-control changes**  

## [1.4.0]
- add module dependencies in plugin.xml conform to Jetbrains plugin rule.  

## [1.3.0]
- data collection announcement: <b>This plugin do not collect any personal data then transferring to outside.</b>  
- bug fix: throw null point exception when inputted jar file not including parent path (i.e. would export to current working directory)  
- ui experience improve: e.g. pop dialog can be resize  

## [1.2.0]
- switch json lib from jackson to gson (already included by platform dependencies)  
- list export jar history in combobox for select  

## [1.1.0]
- fix throw IllegalArgumentException in earlier than version 2018.3  
- extend support version scope: compatible with 2017.3 and later  
- support community edition  
- add jackson external dependency  
- ui little improvement  

## [1.0.0]
### Features
- Quick and Handy export  
- Supports export java, class, resource file in java project classpath and their compiled output directories  
- Supports single file or multi-files selection for export  
- Supports different scopes export: class, package, module, project  
- Supports cross modules export (no duplication selected)  
- Supports export files in test directory  
- Supports custom setting export file type  
