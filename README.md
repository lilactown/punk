# Punk

A [REBL-like](https://www.youtube.com/watch?v=c52QhiXsmyI) data browser built
for the web.

![how to browser](./punk-1.gif)

## Usage

punk is built with a client-server architecture in order to support different 
kinds of environments you might want to send values from.

When in a web browser, it passes messages directly back and forth between your
application and the UI. When in a server context (e.g. Node.js), it communicates
via websockets.

For each platform, there is a punk `adapter` library which sets up the necessary 
plumbing to allow your application to communicate to the UI application.

### Browser

In a browser project, include the `punk.adapter.web` library:

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/punk-adapter-web.svg)](https://clojars.org/lilactown/punk-adapter-web)

Then, in your CLJS build preloads, include the `punk.adapter.web.preload` 
namespace:

```clojure
  ;; shadow-cljs.edn example
  :app {:target :browser
        ;; ...
        :devtools {:preloads [punk.adapter.web.preload]}}
```

Now, load your app in the browser; punk's drawer should be on the right hand 
side. Clicking the drawer or pressing the hot key `Ctrl-Alt-P` should expand the
UI.


### Node.js

In a Node.js project, include the `punk.adapter.node` library:

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/punk-adapter-node.svg)](https://clojars.org/lilactown/punk-adapter-node)

Then, in your CLJS build preloads, include the `punk.adapter.node.preload` 
namespace:

```clojure
  ;; shadow-cljs.edn example
  :app {:target :node-script
        ;; ...
        :devtools {:preloads [punk.adapter.node.preload]}}
```

The Node.js adapter automatically starts a web server at [http://localhost:9876](http://localhost:9876).
Navigate there to see the the punk UI.

### Browsing data

By default, punk adds a listener to values emitted by `tap>`. To inspect a value,
simply use the `tap>` function to send it to the punk application. 


## Issues

It's new, and still has bugs. Please file issues as you come across them!
