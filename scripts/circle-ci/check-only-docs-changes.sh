#!/bin/bash
# Returns true if only the docs have been modified

set -x

DOCS_REPO="docs/"

FILES_CHANGED=$(git diff --name-only origin/develop...HEAD)

for file in $FILES_CHANGED
do
    if [[ $file != ${DOCS_REPO}* ]]; then
        exit 1;
    fi
done

cat <<EOF
======================================================
We detected only docs changes, take appropriate action
======================================================
EOF

exit 0