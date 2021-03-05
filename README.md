# proof-of-date


## Visit site

Go to https://proofof.date

## Run locally

#### Required: 
- `leiningen`, which requires `java`
- `node` and `npm`

#### Frontend:
``` 
lein watch
```

And visit `localhost:8280`

#### There is no backend, but to properly use p2p, run the peering server:
```
cd gun-node-server
npm install
node index.js
```
To use it, change (while frontend watch is running) `cljs-proof-of-date.events/peer-url` from `https://proofof.date:8765/gun`, which uses the live site peer server, to local address: `http://localhost:8765/gun`.


## Build a jar
``` 
lein uberjar
```
