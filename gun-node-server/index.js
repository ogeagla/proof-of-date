
const Gun = require('gun');

const port = 8765;

const server = require('http').createServer().listen(port);

const gun = Gun({web: server});

console.log(`Server started on port: ${port}`);
console.log(`Gun: ${gun}`);


function wallDataHandler(value, key, _msg, _ev) {
    console.log(`Got wall datum: ${new Date()} : ${key} -> ${JSON.stringify(value)}`);
}

gun.get('user-proofs').on(wallDataHandler);
