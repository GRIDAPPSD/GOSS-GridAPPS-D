The Open Distribution System Simulator, OpenDSS

Copyright (c) 2008-2020, Electric Power Research Institute, Inc.
Copyright (c) 2017-2020, Battelle Memorial Institute
All rights reserved.

opendsscmd version 1.2.11
=========================

This is a 64-bit command-line version of the simulator for Windows, Linux and Mac OSX operating systems. It is model-compatible with version 7.6.5 of the Windows-only version, and source code revision r2181. The major differences between opendsscmd and Windows-only OpenDSS are:

1 - There is no support for Windows COM automation, and no separate DLL version. (Use FNCS or HELICS instead)

2 - There is no graphical user interface (GUI) or plotting. (Use MATLAB or Python instead)

3 - Automation is provided through the Framework for Network Cosimulation (FNCS) library developed by Pacific Northwest National Laboratory (PNNL). It is  planned to upgrade FNCS with the HELIC framework that is currently under development by several US Department of Energy labs under the Grid Modernization Laboratory Consortium (GMLC) project 1.4.15. Both FNCS and HELICS must be obtained separately.

Change Log
==========

1.0.1 - CIM export of Relay, Fuse, Breaker based on controls attached to Lines having switch=yes


1.1.0 - CIM100 support, time-stepping under control of FNCS


1.2.0 - FNCS output publications

      - operational limits included in CIM100 export

      - ExpControl and VCCS enhancements from IEEE PVSC 46 papers


1.2.1 - removed FNCS debug output

      - added test.json sample FNCS messaging config file
1.2.2 - changed CIM100 mRID from GUID to UUID v4; see RFC 4122

1.2.3 - added "-v" flag and "about" command for version number


1.2.4 - fixed FNCS time synchronization for power rationing example
      
      - implemented FNCS log levels (opendsscmd --help for details)


1.2.5 - include Buses in "export UUIDS" and "UUID" commands


1.2.6 - persist all CIM mRID values in the "uuid" and "export uuid" commands


1.2.7 - bugfix for persistent mRID values on CIM-created XfmrCodes

1.2.8 - performance tuning in the FNCS interface
1.2.9 - added the option to export CIM100 in six separate sub-profiles
1.2.10 - trap UUID for a missing bus
1.2.11 - fix TransformerCoreAdmittance, TransformerEnd and some indentation
       - fix undervoltage relay property values

Quick Start

===========



If you're unfamiliar with OpenDSS, see install_dir/doc/OpenDSSPrimer.pdf and install_dir/doc/OpenDSSManual.pdf to learn about its modeling and analysis features.  However, none of the COM automation or plotting features are supported in opendsscmd. To run any of the non-graphical commands:

1.  Enter "opendsscmd" from a command prompt
    a. The program's >> prompt will appear. Enter any OpenDSS command(s) from this prompt
    b. Up and down arrows navigate through the command history
    c. Enter "help" from the >> prompt for the built-in help
    d. Enter "exit", "q" or an empty line from >> to exit
2. You can enter "opendsscmd filename.dss" from a command prompt. This runs the OpenDSS commands in filename.dss, and then exits immediately.
3. You can enter "opendsscmd –f" from a command prompt; this enters a FNCS time step loop.
4. You can enter "opendsscmd –f filename.dss" from a command prompt. This runs the OpenDSS commands in filename.dss, and then enters a FNCS time step loop.

To verify proper installation:

1. From install_dir/test, invoke "opendsscmd", and then "redirect IEEE13Nodeckt.dss". A list of solved node voltages should appear in your system's default test editor. Enter "quit" to leave opendsscmd
2. From install_dir/test, invoke "opendsscmd export_test.dss". This should create a Common Information Model (CIM) export of the IEEE 13-bus feeder.
3. If you have FNCS installed, from install_dir/test invoke "test_fncs.bat" (on Windows) or "./test_fncs.sh" (on Linux or Mac OSX). This will play some basic commands to opendsscmd over FNCS on port 5570, and then exit. If something goes wrong here:
   a. To list processes using port 5570, use "list5570.bat" on Windows or "lsof -i tcp:5570" on Linux/Mac
   b. To kill all processes using port 5570, use "kill5570.bat" on Windows or "kill5570.sh" on Linux/Mac


Open Issues

===========


1. The regular expressions for the batchedit command, which are implemented in ExecHelper.pas, have become case-sensitive.  They need to be made case-insensitive.
2. On Windows, the command history editor is "sluggish". You have to type slowly.

Installation
============



On all platforms, the documentation and sample files will be copied to a user-specified installation directory, called install_dir. An uninstall script is also provided.

On Linux and Mac OSX, the executables will be copied to /usr/local/bin

On Linux and Mac OSX, dynamic libraries will be copied to /usr/local/lib

On Windows, the executables and DLLs will be copied to install_dir. Also, install_dir will be appended to the Windows path.

Source Code
===========

OpenDSS source code is available from the following SVN repository: 

http://svn.code.sf.net/p/electricdss/code/trunk/

The opendsscmd version requires Lazarus/Free Pascal to build. Some of the supporting modules may require a C++ compiler to build from source. See install_dir/Doc/OpenDSS_FNCS_Build.pdf for directions.

Third-party Components
======================

KLUSolve.DLL is open source software, available from www.sourceforge.net/projects/klusolve

The command history editor is forked from open source software, available from https://github.com/pnnl/linenoise-ng.git 

Manual Installation (deprecated)
================================

Linux:

1 - cp opendsscmd /usr/local/bin
2 - cp *.so /usr/local/lib
3 - sudo ldconfig
4 - *.dss, *.DSS, *.sh, *.player and *.yaml are test files
5 - if you have FNCS, update with cp fncs_player /usr/local/bin

Mac OSX:

1 - cp opendsscmd /usr/local/bin
2 - cp *.dylib /usr/local/lib
3 - *.dss, *.DSS, *.sh, *.player and *.yaml are test files
4 - if you have FNCS, update with cp fncs_player /usr/local/bin

Windows:

1 - copy opendsscmd.exe, libklusolve.dll and liblinenoise.dll to a new directory such as c:\opendsscmd
2 - add c:\opendsscmd to your path
3 - Unless you have 64-bit GridLAB-D installed, unzip MinGW64Redist.zip into c:\opendsscmd
4 - *.dss, *.DSS, *.bat, *.player and *.yaml are test files to be put in your working or test directory

Limitations on Windows:

1 - the FNCS player has not been updated, so test_fncs.bat will not work. Instead, you can type those commands at the opendsscmd prompt (>>)
2 - please report any missing DLLs not included with MinGW64redist.zip
3 - the command-line editor in opendsscmd will drop characters if you type at medium speed or faster. We may look for an alternative or patch to linenoise-ng

License
=======

Use of this software is subject to a license. The terms are in:

1 - A file called "license.txt" distributed with the software, and
2 - The user manual
