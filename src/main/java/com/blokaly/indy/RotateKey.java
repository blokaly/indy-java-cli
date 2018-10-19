package com.blokaly.indy;

import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;

import java.net.URL;

import static org.hyperledger.indy.sdk.ledger.Ledger.*;

public class RotateKey {

  static void demo() throws Exception {

    URL genesisUrl = WriteDIDAndQueryVerkey.class.getClassLoader().getResource("docker_pool_transactions_genesis");
    String genesisFile = genesisUrl.getPath();
    String poolConfig = "{\"genesis_txn\": \""+ genesisFile + "\"}";
    String walletConfig = "{\"id\": \"wallet\"}";
    String walletCredentials = "{\"key\": \"wallet_key\"}";

    String poolName = "pool";
    int procotolVersion = 2;

    Pool.setProtocolVersion(procotolVersion);

    // Step 2 code goes here.
    // Tell SDK which pool you are going to use. You should have already started
    // this pool using docker compose or similar. Here, we are dumping the config
    // just for demonstration purposes.
    System.out.println("\n1. Creating a new local pool ledger configuration that can be used later to connect pool nodes.\n");
    try {
      Pool.createPoolLedgerConfig(poolName, poolConfig).get();
    } catch (Exception ex) {
      Pool.deletePoolLedgerConfig(poolName).get();
    }

    System.out.println("\n2. Open pool ledger and get the pool handle from libindy.\n");
    Pool pool = Pool.openPoolLedger(poolName, "{}").get();

    System.out.println("\n3. Creates a new secure wallet\n");
    try {
      Wallet.createWallet(walletConfig, walletCredentials).get();
    } catch (Exception ex) {
      Wallet.deleteWallet(walletConfig, walletCredentials).get();
    }

    System.out.println("\n4. Open wallet and get the wallet handle from libindy\n");
    Wallet walletHandle = Wallet.openWallet(walletConfig, walletCredentials).get();

    // First, put a steward DID and its keypair in the wallet. This doesn't write anything to the ledger,
    // but it gives us a key that we can use to sign a ledger transaction that we're going to submit later.
    System.out.println("\n5. Generating and storing steward DID and Verkey\n");

    // The DID and public verkey for this steward key are already in the ledger; they were part of the genesis
    // transactions we told the SDK to start with in the previous step. But we have to also put the DID, verkey,
    // and private signing key into our wallet, so we can use the signing key to submit an acceptably signed
    // transaction to the ledger, creating our *next* DID (which is truly new). This is why we use a hard-coded seed
    // when creating this DID--it guarantees that the same DID and key material are created that the genesis txns
    // expect.
    String stewardSeed = "000000000000000000000000Steward1";
    String did_json = "{\"seed\": \"" + stewardSeed + "\"}";
    DidResults.CreateAndStoreMyDidResult stewardResult = Did.createAndStoreMyDid(walletHandle, did_json).get();
    String defaultStewardDid = stewardResult.getDid();
    System.out.println("Steward did: " + defaultStewardDid);

    // Now, create a new DID and verkey for a trust anchor, and store it in our wallet as well. Don't use a seed;
    // this DID and its keyas are secure and random. Again, we're not writing to the ledger yet.
    System.out.println("\n6. Generating and storing Trust Anchor DID and Verkey\n");
    DidResults.CreateAndStoreMyDidResult trustAnchorResult = Did.createAndStoreMyDid(walletHandle, "{}").get();
    String trustAnchorDID = trustAnchorResult.getDid();
    String trustAnchorVerkey = trustAnchorResult.getVerkey();
    System.out.println("Trust anchor DID: " + trustAnchorDID);
    System.out.println("Trust anchor Verkey: " + trustAnchorVerkey);

    // Here, we are building the transaction payload that we'll send to write the Trust Anchor identity to the ledger.
    // We submit this transaction under the authority of the steward DID that the ledger already recognizes.
    // This call will look up the private key of the steward DID in our wallet, and use it to sign the transaction.
    System.out.println("\n7. Build NYM request to add Trust Anchor to the ledger\n");
    String nymRequest = buildNymRequest(defaultStewardDid, trustAnchorDID, trustAnchorVerkey, null, "TRUST_ANCHOR").get();
    System.out.println("NYM request JSON:\n" + nymRequest);

    // Now that we have the transaction ready, send it. The building and the sending are separate steps because some
    // clients may want to prepare transactions in one piece of code (e.g., that has access to privileged backend systems),
    // and communicate with the ledger in a different piece of code (e.g., that lives outside the safe internal
    // network).
    System.out.println("\n8. Sending NYM request to ledger\n");
    String nymResponseJson = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, nymRequest).get();
    System.out.println("NYM transaction response:\n" + nymResponseJson);

    // At this point, we have successfully written a new identity to the ledger.

    // Step 3 code goes here.
    System.out.println("\n9. Generating new Verkey of Trust Anchor in the wallet\n");
    String newTrustAnchorVerkey = Did.replaceKeysStart(walletHandle, trustAnchorDID, "{}").get();
    System.out.println("New Trust Anchor's Verkey: " + newTrustAnchorVerkey);

    System.out.println("\n10. Building NYM request to update new verkey to ledger\n");
    String nymUpdateRequest = buildNymRequest(trustAnchorDID, trustAnchorDID, newTrustAnchorVerkey, null, "TRUST_ANCHOR").get();
    System.out.println("NYM request:\n" + nymUpdateRequest);

    System.out.println("\n11. Sending NYM request to the ledger\n");
    String nymUpdateResponse = signAndSubmitRequest(pool, walletHandle, trustAnchorDID, nymUpdateRequest).get();
    System.out.println("NYM response:\n" + nymUpdateRequest);

    System.out.println("\n12. Applying new Trust Anchor's Verkey in wallet\n");
    Did.replaceKeysApply(walletHandle, trustAnchorDID);

    // Step 4 code goes here
    System.out.println("\n13. Reading new Verkey from wallet\n");
    String trustAnchorVerkeyFromWallet = Did.keyForLocalDid(walletHandle, trustAnchorDID).get();

    System.out.println("\n14. Building GET_NYM request to get Trust Anchor from Verkey\n");
    String getNymRequest = buildGetNymRequest(trustAnchorDID, trustAnchorDID).get();
    System.out.println("GET_NYM request:\n" + getNymRequest);

    System.out.println("\n15. Sending GET_NYM request to ledger\n");
    String getNymResponse = submitRequest(pool, getNymRequest).get();
    System.out.println("GET_NYM response:\n" + getNymResponse);

    System.out.println("\n16. Comparing Trust Anchor verkeys\n");
    System.out.println("Written by Steward: " + trustAnchorDID);
    System.out.println("Current from wallet: " + trustAnchorVerkeyFromWallet);
    String responseData = new JSONObject(getNymResponse).getJSONObject("result").getString("data");
    String trustAnchorVerkeyFromLedger = new JSONObject(responseData).getString("verkey");
    System.out.println("Current from ledger: " + trustAnchorVerkeyFromLedger);
    boolean match = !trustAnchorDID.equals(trustAnchorVerkeyFromWallet) && trustAnchorVerkeyFromWallet.equals(trustAnchorVerkeyFromWallet);
    System.out.println("Matching: " + match);

    // Do some cleanup.
    System.out.println("\n17. Close and delete wallet\n");
    walletHandle.closeWallet().get();
    Wallet.deleteWallet(walletConfig, walletCredentials).get();

    System.out.println("\n18. Close pool\n");
    pool.closePoolLedger().get();

    System.out.println("\n19. Delete pool ledger config\n");
    Pool.deletePoolLedgerConfig(poolName).get();
  }
}