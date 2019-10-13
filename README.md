# run-plotter

https://run-plotter.herokuapp.com

A Clojurescript web app for plotting runs, saving them and showing equivalent times for common race distances.

Built with [re-frame](https://github.com/Day8/re-frame)
and [react-leaflet](https://github.com/PaulLeCam/react-leaflet).

## Running

In production, both the frontend and REST API backend are served by a single server.
In dev mode, I use [shadow-cljs](https://github.com/thheller/shadow-cljs)
to serve the frontend on port 8280 to get hot reloading.

### Development Mode

Start the shadow-cljs server for the frontend:
```
npx shadow-cljs watch app
```
Start the backend server:
```
lein run
```

Wait a bit, then browse to [http://localhost:8280](http://localhost:8280).

## Production Build

Compile the optimised Javascript:
```
npx shadow-cljs release app
```
Run the server:
```
lein run
```

Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).

