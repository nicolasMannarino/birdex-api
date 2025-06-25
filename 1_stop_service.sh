#!/bin/bash

aws ecs update-service \
  --cluster bx-backend-cluster \
  --service bx-backend-service \
  --desired-count 0