![GitHub Workflow Status](https://img.shields.io/github/workflow/status/michaelboyles/simple-di/Java%20CI%20with%20Maven)
![License](https://img.shields.io/github/license/michaelboyles/simple-di)

A simple dependency injection framework for Java, using code generation at compile-time.

This project is designed to demonstrate how code generation techniques can be used to write frameworks
which are often implemented at runtime using reflection (e.g. Spring). It is not designed for real-world use.

It is built around the [`javax.inject`](https://docs.oracle.com/javaee/6/api/javax/inject/package-summary.html)
annotations, though doesn't conform completely to that spec.

## Sample output

```java
public final class SimpleDIContext {
    private final Map<String, Object> nameToSingleton = new HashMap<>();

    public void start() {
        DriversSeat driversSeat = new DriversSeat();
        Turbocharger turbocharger = new Turbocharger();
        Engine engine = new Engine(turbocharger);
        Car car = new Car(engine, driversSeat);
        nameToSingleton.put("driversSeat", driversSeat);
        nameToSingleton.put("turbocharger", turbocharger);
        nameToSingleton.put("engine", engine);
        nameToSingleton.put("car", car);
    }

    public Object getSingletonByName(String name) {
        return nameToSingleton.get(name);
    }
}
```

## How to run

Run `mvn verify` and `test/target/generated-sources/annotations` will then contain the generated context
class.

Make changes to the `test` module, e.g. to add or remove components, then re-run `mvn verify` and the class will be
updated.
