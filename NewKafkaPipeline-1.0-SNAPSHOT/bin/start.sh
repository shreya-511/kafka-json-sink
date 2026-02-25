#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE}")" && pwd)"
APP_HOME="$(cd "$DIR/.." && pwd)"
PID_FILE="$APP_HOME/kafka-pipeline.pid"
JAR_FILE="$APP_HOME/NewKafkaPipeline-1.0-SNAPSHOT.jar"

# Configuration matching your new Java variables
export KAFKA_BOOTSTRAP="localhost:9092"
export KAFKA_TOPIC="vehicle-data"
export CONSUMER_GROUP="vehicle-monitoring-group"

start() {
    if [ -f "$PID_FILE" ]; then
        echo "Pipeline already running (PID: $(cat $PID_FILE))"
        return
    fi
    echo "Launching Kafka Pipeline..."
    # Running using the Full Class Path for safety
    nohup java -cp "$JAR_FILE" com.kafkapipeline.kafkaPipeline > "$APP_HOME/output.log" 2>&1 &
    echo $! > "$PID_FILE"
    echo "Started with PID $!"
}

stop() {
    if [ -f "$PID_FILE" ]; then
        kill $(cat "$PID_FILE") && rm "$PID_FILE"
        echo "Stopped."
    else
        echo "Not running."
    fi
}

case "$1" in
    start) start ;;
    stop) stop ;;
    restart) stop; sleep 2; start ;;
    *) echo "Usage: $0 {start|stop|restart}"; exit 1 ;;
esac
