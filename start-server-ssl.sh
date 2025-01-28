#!/bin/bash

export VIDEO_FILE_PATH=/root/video/
export VIDEO_FILE_NAME=video.mp4
export VIDEO_FILE_NAME=video-good.mp4

nohup java -jar gRPC_VideoServer/target/grpc-video-server.jar &

