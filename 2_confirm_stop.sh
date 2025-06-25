#!/bin/bash

aws ecs describe-services \
  --cluster bx-backend-cluster \
  --services bx-backend-service \
  --query "services[0].desiredCount"