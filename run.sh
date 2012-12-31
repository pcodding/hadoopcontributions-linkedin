#!/bin/bash
cd target
java -Xmx1024m -jar employer-resolver.jar "$@" 