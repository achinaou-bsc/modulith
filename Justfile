set dotenv-load

default:
  @just --list --unsorted

check-formatting:
  ./mill mill.scalalib.scalafmt/checkFormatAll

format:
  ./mill mill.scalalib.scalafmt/

run:
  ./mill modules.application.run

clean:
  rm --recursive --force dist
  ./mill clean

compile:
  ./mill -j 0 modules.__.compile

test:


package:
  mkdir --parents dist

  ./mill modules.application.assembly

  cp out/modules/application/assembly.dest/out.jar dist/application.jar

env ENVIRONMENT:
  cp .env.d/{{ENVIRONMENT}}.env .env

local-env-create:
  docker compose up --detach

local-env-start:
  docker compose start

local-env-stop:
  docker compose stop

local-env-destroy:
  docker compose down --volumes

local-env-reset: local-env-destroy local-env-create

tunnel-hadoop:
  gcloud compute ssh hadoop-m --project="$GCLOUD_PROJECT_ID" --zone="$GCLOUD_ZONE" --tunnel-through-iap -- \
    -N \
    -L 8020:$GCLOUD_INSTANCE:8020 \
    -L 8032:$GCLOUD_INSTANCE:8032 \
    -L 9866:$GCLOUD_INSTANCE:9866 \
    $(for port in $(seq "$HADOOP_CONFIGURATION_MAPRED_DEFAULT_YARN_APP_MAPREDUCE_AM_JOB_CLIENT_PORT_RANGE_START" "$HADOOP_CONFIGURATION_MAPRED_DEFAULT_YARN_APP_MAPREDUCE_AM_JOB_CLIENT_PORT_RANGE_END"); do printf ' -L %d:%s:%d' "$port" "$GCLOUD_INSTANCE" "$port"; done)
