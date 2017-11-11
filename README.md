## Installation and running

Clone the repository and go to project directory

Assembly artifacts
```bash
sbt assembly
```

Run workers
```bash
java -cp worker/target/scala-2.12/worker-assembly-0.1-SNAPSHOT.jar search.WorkerServer 10101
```
```bash
java -cp worker/target/scala-2.12/worker-assembly-0.1-SNAPSHOT.jar search.WorkerServer 10102
```
Run master
```bash
java -cp master/target/scala-2.12/master-assembly-0.1-SNAPSHOT.jar search.MasterServer 10000 127.0.0.1:10101 127.0.0.1:10102
```
Use console client to interact with system
```bash
java -cp client/target/scala-2.12/client-assembly-0.1-SNAPSHOT.jar search.ConsoleClient 127.0.0.1:10000 -put 10 "21212 81281"
java -cp client/target/scala-2.12/client-assembly-0.1-SNAPSHOT.jar search.ConsoleClient 127.0.0.1:10000 -get 10
java -cp client/target/scala-2.12/client-assembly-0.1-SNAPSHOT.jar search.ConsoleClient 127.0.0.1:10000 -search 21212
```
