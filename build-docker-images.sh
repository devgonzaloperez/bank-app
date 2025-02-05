#!/bin/bash

#Define the root directory as the location of the script itself.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

#Navigate to the root directory (where the script is located).
cd "$ROOT_DIR" || exit

#Loop through each directory.
for dir in */ ; do
  #Skip the .vscode and docker-compose directories.
  if [ "$dir" == ".vscode/" ] || [ "$dir" == "docker-compose/" ]; then
    echo "Skipping $dir"
    continue
  fi

  if [ -d "$dir" ]; then
    echo "Building Docker image for $dir"
    cd "$dir" || exit
    mvn compile jib:dockerBuild
    if [ $? -ne 0 ]; then
      echo "Failed to build Docker image for $dir"
      exit 1
    fi
    cd ..
  fi
done

echo "Docker images built successfully for all microservices."

#Give execution permits to the script with chmod +x build-docker-images.sh.
#Execute with ./build-docker-images.sh.