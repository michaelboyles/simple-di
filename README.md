[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/michaelboyles/simple-di/maven.yml?branch=develop)](https://github.com/michaelboyles/simple-di/actions)
[![License](https://img.shields.io/github/license/michaelboyles/simple-di)](https://github.com/michaelboyles/simple-di/blob/develop/LICENSE)

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
        Driver driver = new Driver();
        DriversSeat driversSeat = new DriversSeat();
        Turbocharger turbocharger = new Turbocharger();
        Engine engine = new Engine(turbocharger);
        Car car = new Car(engine, driversSeat, List.of(passengerSeat, driversSeat));
        car.addDriver(driver);
        car.addSeats(new Seat[] {passengerSeat, driversSeat});
        nameToBean.put("passengerSeat", passengerSeat);
        nameToBean.put("driver", driver);
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

- Provide beans by annotating classes with [`@Singleton`](https://docs.oracle.com/javaee/6/api/javax/inject/Singleton.html)
- Constructor and method injection
- Disambiguate constructors with [`@Inject`](https://docs.oracle.com/javaee/6/api/javax/inject/Inject.html)
- Disambiguate beans with [`@Named`](https://docs.oracle.com/javaee/6/api/javax/inject/Named.html)
- Autowire collections (List, Set, arrays, etc.), including wildcards
- Circular dependency resolution with [`Provider<T>`](https://docs.oracle.com/javaee/6/api/javax/inject/Provider.html)

### Not implemented

I don't intend to implement these, but they might make interesting projects.

- Field injection. [It's error-prone](https://stackoverflow.com/questions/19896870/why-is-my-spring-autowired-field-null)
  (check the view count), and private fields would need to be set via reflection, which is against the philosophy of
  this example.
- REST support. This goes beyond dependency injection but, in the spirit of Spring, you could search all
  `@Singleton`s for a custom `@GetMapping` annotation and use that to generate code to run a Tomcat server.
