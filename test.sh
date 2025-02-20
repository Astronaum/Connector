#!/bin/bash

USER_FILE="users.txt"

# Ensure the user file exists
touch "$USER_FILE"

operation="$1"

case "$operation" in
  "createUser")
    username="$2"
    email="$3"
    firstName="$4"
    lastName="$5"

    if grep -q "^$username$" "$USER_FILE"; then
      echo "User $username already exists"
      exit 1
    fi

    echo "$username" >> "$USER_FILE"
    echo "User created successfully"
    ;;

  "deleteUser")
    username="$2"

    if ! grep -q "^$username$" "$USER_FILE"; then
      echo "User $username not found"
      exit 1
    fi

    grep -v "^$username$" "$USER_FILE" > temp.txt && mv temp.txt "$USER_FILE"
    echo "User deleted successfully"
    ;;

  "getUsers")
    cat "$USER_FILE"
    ;;

  *)
    echo "Unknown operation: $operation"
    exit 1
    ;;
esac
