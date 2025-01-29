#!/bin/bash

if [ `uname -m` == "arm64" ]; then 
  export VIDEO_FILE_PATH=/Users/sdubois/workspace/grpc-video-demo/videos/
  export VIDEO_FILE_NAME=video.mp4
  export VIDEO_FILE_NAME=video-good.mp4

  java -jar gRPC_VideoServerSSL/target/grpc-video-server.jar
  #java -jar gRPC_VideoServerSSL/target/grpc-video-server.jar --debug
  #java -jar gRPC_VideoServerSSL/target/grpc-video-server.jar 
else
  export VIDEO_FILE_PATH=/root/video/
  export VIDEO_FILE_NAME=video.mp4
  export VIDEO_FILE_NAME=video-good.mp4

  nohup java -jar gRPC_VideoServerSSL/target/grpc-video-server.jar &
fi


