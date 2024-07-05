- pre-requisites: install maven, install java 17
Download ScalarDB schema-loader v3.12.2 from https://github.com/scalar-labs/scalardb/releases/tag/v3.12.2
Put it in the working directory

- Create the database:
$ java -jar scalardb-schema-loader-3.12.2.jar --config ./src/main/resources/scalardb.properties -f ./src/main/resources/schema.json --coordinator

- Install the app:
$ mvn clean install

- Run the app:
$ mvn exec:java -Dexec.mainClass=com.example.moviedb.App

URL: http://localhost:7000/
