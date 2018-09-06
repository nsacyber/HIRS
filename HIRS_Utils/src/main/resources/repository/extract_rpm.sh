#!/usr/bin/env bash

rpm2cpio $RPM_FILEPATH | cpio -id; chmod -R u+r .