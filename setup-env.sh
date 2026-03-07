#!/bin/bash
# setup-env.sh: Configures Java 21 and Maven for the current terminal session.

export JAVA_HOME="/c/Program Files/Java/jdk-21"
export MAVEN_HOME="/c/Program Files/apache-maven-3.9.13"

# Add Java and Maven to the PATH
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

# Load environment variables from .env if it exists
if [ -f .env ]; then
  # Export all variables that don't start with a comment (#)
  export $(grep -v '^#' .env | xargs)
  echo "Loaded variables from .env"
fi

echo "----------------------------------------------------"
echo "Environment configured for AI Donor Matcher Backend"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Maven Version: $(mvn -v | head -n 1)"
echo "----------------------------------------------------"
