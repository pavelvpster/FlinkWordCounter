# Flink Word Counter

Word counter example using [Apache Flink](https://flink.apache.org/)

## How to build

```shell
./gradlew shadowJar
```

## How to run

Start Apache Flink

```shell
cd ./docker
docker compose up
```

Open [Dashboard](http://localhost:64000/)

Go to 'Submit New Job'

In 'Uploaded Jars' hit 'Add New' button

Select ./build/libs/*-all.jar

Click on just uploaded jar and hit 'Submit' button

Search for output in docker compose output

```
Reduce -> Sink: Print to Std. Out (1/1) ...
docker-taskmanager-1  | (123,1)
docker-taskmanager-1  | (abc,2)
docker-taskmanager-1  | (def,2)
docker-taskmanager-1  | (xyz,1)
```

Or provide 'output' program argument to save output to file

```
-output /data/output
```

The program will create a directory, not single file


## License

Copyright (C) 2023 Pavel Prokhorov (pavelvpster@gmail.com)


This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
