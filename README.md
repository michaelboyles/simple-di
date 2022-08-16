![GitHub Workflow Status](https://img.shields.io/github/workflow/status/michaelboyles/simple-di/Java%20CI%20with%20Maven)
![License](https://img.shields.io/github/license/michaelboyles/simple-di)

A simple dependency injection framework for Java, using code generation at compile-time.

This project is designed to demonstrate how code generation techniques can be used to write frameworks
which are often implemented at runtime using reflection (e.g. Spring). It is not designed for real-world use.

It is built around the [`javax.inject`](https://docs.oracle.com/javaee/6/api/javax/inject/package-summary.html)
annotations, though doesn't conform completely to that spec.

### Sample output

```java
public final class SimpleDIContext {
    private final Map<String, Object> nameToBean = new HashMap<>();

    public SimpleDIContext() {
        PassengerSeat passengerSeat = new PassengerSeat();
        DriversSeat driversSeat = new DriversSeat();
        Turbocharger turbocharger = new Turbocharger();
        Engine engine = new Engine(turbocharger);
        Car car = new Car(engine, driversSeat, List.of(passengerSeat, driversSeat));
        nameToBean.put("passengerSeat", passengerSeat);
        nameToBean.put("driversSeat", driversSeat);
        nameToBean.put("turbocharger", turbocharger);
        nameToBean.put("engine", engine);
        nameToBean.put("car", car);
    }

    public Object getBeanByName(String name) {
        return nameToBean.get(name);
    }
}
```

### How to run

Run `mvn verify` and `test/target/generated-sources/annotations` will then contain the generated context
class.

Make changes to the `test` module, e.g. to add or remove components, then re-run `mvn verify` and the class will be
updated.

### Implemented

- Provide beans by annotating classes with `@Singleton`
- Constructor injection
- Disambiguate constructors with `@Inject`
- Disambiguate beans with `@Named`
- Autowire collections (List, Set, etc.)

### Might be implemented

 - Method injection
 - Circular dependency resolution

### Won't be implemented

 - Field injection. [It's error-prone](https://stackoverflow.com/questions/19896870/why-is-my-spring-autowired-field-null)
   (check the view count), and private fields would need to be set via reflection, which is against the philosophy of
   this example.
