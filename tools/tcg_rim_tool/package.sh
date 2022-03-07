#!/bin/bash
VERSION=2.1.0
GIT_HASH=`git rev-parse HEAD | head -c6`
GIT_COMMIT_UNIX_TIMESTAMP=`git show -s --format=%ct | xargs echo -n`
RELEASE="$((GIT_COMMIT_UNIX_TIMESTAMP)).$GIT_HASH"
name="tcg_rim_tool"

# Enter package directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $SCRIPT_DIR

tar -cf "$name-$VERSION.$RELEASE".tar build.gradle gradle* src/ docs/ rim_fields.json keystore.jks scripts/
gzip -f "$name-$VERSION.$RELEASE".tar
if [ -d rpmbuild ]; then
	rm -rf rpmbuild
fi
mkdir -p rpmbuild/BUILD rpmbuild/BUILDROOT rpmbuild/SOURCES rpmbuild/RPMS rpmbuild/SPECS rpmbuild/SRPMS
rpmbuild -bb $name.spec --define "_sourcedir $PWD" --define "_topdir $PWD/rpmbuild" --define 'RELEASE '$RELEASE --define 'VERSION '$VERSION

popd
