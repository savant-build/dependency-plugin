#!/bin/bash
mkdir -p ~/.savant/cache/org/savantbuild/plugin/dependency/0.1.0-\{integration\}/
cp build/jars/*.jar ~/.savant/cache/org/savantbuild/plugin/dependency/0.1.0-\{integration\}/
cp src/main/resources/amd.xml ~/.savant/cache/org/savantbuild/plugin/dependency/0.1.0-\{integration\}/dependency-0.1.0-\{integration\}.jar.amd
cd ~/.savant/cache/org/savantbuild/plugin/dependency/0.1.0-\{integration\}/
md5sum dependency-0.1.0-\{integration\}.jar > dependency-0.1.0-\{integration\}.jar.md5
md5sum dependency-0.1.0-\{integration\}.jar.amd > dependency-0.1.0-\{integration\}.jar.amd.md5
md5sum dependency-0.1.0-\{integration\}-src.jar > dependency-0.1.0-\{integration\}-src.jar.md5
