#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# ...
#

exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
