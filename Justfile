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
