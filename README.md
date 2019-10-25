
Performance test: cglib vs. ByteBuddy lazy-loading proxies
====

This is a [JMH](https://openjdk.java.net/projects/code-tools/jmh/) based micro benchmark comparing the performance of Lazy-Loading proxies created
by [ByteBuddy](https://bytebuddy.net) vs [cglib](https://github.com/cglib/cglib) standard [lazy loader](https://github.com/cglib/cglib/blob/master/cglib/src/main/java/net/sf/cglib/proxy/LazyLoader.java) proxy.

## Running the tests

Build the project using

    mvn clean install
    
Subsequently, run the benchmark using

    java -jar target/benchmarks.jar

