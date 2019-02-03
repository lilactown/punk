# Punk

Punk is a developer tool for helping you visualize and navigate data in your application.

It implements the [Read-Eval-Browse-Loop](https://www.youtube.com/watch?v=c52QhiXsmyI)
concept that Stuart Halloway presented at Clojure/conj 2018.

The project status is currently what I would consider a "polished proof-of-concept":
fit for use, but by no means can I call it a maximal UX for such a 
tool.

I implore you to use it with high expectations, and give feedback accordingly!

- [Usage](#usage)
- [How it works](#how-it-works)
- [Installation](#installation)
- [Issues](#issues)
- [Feature requests](#feature-requests)

<img src="/screenshot.png" alt="screenshot" width="600">

## Usage

By default, Punk adds a listener to values emitted by `tap>`. To inspect a value,
simply use the `tap>` function to send it to the Punk UI. 

While your application is running, you will be able to view the Punk UI
either embedded on the webpage (browser) or on a web server started on your
localhost (Node.js).

![how to browser](./punk-1.gif)

There are 3 *panes* that will appear in the Punk UI:

- **Entries**: A list of the values that have been received by Punk.
- **Current**: The value that we most recently navigated to
- **Next**: A preview of a value that we can navigate to

Clicking on a value in the **Entries** pane will navigate to it and bring it 
into the **Current** view to be inspected.

Clicking on nested values in the **Current** pane will navigate to that value
and show it in the **Next** pane. Clicking on the value in the **Next** pane
will bring it into the **Current** pane.

In both **Current** and **Next**, you may change the "view" which you use to
visualize the value in that pane.

Currently, there are only simple aggregates (e.g. "map" for a tabular view of 
maps, "coll" for a tabular view of collections) and an "edn" view for easy copy
& paste. *User-built custom views coming soon!*


## How it works

Punk is built with a client-server architecture in order to support different 
environments your projects might run in.

The Punk `core` library is what handles calling `datafy` and `nav` on values, 
and must live in your application in order to work correctly. However, the UI 
can live completely separate from your application code, which means that the 
whole package is much lighter weight in terms of dependencies.

When in a web browser, it passes messages directly between your application and 
the UI via `core.async`. When in a server context (e.g. Node.js), it 
communicates via websockets.

If you're interested in implementing a Punk adapter in a specific context (e.g.
JVM, vanilla JavaScript, etc.), let me know! It is relatively straight forward
but there are still some details that I'm sure will need to be ironed out.

## Installation

For each platform, there is a Punk `adapter` library which sets up the necessary 
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

Now, load your app in the browser; Punk's drawer should be on the right hand 
side. Clicking the drawer or pressing the hot key `Ctrl-Alt-P` will expand the
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
Navigate there to see the the Punk UI.


## Issues

It's new, and still has bugs. Please file issues as you come across them!

## Feature requests

If you have a feature request or any feedback on features, please create
an issue with the "enhancement" tag.
