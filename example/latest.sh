#!/bin/bash

cd ../ui/
shadow-cljs release app
cd ../example/
cp ../ui/target/out/main.js public/app/punk.js
