# Punk

Punk is a [REBL-like](https://www.youtube.com/watch?v=c52QhiXsmyI) data browser
built for the web.

![how to browser](./punk-1.gif)

## Usage

Punk is built with a client-server architecture in order to support different 
kinds of environments you might want to send values from.

When in a web browser, it passes messages directly back and forth between your
application and the UI. When in a server context (e.g. Node.js), it communicates
via websockets.

For each platform, there is a Punk `adapter` library which sets up the necessary 
plumbing to allow your application to communicate to the UI application.

### Browser

In a browser project, include the `punk.adapter.web` library:

Leiningen/Boot

`[lilactown/punk-adapter-web "0.0.2-SNAPSHOT"]`

Clojure CLI/deps.edn

`lilactown/punk-adapter-web {:mvn/version "0.0.2-SNAPSHOT"}`

Then, in your CLJS build preloads, include the `punk.adapter.web.preload` 
namespace:

```clojure
  ;; shadow-cljs.edn example
  :app {:target :browser
        ;; ...
        :devtools {:preloads [punk.adapter.web.preload]}}
```

Now, load your app in the browser; Punk's drawer should be on the right hand 
side. Clicking the drawer or pressing the hot key `Ctrl-Alt-P` should expand the
UI.


### Node.js

In a Node.js project, include the `punk.adapter.node` library:

Leiningen/Boot

`[lilactown/punk-adapter-node "0.0.2-SNAPSHOT"]`

Clojure CLI/deps.edn

`lilactown/punk-adapter-node {:mvn/version "0.0.2-SNAPSHOT"}`

Then, in your CLJS build preloads, include the `punk.adapter.node.preload` 
namespace:

```clojure
  ;; shadow-cljs.edn example
  :app {:target :node-script
        ;; ...
        :devtools {:preloads [punk.adapter.node.preload]}}
```

The Node.js adapter automatically starts a web server at [http://localhost:9876](http://localhost:9876).
Navigate there to see the the Punk UI.

### Browsing data

By default, Punk adds a listener to values emitted by `tap>`. To inspect a value,
simply use the `tap>` function to send it to the Punk application. 


## Issues

It's new, and still has bugs. Please file issues as you come across them!
