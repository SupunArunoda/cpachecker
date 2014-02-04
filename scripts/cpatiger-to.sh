#!/bin/bash

### This version of cpatiger.sh that uses the "timeout" command to gently kill the process

# the location of the java command
[ -z "$JAVA" ] && JAVA=java

# the default heap size of the javaVM
DEFAULT_HEAP_SIZE="1200m"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

java_version="`$JAVA -Xmx5m -version 2>&1`"
result=$?
if [ $result -eq 127 ]; then
  echo "Java not found, please install Java 1.7 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-7-jre" 1>&2
  echo "If you have installed Java 7, but it is not in your PATH," 1>&2
  echo "let the environment variable JAVA point to the \"java\" binary." 1>&2
  exit 1
fi
if [ $result -ne 0 ]; then
  echo "Failed to execute Java VM, return code was $result and output was"
  echo "$java_version"
  echo "Please make sure you are able to execute Java processes by running \"$JAVA\"."
  exit 1
fi
java_version="`echo "$java_version" | grep "^java version" | cut -f2 -d\\\" | sed 's/\.//g' | cut -b1-2`"
if [ -z "$java_version" ] || [ "$java_version" -lt 17 ] ; then
  echo "Your Java version is too old, please install Java 1.7 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-7-jre" 1>&2
  echo "If you have installed Java 7, but it is not in your PATH," 1>&2
  echo "let the environment variable JAVA point to the \"java\" binary." 1>&2
  exit 1
fi

platform="`uname -s`"

# where the project directory is, relative to the location of this script
case "$platform" in
  Linux)
    SCRIPT="$(readlink -f "$0")"
    [ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="$(readlink -f "$(dirname "$SCRIPT")/..")"
    ;;
  # other platforms like Mac don't support readlink -f
  *)
    [ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="$(dirname "$0")/.."
    ;;
esac

if [ ! -e "$PATH_TO_CPACHECKER/bin/org/sosy_lab/cpachecker/cmdline/CPATigerMain.class" ] ; then
  if [ ! -e "$PATH_TO_CPACHECKER/cpachecker.jar" ] ; then
    echo "Could not find CPAtiger binary, please check path to project directory" 1>&2
    exit 1
  fi
fi

export CLASSPATH="$CLASSPATH:$PATH_TO_CPACHECKER/bin:$PATH_TO_CPACHECKER/cpachecker.jar:$PATH_TO_CPACHECKER/lib/*:$PATH_TO_CPACHECKER/lib/java/runtime/*:$PATH_TO_CPACHECKER/lib/JavaParser/*"

# loop over all input parameters and parse them
declare -a OPTIONS
JAVA_ASSERTIONS=-ea
while [ $# -gt 0 ]; do

  case $1 in
   "-forceto")
       shift
       TO=$1
       ;;
   "-heap")
       shift
       JAVA_HEAP_SIZE=$1
       ;;
   "-debug")
       JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
       ;;
   "-disable-java-assertions")
       JAVA_ASSERTIONS=-da
       ;;
   *) # other params are only for CPAchecker
       OPTIONS+=("$1")
       ;;
  esac

  shift
done

TOPREF=''
if [ -n "$TO" ]; then
  echo "Run timeout after $TO seconds"
  TOPREF="timeout $TO "
fi

if [ -n "$JAVA_HEAP_SIZE" ]; then
  echo "Running JavaVM with special heap size: $JAVA_HEAP_SIZE"
fi

if [ ! -z "$JAVA_VM_ARGUMENTS" ]; then
  echo "Running CPAtiger with the following extra VM options: $JAVA_VM_ARGUMENTS"
fi

if [ ! -z "$CPACHECKER_ARGUMENTS" ]; then
  echo "Running CPAtiger with the following extra arguments: $CPACHECKER_ARGUMENTS"
fi

# run CPAchecker
exec $TOPREF "$JAVA" $JAVA_VM_ARGUMENTS -Xmx${JAVA_HEAP_SIZE:-$DEFAULT_HEAP_SIZE} $JAVA_ASSERTIONS org.sosy_lab.cpachecker.cmdline.CPATigerMain "${OPTIONS[@]}" $CPACHECKER_ARGUMENTS
