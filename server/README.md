# Server

The Server side of this project is web api mainly based on Clojure web library **[Ring](https://github.com/ring-clojure/ring)** and **[MySQL](https://www.mysql.com/)** for the database. 

## Local Setup

The project was built and developed with help of **[Leiningen](https://leiningen.org/)** for build automation and dependecy managment.

1. Rename 'config-copy.end' file to 'config.end' and populate the fieds base on your local setup (database scripts can be found inside db folder):
    ```bash
    {:server-port "8080" ;; port number
      :disableDB false ;; disable database 
      :jwt-secret "testtest" ;;jwt key
      :db-config ;; database connection config 
      {
        :dbtype "mysql" ;; type of database
        :dbname "test-database" ;; name
        :user "" ;; username
        :password "" ;; passowrd
        :host "localhost" ;; host
        :port 3000  ;; hosted port number 
      }
    }  
    ```

2. Build and run:
    ```
    lein deps
    lein run
    ```
3. Run tests
    ```
    lein midje
    ```

## API
A lightweight API built with **[Ring](https://github.com/ring-clojure/ring)** and **[Compojure](https://github.com/ring-clojure/ring)**.

Compojure is used to define and compose routes into a single handler for Ring.

``` clojure
(def all-routes
  (routes
   (GET "/ws" [] echo-handler)
   public-routes
   wrapped-protected-routes))

(def app
  (-> all-routes
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete])))
```

Ring is the core of this API, handling all HTTP requests and responses. The application runs on the Ring Jetty adapter, which uses the Jetty web server to serve the API.
``` clojure
(defn -main [& args]
  (let [port (Integer/parseInt
              (or (System/getenv "PORT")
                  (str (:server-port config))
                  "3000"))]
    (println "Starting server on port" port)
    (run-jetty app {:port port :join? false})))
```
All requests and responses use the **[application/edn](https://github.com/edn-format/edn)**
 format instead of JSON. This decision is based on EDN’s natural compatibility with Clojure data structures. Unlike JSON, EDN can be parsed and emitted directly by Clojure without requiring additional transformation layers or third-party libraries.

Using EDN offers several advantages:

1. Native compatibility: EDN is a subset of Clojure syntax, which means data can be read and written using built-in tools.

2. Richer data representation: EDN supports keywords, sets, ratios, symbols, tagged literals (e.g. #inst, #uuid), and other constructs that JSON cannot represent natively.

3. Consistency: By removing the impedance mismatch between JSON and Clojure, systems remain simpler, more predictable, and less error-prone.

For authentication and security of the endpoint, server uses **[JSON Web Tokens](https://datatracker.ietf.org/doc/html/rfc7519)**. When a user logs in, the server issues a signed JWT, which the client stores in local storage. For each request, the client includes this token in the HTTP headers under the **Authorization: Bearer 'token'** which server validates. The JWT is implemented using **[Buddy Sign](https://github.com/funcool/buddy-sign)**.

## Database
The application database is implemented using MySQL and hosted locally through XAMPP, which provides the MySQL server environment. For connectivity, the project uses the official **[MySQL Connector/J JDBC driver](https://mvnrepository.com/artifact/com.mysql/mysql-connector-j)** together with **[next.jdbc](https://github.com/seancorfield/next-jdbc)**.

Inside /db folder you can find the .sql script for creating the database locally.

## Testing
Server code is covered by unit tests supported by **[Midje](https://github.com/marick/Midje)**.

Run tests:
```
lein midje
```