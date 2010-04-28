#! /bin/bash

# this script will update the android project files with the appropriate system
# paths so that you can build the BHHome app using `ant debug'
#
# you should run this script from BHHome/

android update project  \
--target 3              \
--path ./android        \
--name BHHome
