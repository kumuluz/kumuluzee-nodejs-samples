# KumuluzEE Microservices in Node.js and Java 

Goal of this tutorial is to develop two microservices, one in Java and one in Node.js, which will communicate between themselves through KumuluzEE's service discovery and config.

We will develop a sample application for managing customers and their orders. Application consists of two microservices; one written in Node.js for managing customers and the other written in Java for managing orders.

Our Java microservice will use following KumuluzEE extensions:
- KumuluzEE Config for dynamic reconfiguration of microservices with the use of configuration servers,
- KumuluzEE Discovery for service registration and service discovery

Our Node.js microservice will use following KumuluzEE packages:
- @kumuluz/kumuluzee-config for dynamic reconfiguration of microservices with the use of configuration servers,
- @kumuluz/kumuluzee-discovery for service registration and service discovery

Both microservices will use Consul to store configuration and register services. With minor tweaks the tutorial will work with Etcd server as well.

First we will create Maven project that will contain our Java microservice. Since this part is covered in more detail in other samples, we will show just the important bits. Then we will setup a MVC styled Express.js application.

Complete source code can be found on the github repository

## Run Consul server

You can download Consul from [their webpage](https://www.consul.io/).

Then you can run it by typing:
```
$ consul agent -dev
```
And you can access its UI by typing http://localhost:8500 into your browser.

## Create Maven project

We will create multi-module Maven project called java-service with following structure and pom.xml:
- java-service
    - api
    - services
    - persistence

```xml
<!-- root pom.xml -->
<properties>
    <java.version>1.8</java.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <kumuluzee.version>2.5.3</kumuluzee.version>
    <postgres.version>42.2.1</postgres.version>
    <kumuluzee-cors.version>1.0.3</kumuluzee-cors.version>
    <kumuluzee-config-consul.version>1.1.0</kumuluzee-config-consul.version>
    <kumuluzee-discovery-consul.version>1.1.0</kumuluzee-discovery-consul.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- KumuluzEE bom -->
        <dependency>
            <groupId>com.kumuluz.ee</groupId>
            <artifactId>kumuluzee-bom</artifactId>
            <version>${kumuluzee.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.kumuluz.ee.cors</groupId>
            <artifactId>kumuluzee-cors</artifactId>
            <version>${kumuluzee-cors.version}</version>
        </dependency>
        <dependency>
            <groupId>com.kumuluz.ee.config</groupId>
            <artifactId>kumuluzee-config-consul</artifactId>
            <version>${kumuluzee-config-consul.version}</version>
        </dependency>
        <dependency>
            <groupId>com.kumuluz.ee.discovery</groupId>
            <artifactId>kumuluzee-discovery-consul</artifactId>
            <version>${kumuluzee-discovery-consul.version}</version>
        </dependency>
        <!-- external -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgres.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Persistence module

This module will store our data object and our persisted entities

```xml
<!-- persistence pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-jpa-eclipselink</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

#### Order - persisted entity

This is Order entity, which is persisted into our local PostgreSQL database.

```java
@Entity
@Table(name = "orders")
@NamedQueries({
    @NamedQuery(name = "Order.findAllByCustomer", query = "SELECT o FROM Order o WHERE o.customerId = :customer_id")
})
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "customer_id")
    private long customerId;

    // getters and setters ...
    
}
```

#### OrderRequest

This data object is used as request body to create new order.

```java 
public class OrderRequest {
    
    private long customerId;
    private String title;
    private String description;

    // getters and setters ....
}
```

#### CustomerResponse

This data object will be used later in the tutorial and it represents data that our Node.js service will return to our Java service.

```java
public class CustomerResponse {

    private long id;
    private String name;
    private String lastName;
    private String email;
    private String phone;

    // getters and setters ....
    
}
```

### Service module

This module provides beans that work with entity manager.

```xml
<!-- services pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-jta-narayana</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-cdi-weld</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee.nodejs.samples.tutorial.java.service</groupId>
        <artifactId>persistence</artifactId>
    </dependency>
</dependencies>
```

#### OrdersBean

This bean is persisting our order entities into our database.

```java
@ApplicationScoped
public class OrdersBean {
    
    @PersistenceContext(unitName = "db-jpa-unit")
    private EntityManager entityManager;
    
    public List<Order> getAllOrdersFromCustomer(long customerId) {
        Query query = entityManager.createNamedQuery("Order.findAllByCustomer");
        query.setParameter("customer_id", customerId);
        return query.getResultList();
    }
    
    public Order getOrderById(long orderId) {
        Order order = entityManager.find(Order.class, orderId);
        if (order == null) {
            throw new JavaServiceException("Order not found!", 404);
        }
        return order;
    }
    
    @Transactional
    public void createOrder(Order order) {
        entityManager.persist(order);
    }
}
```

### Api module

Api module is used to expose our application through REST Api and also to register and discover services.

```xml
<dependencies>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-servlet-jetty</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee.nodejs.samples.tutorial.java.service</groupId>
        <artifactId>services</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee</groupId>
        <artifactId>kumuluzee-jax-rs-jersey</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee.cors</groupId>
        <artifactId>kumuluzee-cors</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee.config</groupId>
        <artifactId>kumuluzee-config-consul</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kumuluz.ee.discovery</groupId>
        <artifactId>kumuluzee-discovery-consul</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>com.kumuluz.ee</groupId>
            <artifactId>kumuluzee-maven-plugin</artifactId>
            <version>${kumuluzee.version}</version>
            <executions>
                <execution>
                    <id>package</id>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Api module also contains config.yaml which looks like this:

```yaml
kumuluzee:
  name: java-service
  version: 1.0.0
  env:
    name: dev
  server:
    base-url: http://localhost:8080
  config:
    start-retry-delay-ms: 500
    max-retry-delay-ms: 900000
    consul:
      hosts: http://localhost:8500
  discovery:
    consul:
      hosts: http://localhost:8500
    ttl: 20
    ping-interval: 15
  datasources:
    - jndi-name: jdbc/orders_database
      connection-url: jdbc:postgresql://localhost:5432/orders
      username: postgres
      password: postgres
      pool:
        max-size: 20
```

#### OrderApplication

Here in OrderApplication.java we register our service to our config server

```java
@ApplicationPath("v1")
@CrossOrigin
@RegisterService
public class OrderApplication extends Application {}
```

#### OrderResource

OrderResource.java exposes two endpoints. First one returns all orders from a given customer. Second endpoint creates order for a given customer and this is the endpoint our Node.js service will call. 

```java
@ApplicationScoped
@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {
    
    @Inject
    private OrdersBean ordersBean;
    
    // get all orders from customer with given id
    @GET
    @Path("customer/{customerId}")
    public Response getOrdersFromCustomer(@PathParam("customerId") long customerId) {
        List<Order> orders = ordersBean.getAllOrdersFromCustomer(customerId);
        return Response.status(Response.Status.OK).entity(orders).build();
    }
    
    // create new order
    @POST
    public Response createOrderForCustomer(OrderRequest newOrder) {
        Order order = new Order();
        order.setCustomerId(newOrder.getCustomerId());
        order.setTitle(newOrder.getTitle());
        order.setDescription(newOrder.getTitle());
        
        ordersBean.createOrder(order);
        
        return Response.status(Response.Status.CREATED).entity(order).build();
    }
}
```

#### CustomerResource

CustomerResource has only one endpoint, which gets order id and fetches information of customer that made the order. This information is retrieved from our Node.js service, which is discovered and stored in serviceUrl.

```java
@ApplicationScoped
@Path("customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {
    
    @Inject
    private OrdersBean ordersBean;
    
    // discovering service
    @Inject
    @DiscoverService(value = "node-service", version = "1.0.0", environment = "dev")
    private Optional<WebTarget> serviceUrl;
    
    // get customer for given order id
    @GET
    @Path("{orderId}")
    public Response getCustomerFromOrder(@PathParam("orderId") long orderId) {
        Order order = ordersBean.getOrderById(orderId);
        
        if (!serviceUrl.isPresent()) {
            throw new JavaServiceException("Service URL not found!", 404);
        }
        
        // build api url from service url
        WebTarget apiUrl = serviceUrl.get().path("v1/customers/" + order.getCustomerId());
        
        // perform GET request
        Response response = apiUrl.request().get();
        
        if (response.getStatus() == 200) {
            // read received entity and cast it into CustomerResponse
            CustomerResponse customerResponse = response.readEntity(CustomerResponse.class);
            return Response.status(Response.Status.OK).entity(customerResponse).build();
        } else {
            // if Node.js service returns any other status code than 200, 
            // our Java service will throw Internal Server Error 500
            throw new JavaServiceException("Service returned error status code: " + response.getStatus(), 500);
        }
        
    }
}
```

## Create Express.js project

Now we will create our Node.js service. It will be built using very popular Express.js framework and axios client to perform promise-based HTTP requests.


First we initialize our npm project:
``` 
$ npm init 
```

And then we install needed dependencies:

```
$ npm install --save express body-parser cookie-parser axios
$ npm install --save @kumuluz/kumuluzee-config @kumuluz/kumuluzee-discovery
```

### create folder structure

In this tutorial we will follow MVC-ish structure, therefore we will structure our application to look like this:
- node-service
    - app/
        - controllers/
        - models/
        - routes/
        - services/
        - app.js
    - bin/
        - www
    - config/
        - config.yaml

app folder will have our routes, controllers, services, models and all our logic, while our bin folder will contain startup file.

### Create startup file

Now we will create our startup file called **bin/www** (which is express convention). Some of files used are non-existent yet, therefore your IDE may complain that it cannot find files, but that will soon be fixed.
```js
const app = require("../app/app.js");
const http = require("http");
const KumuluzeeService = require("../app/services/kumuluzee.service");

const normalizePort = (val) => {
    const port = parseInt(val, 10);
    if (isNaN(port)) {
        return val;
    }
    if (port >= 0) {
        return port;
    }
    return false;
};

const onError = (error) => {
    if (error.syscall !== "listen") {
        throw error;
    }
    const bind = typeof port === "string" ? `Pipe ${port}` : `Port ${port}`;
    switch (error.code) {
        case "EACCES":
            console.error(`${bind} requires elevated privileges!`);
            process.exit(1);
            break;
        case "EADDRINUSE":
            console.error(`${bind} is already in use!`);
            process.exit(1);
            break;
        default:
            throw error;
    }
};

const onListening = () => {
    const addr = server.address();
    const bind = typeof port === "string" ? `Pipe ${addr}` : `Port ${addr.port}`;
    console.log(`Listening on ${bind}`);
    // call our KumuluzeeService to initialize and register our application on its startup
    KumuluzeeService.registerService();
};

// normalize and set port
const port = normalizePort(process.env.PORT || "3000");
app.set("port", port);

// create http server
const server = http.createServer(app);
// tell server to listen on port
server.listen(port);
// add event listeners
server.on("error", onError);
server.on("listening", onListening);
```

### Create config.yaml file

Now in our config folder we will create file **config.yaml** and write our configuration in the already known KumuluzEE format:
```yaml
kumuluzee:
  # service name
  name: node-service
  server:
    # url where our service will live
    base-url: http://localhost:3000
    http:
      port: 3000
  env:
    name: dev
  # specify hosts for discovery server
  discovery:
    consul:
      hosts: http://localhost:8500
  # specify hosts for config server
  config:
    consul:
      hosts: http://localhost:8500
# our custom configuration which will be registered in config server
rest-config:
  maintenance: false
```

### Create Config Bundle file

Now we create our **config.bundle.js** file, which will map our configuration and watch for its changes if needed.

```js
const ConfigBundle = require("@kumuluz/kumuluzee-config").default;

const remoteConfig = new ConfigBundle({
    // prefix key of our custom configuration
    prefixKey: "rest-config",
    type: "object",
    fields: {
        // name used in our app to access property
        maintenanceMode: {
            // variable type
            type: "boolean",
            // name as specified in config.yaml
            name: "maintenance",
            // watch for any incoming changes
            watch: true
        }
    }
});

// export remoteConfig object
module.exports = remoteConfig;
```

### Create KumuluzEE Service File

Now we will create our KumuluzEE Service, which will take care of registering and discovering services and initializing configuration.

```js
const path = require("path");
// we specify path to our config.yaml
const configurationPath = path.join(__dirname, "..", "..", "config", "config.yaml");
const KumuluzeeDiscovery = require("@kumuluz/kumuluzee-discovery").default;
const ConfigurationUtil = require("@kumuluz/kumuluzee-config").ConfigurationUtil;
// import our remote config
const remoteConfig = require("./config.bundle");

let util = null;

class KumuluzeeService {

    // returns configuration util, which can be used to lookup configuration manually
    static getUtil () {
        return util;
    }

    // registers our service and initializes config - called from ./bin/www
    static async registerService () {
        // initialize config
        await remoteConfig.initialize({
            extension: "consul",
            configPath: configurationPath
        });
        // set util when config is initialized
        util = ConfigurationUtil;
        // initialize discovery library
        await KumuluzeeDiscovery.initialize({
            extension: "consul"
        });
        // register service
        KumuluzeeDiscovery.registerService();
    }

    // discovers service 
    static discoverService (serviceName, serviceVersion, environment) {
        return KumuluzeeDiscovery.discoverService({
            value: serviceName,
            version: serviceVersion,
            environment: environment,
            accessType: "DIRECT"
        });
    }

}

module.exports = KumuluzeeService;
```

### Create app.js

In express, **app.js** is the main file, which glues all the other files together. Here we can define error handlers, middlewares and our routes.

```js
const express = require("express");
const cookieParser = require("cookie-parser");
const bodyParser = require("body-parser");
// our router, which will be added later
const CustomerRouter = require("./routes/customers.route");
// our remote config file we wrote earlier
const remoteConfig = require("./services/config.bundle");

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));
app.use(cookieParser());

// Registers middleware function, which for each request checks
// our external configuration and if 'maintenance' key is set to true
// it will return error saying service is unavailable, 
// otherwise it will call next handler.
// To test, while running go to http://localhost:8500 and change
// key 'environments/dev/services/node-service/1.0.0/config/rest-config/maintenance' to 'true'
// and then try to perform a request. To enable it again, just change the key to 'false'
app.use(async (req, res, next) => {
    const isInMaintenance = remoteConfig.maintenanceMode;
    if (isInMaintenance) {
        res.status(503);
        res.json({
            status: 503,
            message: "Service is in maintenance mode, try again later!"
        });
    } else {
        next();
    }
});

// registers our route
app.use("/v1/customers", CustomerRouter);

// if no other middleware stopped execution, set error to Not Found
app.use((req, res, next) => {
    const err = new Error("Not found");
    err.status = 404;
    next(err);
});

// error handler, which returns error object
app.use((err, req, res, next) => {
    const status = err.status || 500;
    const responseObject = {
        status: status,
        message: err.message,
        stackTrace: req.app.get("env") === "development" ? err.stack : {}
    };
    res.status(status);
    res.json(responseObject);
});

module.exports = app;
```

### Create router

Now we will define our router file, which will forward our request to controller functions

```js
const express = require("express");
const router = express.Router();

const CustomerController = require("../controllers/customers.controller");

// GET /v1/customers/
router.get("/", CustomerController.getCustomers);
// GET /v1/customers/:id/order/
router.get("/:id/order", CustomerController.createOrder);
// GET /v1/customers/:id
router.get("/:id", CustomerController.getOneCustomer);

module.exports = router;
```

### Create controller

This is controller which performs logic that was set in router file

```js
const CustomerService = require("../services/customers.service");
const OrderService = require("../services/order.service");
const OrderRequest = require("../models/order.request");

// returns array of customers with 200 OK code
module.exports.getCustomers = async (req, res, next) => {
    const users = CustomerService.getAllCustomers();
    res.status(200).json(users);
};

// returns user object with 200 OK code if found
// and 404 NOT FOUND code if such user doesn't exists
module.exports.getOneCustomer = (req, res, next) => {
    const customerId = parseInt(req.params.id, 10);
    const user = CustomerService.getOneCustomer(customerId);
    if (!user || user == null || user === undefined) {
        res.status(404).json({
            status: 404,
            message: `Customer with id ${customerId} not found!`
        });
    } else {
        res.status(200).json(user);
    }

};

// this endpoint generates new Order Request and calls our
// Java service to create it
// Returns order with 201 CREATED code if successful.
module.exports.createOrder = async (req, res, next) => {
    const newOrderRequest = new OrderRequest(101, "Second order",
        "This is the second order, which follows the first one.");

    const orderResponse = await OrderService.createOrder(newOrderRequest);

    res.status(201).json(orderResponse);
};
```

### Create Services

Now we will create services which will store our data.

#### OrderService

Order service calls our Java service, by first discovering that service and then performing request with axios library.

```js
const axios = require("axios");
const KumuluzeeService = require("./kumuluzee.service");
const OrderResponse = require("../models/order.response");

class OrderService {

    static async createOrder (orderRequest) {
        // discover Java service
        const serviceUrl = await KumuluzeeService.discoverService("java-service", "1.0.0", "dev");
        // build api url
        const apiUrl = `${serviceUrl}v1/orders`;
        try {
            // perform POST request
            const responseObject = await axios.post(apiUrl, orderRequest);
            if (responseObject.status === 201) {
                return new OrderResponse(responseObject.data);
            } else {
                // Java service returned some other code than 201
                throw new Error(`Bad response code: ${responseObject.status}!`);
            }
        } catch (requestException) {
            console.error("Napaka pri POST zahtevku! ", requestException);
        }
    }
}

module.exports = OrderService;
```

#### CustomerService

Customer service is our local storage of our Customer data. Usually we would store such data into database and our CustomerService would perform SELECT statement, but for the sake of tutorial we will omit database connectivity and store our users into array. There are many libraries available for handling many different databases, so you can choose your prefferred one and implement CustomerService so that it reads from database.

```js
const Customer = require("../models/customer.model");

const mockDB = [
    new Customer(100, "John", "Carlile", "john.ca@mail.com", "053347863"),
    new Customer(101, "Ann", "Lockwood", "lockwood_ann@mail.com", "023773123"),
    new Customer(102, "Elizabeth", "Mathews", "eli23@mail.com", "043343403"),
    new Customer(103, "Isaac", "Anderson", "isaac.anderson@mail.com", "018743831"),
    new Customer(104, "Barret", "Peyton", "barretp@mail.com", "063343148"),
    new Customer(105, "Terry", "Cokes", "terry_cokes@mail.com", "053339123")
];

class CustomerService {

    static getAllCustomers () {
        return mockDB;
    }

    static getOneCustomer (id) {
        if (!Number.isInteger(id)) {
            throw new Error("id must be integer!");
        }
        return mockDB.find((user) => {
            return user.id === id;
        });
    }
}

module.exports = CustomerService;
```

### Create data objects

Now we will create our data objects, which represent data we work with.

```js
// our "persisted" entity
class Customer {
    constructor (id, name, lastName, email, phone) {
        if (!Number.isInteger(id)) {
            throw new Error("id is integer!");
        }
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }
}

module.exports = Customer;
```

```js
// request for creating new order (same as in our Java service)
class OrderRequest {
    constructor (customerId, title, description) {
        this.customerId = customerId;
        this.title = title;
        this.description = description;
    }
}

module.exports = OrderRequest;
```

```js
// our order response, which we receive from Java service
class OrderResponse {
    constructor (data) {
        this.id = data.id;
        this.title = data.title;
        this.description = data.description;
        this.customerId = data.customerId;
    }
}

module.exports = OrderResponse;
```

## Run it

Now we can run both projects:

```
$ cd java-service
$ mvn clean package
$ java -jar ./api/target/api-1.0.0.jar
...
$ cd node-service
$ node ./bin/www
```

And access their endpoints:
- Java service:
    - http://localhost:8080/v1/customers/{orderId}
    - http://localhost:8080/v1/orders/customer/{customerId}
- Node.js service:
    - http://localhost:3000/v1/customers/
    - http://localhost:3000/v1/customers/{customerId}/order
    - http://localhost:3000/v1/customers/{customerId}

## Conclusion

In this tutorial we have used the KumuluzEE framework to build Java and Node.js services and make them communicate between themselves. We demonstrated how to register and discover services and how to read external configuration.

Source code can be found in GitHub repository.
