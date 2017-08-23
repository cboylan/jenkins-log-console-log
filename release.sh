#!/bin/sh
rm -fv release.properties pom.xml.releaseBackup
exec mvn $(test "$DEBUG" && echo '-X') release:prepare release:perform
