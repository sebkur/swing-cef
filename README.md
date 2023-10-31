# Swing CEF

Run the application:

    ./gradlew pinpitRun

Package installers:

    ./gradlew pinpitPackageDefault

Run the application in test mode:

    ./gradlew pinpitRun --args="--test"

This project has been created using [pinpit](https://github.com/mobanisto/pinpit):

```
pinpit create-project --template swing --output . --project-name "Swing CEF" --package "de.mobanisto.jcef" \
  --description "a CEF-based web browser" --vendor-full "Mobanisto UG (haftungsbeschränkt)" \
  --vendor-short "Mobanisto" --input globe.svg --color-background 0x12567f --color-dialog 0xe3ff87
```
