@echo off

set binPath=F:\dev\protobuf\protoc.exe


%binPath% -I=.\ --java_out=..\..\..\..\..\ DMCluster.proto
