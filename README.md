# ch.hsr.ifs.cdttesting
Eclipse Plugin providing Junit4 testing classes that can be used to test Eclipse CDT C/C++ code based functionality. 
Extends CDT RTS testing with the functionality to 

* re-run a single (failing) test which was not possible previously, 
* automatically setup of the CDT index, 
* allow tests that include external include paths, 
* allow testing against other referenced projects and 
* allow testing of C (non-C++) projects. 

To use, add https://www.cevelop.com/cdt-testing/mars/ (Eclipse CDT 8.8) to your target platform (.target file).
