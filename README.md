# ch.hsr.ifs.cdttesting
Eclipse Plugin providing Junit4 testing classes that can be used to test Eclipse CDT C/C++ code based functionality. 
Extends CDT RTS testing with the functionality to 

* re-run a single (failing) test which was not possible previously, 
* automatically setup of the CDT index, 
* allow tests that include external include paths, 
* allow testing against other referenced projects and 
* allow testing of C (non-C++) projects. 

To use, add http://dev.ifs.hsr.ch/updatesites/cdttesting/kepler (Eclipse Juno CDT 8.2) to your target platform (.target file).
(the last info might needed to be adjusted)
