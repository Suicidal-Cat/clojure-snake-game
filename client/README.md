# Client

The Client side of this project is created using ClojureScript in combination with **[Reagent](https://github.com/reagent-project/reagent)** (React like interface) and **[Figwheel](https://github.com/bhauman/lein-figwheel)** for fast build times and fast development.

## Local setup

The project was built and developed with help of **[Leiningen](https://leiningen.org/)** for build automation and dependecy managment.

Build and run:
```
lein deps
lein figwheel
```

## Game
The game combines real-time communication through default JavaScript **WebSockets** with the graphics capabilities of **[Quil](https://github.com/quil/quil)**, delivering both smooth data flow and visual content.

Quil is a great option for building a simple multiplayer games, that doesn't require strong engines to support it. In Quil, a sketch is defined mainly by two functions:

1. **Setup** – This function runs once at the beginning of the sketch. It’s used to define settings such as canvas size, background color, frame rate, or to initialize any state you need (for example, the starting position of a snake).

2. **Draw** – This function runs repeatedly, once per frame. It’s responsible for rendering graphics on the screen. Anything you want to see shapes, colors, text, animations gets drawn here. By reading and updating your state inside draw, you can create smooth animations and interactive elements.

## Server communication
The game communicates with the server using the JavaScript fetch API, which provides a promise-based way to send HTTP requests.

For authentication and security, the client uses **[JSON Web Tokens](https://datatracker.ietf.org/doc/html/rfc7519)**. When a user logs in, the server issues a signed JWT, which the client stores in local storage. For each request, the client includes this token in the HTTP headers under the **Authorization: Bearer 'token'**.

This approach ensures that:

1. The server can validate the identity of the user with every request.

2. Communication remains stateless, since the server doesn’t need to store session data.

## Reagent
**[Reagent](https://github.com/reagent-project/reagent)** is a minimalistic ClojureScript interface to React, allowing you to build dynamic user interfaces using ClojureScript’s functional programming style.

The reason why i choose Reagent it was because i already knew the basics of React and there were many tutorials about it :).