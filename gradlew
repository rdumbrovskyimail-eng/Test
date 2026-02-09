#!/bin/bash

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
# Gradle start up script for UN*X
##############################################################################

DIRNAME=`dirname "$0"`
cd "$DIRNAME" || exit

APP_NAME="gradlew"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# Increase the default file descriptor limit if possible.
if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD_LIMIT" != "unlimited" ] ; then
            ulimit -n $MAX_FD_LIMIT
        fi
    else
        warn "Could not query maximum file descriptor limit"
    fi
fi

# For Darwin, add options to specify how the Gradle app appears in the dock.
if $darwin; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_DIR/media/gradle.icns\""
fi

# Start the Gradle daemon
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"