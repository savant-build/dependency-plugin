#!/bin/bash
cp build/jars/*.jar ~/dev/inversoft/repositories/savant/org/savantbuild/plugin/groovy/0.1.0
cp ~/dev/inversoft/repositories/savant/org/testng/testng/6.8.0/testng-6.8.0.jar.amd ~/dev/inversoft/repositories/savant/org/savantbuild/plugin/groovy/0.1.0/groovy-0.1.0.jar.amd
cd ~/dev/inversoft/repositories/savant/org/savantbuild/plugin/groovy/0.1.0
md5sum groovy-0.1.0.jar > groovy-0.1.0.jar.md5
md5sum groovy-0.1.0.jar.amd > groovy-0.1.0.jar.amd.md5
md5sum groovy-0.1.0-src.jar > groovy-0.1.0-src.jar.md5
