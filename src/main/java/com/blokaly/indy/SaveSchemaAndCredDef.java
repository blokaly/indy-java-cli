package com.blokaly.indy;

import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateAndStoreCredentialDef;
import static org.hyperledger.indy.sdk.ledger.Ledger.*;

public class SaveSchemaAndCredDef {
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

    System.out.println("\n5. Generating and storing steward DID and Verkey\n");
    String stewardSeed = "000000000000000000000000Steward1";
    String did_json = "{\"seed\": \"" + stewardSeed + "\"}";
    DidResults.CreateAndStoreMyDidResult stewardResult = Did.createAndStoreMyDid(walletHandle, did_json).get();
    String defaultStewardDid = stewardResult.getDid();
    System.out.println("Steward DID: " + defaultStewardDid);
    System.out.println("Steward Verkey: " + stewardResult.getVerkey());

    System.out.println("\n6. Generating and storing Trust Anchor DID and Verkey\n");
    DidResults.CreateAndStoreMyDidResult trustAnchorResult = Did.createAndStoreMyDid(walletHandle, "{}").get();
    String trustAnchorDID = trustAnchorResult.getDid();
    String trustAnchorVerkey = trustAnchorResult.getVerkey();
    System.out.println("Trust anchor DID: " + trustAnchorDID);
    System.out.println("Trust anchor Verkey: " + trustAnchorVerkey);

    System.out.println("\n7. Build NYM request to add Trust Anchor to the ledger\n");
    String nymRequest = buildNymRequest(defaultStewardDid, trustAnchorDID, trustAnchorVerkey, null, "TRUST_ANCHOR").get();
    System.out.println("NYM request JSON:\n" + nymRequest);

    System.out.println("\n8. Sending the nym request to ledger\n");
    String nymResponseJson = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, nymRequest).get();
    System.out.println("NYM transaction response:\n" + nymResponseJson);

    // Step 3 code goes here.
    System.out.println("\n9. Build the SCHEMA request to add new schema to the ledger as a Steward\n");

    JSONObject schemaData = new JSONObject();
    schemaData.put("id", "1");
    schemaData.put("name", "gvt");
    schemaData.put("version", "1.0");
    schemaData.put("ver", "1.0");
    schemaData.append("attrNames", "age");
    schemaData.append("attrNames", "sex");
    schemaData.append("attrNames", "height");
    schemaData.append("attrNames", "name");

    JSONObject schema = new JSONObject();
    schema.put("seqNo", 1);
    schema.put("dest", defaultStewardDid);
    schema.put("data", schemaData);

    System.out.println("Schema: " + schema.toString());
    String schemaDataJSON = schemaData.toString();
    String schemaRequest = buildSchemaRequest(defaultStewardDid, schemaDataJSON).get();
    System.out.println("Schema request:\n" + schemaRequest);

    System.out.println("\n10. Sending the SCHEMA request to the ledger\n");
    String schemaResponse = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, schemaRequest).get();
    System.out.println("Schema response:\n" + schemaResponse);

    // Step 4 code goes here.
    System.out.println("\n11. Creating and storing CRED DEF using anoncreds as Trust Anchor, for the given Schema\n");
    String credDefTag = "cred_def_tag";
    String credDefType = "CL";
    String credConfigJson = "{\"support_revocation\": false}";
    AnoncredsResults.IssuerCreateAndStoreCredentialDefResult defResult = issuerCreateAndStoreCredentialDef(walletHandle, trustAnchorDID, schemaDataJSON, credDefTag, credDefType, credConfigJson).get();
    System.out.println("Returned Cred DefId:\n" + defResult.getCredDefId());
    System.out.println("Returned Cred Definition:\n" + defResult.getCredDefJson());

    // Some cleanup code.
    System.out.println("\n12. Close and delete wallet\n");
    walletHandle.closeWallet().get();
    Wallet.deleteWallet(walletConfig, walletCredentials).get();

    System.out.println("\n13. Close pool\n");
    pool.closePoolLedger().get();

    System.out.println("\n14. Delete pool ledger config\n");
    Pool.deletePoolLedgerConfig(poolName).get();

  }
}