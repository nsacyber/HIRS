#!/bin/bash

# Enter package directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $SCRIPT_DIR

name="ACA_SwidTag"

tar -cf $name.tar build.gradle gradle* src/ docs/
gzip $name.tar
if [ -d rpmbuild ]; then
	rm -rf rpmbuild
fi
mkdir -p rpmbuild/BUILD rpmbuild/BUILDROOT rpmbuild/SOURCES rpmbuild/RPMS rpmbuild/SPECS rpmbuild/SRPMS
rpmbuild -bb $name.spec --define "_sourcedir $PWD" --define "_topdir $PWD/rpmbuild"

popd
