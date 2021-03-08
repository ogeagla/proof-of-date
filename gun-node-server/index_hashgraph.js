const Gun = require('gun');

const SEA = Gun.SEA;

require('gun/lib/not.js');

const {
    Client,
    PrivateKey,
    AccountCreateTransaction,
    AccountId,
    Mnemonic,
    PublicKey,
    TransferTransaction,
    TransactionResponse,
    TransactionRecord,
    AccountBalanceQuery,
    Hbar
} = require("@hashgraph/sdk");

require("dotenv").config();


let masterAccountId, masterPrivateKey, client;
let recvAccountId, recvPrivateKey, recvPublicKey;
let gunUser;

async function proveFact(factKey, fact) {
    if (!gunUser) {
        console.error(`Hashgraph gunuser not found: `, gunUser);
    }

    console.log(`HASHGRAPH PROVE FACT BEGIN ${JSON.stringify(fact)}`);
    try {
        const userDat = `${fact['username']}|${fact['label']}`.slice(0, 32);

        const transferTransactionResponse = await new TransferTransaction()
            .addHbarTransfer(masterAccountId, Hbar.fromTinybars(-1)) //Sending account
            .addHbarTransfer(recvAccountId, Hbar.fromTinybars(1)) //Receiving account
            .setMaxTransactionFee(Hbar.fromTinybars(200000))
            .setTransactionMemo(`${userDat}|${fact['digest']}`) // max 100 chars
            .execute(client);


        const record = await transferTransactionResponse.getRecord(client);
        const txTs = record.consensusTimestampstamp.seconds.toString();
        const txId = transferTransactionResponse.transactionId.toString()
        const transactionReceipt = await transferTransactionResponse.getReceipt(client);
        const status = transactionReceipt.status.toString();
        console.log("HASHGRAPH PROVE FACT END: ", status, txTs, txId);

        if (status !== "SUCCESS") {
            console.error(`!!!\nHASHGRAPH tx failed: ${status} `, transferTransactionResponse, '\n!!! HASHGRAPH');
            return {};
        }

        const proofData = {
            'proof-hashgraph-tx-id': txId,
            'proof-hashgraph-tx-ts': txTs,
        };



        return proofData;
    } catch (e) {
        console.error(`Error in proveFact: ${JSON.stringify(e)}`);
    }
}

function loadRecvUser(d) {
    recvAccountId = AccountId.fromString(d['acctId']);
    recvPrivateKey = PrivateKey.fromString(d['privKey']);
    recvPublicKey = PublicKey.fromString(d['pubKey']);

    console.log(`Loaded user: ${recvAccountId}`);
}

async function setupRecvUser(gunServerUser) {


    const words = await Mnemonic.generate();
    const newAccountPrivateKey = await PrivateKey
        .fromMnemonic(words, "passworf");
    const newAccountPublicKey = newAccountPrivateKey.publicKey;

    const newAccountTransactionId = await new AccountCreateTransaction()
        .setKey(newAccountPublicKey)
        .setInitialBalance(Hbar.fromTinybars(1000))
        .execute(client);

    const getReceipt = await newAccountTransactionId.getReceipt(client);
    const newAccountId = getReceipt.accountId;


    const d = {
        privKey: newAccountPrivateKey.toString(),
        pubKey: newAccountPrivateKey.publicKey.toString(),
        acctId: newAccountId.toString(),
    };

    gunServerUser
        .get('creds-hashgraph')
        .get('active')
        .put(d, (fin) => {
            console.log(`Finished writing hashgrah creds! `, fin);
            loadRecvUser(d);
        });
}

async function boot(gunServerUser) {

    gunUser = gunServerUser;
    //Grab your Hedera testnet account ID and private key from your .env file
    masterAccountId = process.env.MY_ACCOUNT_ID;
    masterPrivateKey = process.env.MY_PRIVATE_KEY;

    // If we weren't able to grab it, we should throw a new error
    if (masterAccountId == null ||
        masterPrivateKey == null) {
        throw new Error("Environment variables myAccountId and myPrivateKey must be present");
    }

    client = Client.forTestnet();

    client.setOperator(masterAccountId, masterPrivateKey);
    console.log(`Got client for master acct: `, client.network);

    gunUser
        .get('creds-hashgraph')
        .get('active')
        .not((notFnd) => {

            setupRecvUser(gunUser);
        })
        .once((credVal, credKey) => {
            if (!credVal) {
                return;
            }
            console.log(`Already have creds: ${credVal['pubKey']}, ${credVal['acctId']} ${credVal['privKey']}`);

            loadRecvUser(credVal);
        })
}

module.exports = {boot, proveFact};
