require('gun/lib/not.js');
require('gun/lib/unset.js')
const hashgraph = require('./index_hashgraph');
const Gun = require('gun');

const SEA = Gun.SEA;
let serverPair = null;

console.log(`SR Gun server user`);

function dbToFact(userFactValue) {
    return {
        label: userFactValue['label'],
        secret: userFactValue['secret'],
        digest: userFactValue['digest'],
        username: userFactValue['username'],
        signature: userFactValue['signature'],
        pubkey: userFactValue['pub-key'],
    };
}



function wallFactNotExists(gun, gunServerUser, i, factRef, userFactValue, userFactKey) {
    return async (dataKey) => {
        console.log(`SR New wall fact! ${i}`);
        const userFactBody = dbToFact(userFactValue);

        const pubkey = userFactBody.pubkey;
        const signature = userFactBody.signature;

        const isVerified = await SEA.verify(signature, pubkey);

        if (isVerified === userFactBody.digest) {
            console.log(`FACT IS VERIF::: ${isVerified}`);


            const proofData = await hashgraph.proveFact(i, userFactBody);
            const wallObj = {...userFactBody, ...proofData};

            gunServerUser
                .get('wall-facts')
                .get(i)
                .put(wallObj, async (resPut) => {
                    console.log(`5 Wrote fact to wall: ${dataKey} -> ${JSON.stringify(resPut)} wrote: ${wallObj['label']}  for user  ${wallObj['username']}`);

                });


        } else {
            console.error(`Fact cannot be verified: ${JSON.stringify(userFactBody)}`);

        }

    }
}

function wallFactExists(gun, gunServerUser, i, factRef, userFactValue, userFactKey) {
    return  async (wallFactValue, wallFactKey) => {
        // console.log(`4a already have wall fact key: ${JSON.stringify(i)} for user ${gunServerUser}`);
        // todo determine diff between wallFactValue and userFactBody for updates

        if (userFactValue && userFactValue['delete'] === true && wallFactValue) {

            const signature = wallFactValue['signature'];
            const pubKeySecret = userFactValue['pub-key-secret'];
            const pubKey = userFactValue['pub-key'];
            const ePubKey = userFactValue['epub-key'];

            const seceret =  await SEA.secret(ePubKey, serverPair)
            const pubKeyDecrypted2 = await SEA.decrypt(pubKeySecret,seceret);

            if (pubKeyDecrypted2 !== pubKey) {
                console.error(`keys secrets do not match: `, pubKeySecret, ' , ', pubKeyDecrypted2,' , ', pubKey, ', epub: ', ePubKey, ', sec: ', seceret);
                process.exit(1);
            }

            console.log(`signature: `, signature);

            const userCanDelete = await SEA.verify(signature, pubKeyDecrypted2);

            if (userCanDelete  === wallFactValue.digest) {
                const del = await gunServerUser
                    .get('wall-facts')
                    .get(i)
                    .put(null);

                console.log(`SR Delete success! ${i}`);
                const del2 = gun
                    .get(factRef)
                    .put(null);

                // TODO this doesnt seem to delete, in fact the data seems to disappear on its own without the above put
                console.log(`SR delete again! ${factRef}, ${userFactKey} / ${JSON.stringify(del2)} `);
            } else {
                console.warn(`!!!! USER cannot delete, verif unmatching! !!!!`);
            }
        }
    }
}


function userPublicFacts(gun, gunServerUser) {
    return (value, key, _msg, _ev) => {
        for (let i in value) {
            // console.log(`ADD I: `, i, " V: ", value[i]);
            const obj = value[i];
            if (i === `_`) {
                console.log(`~~~~~~~~~ SR New Data got summary: ${i} ~~~~~~~~~`);

            } else {
                if (obj === null || obj === undefined) {
                    // console.log(`obj is null,, ignoring...`, i);
                    continue;
                }
                const factRef = obj['#'];
                gun
                    .get(factRef)
                    .once(
                        (userFactValue, userFactKey) => {

                            gunServerUser
                                .get('wall-facts')
                                .get(i)
                                .once(wallFactExists(gun, gunServerUser, i, factRef, userFactValue, userFactKey))
                                .not(wallFactNotExists(gun, gunServerUser, i, factRef, userFactValue, userFactKey));
                        })
            }
        }

    }
}

function userPublicMeta(gun, gunServerUser) {

    return (value, key, _msg, _ev) => {
        console.log(`~~~~~~~~~~ got user k/v: `, key, value, ' ~~~~~~~~~~');
    }

}

function bootListeners(gun, gunServerUser) {

    console.log(`SV boot listeners`);
    gun.get('facts').on(userPublicFacts(gun, gunServerUser));
    // gun.get('users').on(userPublicMeta(gun, gunServerUser));

}

function boot(gun, gunServerUser) {
    console.log(`SV boot`);
    const createServerUser = () => {


        serverPair = gunServerUser._.sea;
        console.log(`SR PUB KEY: `, serverPair.pub);
        bootListeners(gun, gunServerUser);

    }
    try {

        createServerUser();


    } catch (e) {
        console.error(`SR Erorr creating server user: ${e}`);
    }


}

// boot();
module.exports = {boot};
