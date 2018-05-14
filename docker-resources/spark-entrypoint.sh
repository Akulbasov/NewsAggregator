#!/bin/bash

set -e
set -x


hdfs dfs -mkdir -p /tmp/sparkCheckpoint/cnn
hdfs dfs -mkdir -p /tmp/sparkCheckpoint/trends
hdfs dfs -mkdir -p /tmp/sparkCheckpoint/filter

${SPARK_HOME}/bin/spark-submit \
    --class "naggr.${APP_CLASS}" \
    --master local[2] \
    ${APP_BASE}/run-app.jar

