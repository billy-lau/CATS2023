/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.test.keyattestationidextractor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

public class MainActivity extends AppCompatActivity {
    /**
     * This activity should be triggered via adb using the following command:
     * adb shell am start -a android.intent.action.SEND -t text --es nonce <random_string> -n com.test.keyattestationidextractor/.MainActivity
     *
     * The resulting keychain certs can be pulled (on Pixel) using the following command:
     * adb pull /storage/emulated/0/Android/data/com.test.keyattestationidextractor/files/response attestation_certs
     */

    static final private String TAG = "KAIdEX";

    static final private String KEY_ALIAS = "test-key";
    static final private String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private KeyStore mKeyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Extracting nonce...");
        String nonce = extractNonceFromIntent();
        if (nonce == null) {
            Log.e(TAG, "Did not successfully extract nonce. Bailing...");
            finish();
            return;
        }
        Log.i(TAG, "Successfully extracted nonce: " + nonce);

        if (!generateKey(nonce.getBytes(StandardCharsets.UTF_8))) {
            // write something to storage
            Log.e(TAG, "Failed to generate key. Bailing...");
            finish();
            return;
        }

        if (!initializeKeyStore()) {
            Log.e(TAG, "Failed to initialize keystore. Bailing...");
            finish();
            return;
        }

        writeCertsToFile();
        finish();
    }

    @Nullable
    private String extractNonceFromIntent() {
        Intent intent = getIntent();

        String intentType = intent.getType();
        if (intentType == null || !intentType.equals("text")) {
            Log.e(TAG, "Type " + intentType + " is not text");
            return null;
        }

        Bundle extraBundle = intent.getExtras();
        if (extraBundle == null) {
            Log.e(TAG, "Failed to extract extras from intent");
            return null;
        }
        String nonce = extraBundle.getString("nonce");
        if (nonce == null) {
            Log.e(TAG, "Failed to extract nonce string from intent's extra");
            return null;
        }
        Log.d(TAG, "nonce: " + nonce);
        return nonce;
    }

    private boolean generateKey(byte[] nonce) {
        String tag = TAG + "-KeyGen";
        try {
            // First, create a key pair generator
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER);
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_SIGN)
                            .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                            .setDigests(KeyProperties.DIGEST_SHA256,
                                    KeyProperties.DIGEST_SHA384,
                                    KeyProperties.DIGEST_SHA512)
                            // Allow the private key to be used regardless of user authentication status
                            .setUserAuthenticationRequired(false)
                            .setAttestationChallenge(nonce)
                            .build());

            // now, use the generator to actually generate a key pair
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Log.d(tag, "KeyPair: " + keyPair.toString());
            PublicKey publicKey = keyPair.getPublic();
            Log.d(tag, "Public Key: " + publicKey.toString());

            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            Log.d(tag, "EC Public Key: " + ecPublicKey.toString());

            Log.d(tag, "Encoded Public Key: " + Utilities.convertBytesToHexString(publicKey.getEncoded()));


        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to find EC algorithm on this device.");
            return false;
        } catch (NoSuchProviderException e) {
            Log.e(TAG, "Failed to obtain AndroidKeyStore as provider on this device.");
            return false;
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Invalid algorithm when calling KeyGenParameterSpec.Builder:", e);
            return false;
        }
        return true;
    }

    private boolean initializeKeyStore() {
        try {
            mKeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            mKeyStore.load(null);
        } catch (KeyStoreException e) {
            Log.e(TAG, "Could not get AndroidKeyStore as keystore provider.", e);
            return false;
        } catch (CertificateException e) {
            Log.e(TAG, "Certificates in the key store could not be loaded.", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "There is an I/O or format problem with the keystore data.", e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "The necessary algorithm could not be found be found.", e);
            return false;
        }
        return true;
    }

    @Nullable
    String signData(@NonNull String data) {

        // before signing data, it needs to be hashed.
        byte[] hashedData = Utilities.computeSHA256DigestOfString(data);
        Log.i(TAG, "hashedData: " + Utilities.convertBytesToHexString(hashedData));
        if (hashedData == null) {
            Log.e(TAG, "Failed to hash the nonce");
            return null;
        }

        // get keypair from Android Keystore
        try {
            PrivateKey privateKey = (PrivateKey) mKeyStore.getKey(KEY_ALIAS, null);
            PublicKey publicKey = mKeyStore.getCertificate(KEY_ALIAS).getPublicKey();

            Log.d(TAG, "[KeyGet] Public Key: " + publicKey.toString());
            Log.d(TAG, "[KeyGet] Encoded key: " + Utilities.convertBytesToHexString(publicKey.getEncoded()));

            mKeyStore.getCertificateChain(KEY_ALIAS);

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(hashedData);
            byte[] signedNonce = signature.sign();
            Log.d(TAG, "[Signing] Signed hash(nonce) with: " + signature.getAlgorithm());
            return Utilities.convertBytesToHexString(signedNonce);
        } catch (KeyStoreException e) {
            Log.e(TAG, "Keystore has not been initialized?", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "The necessary algorithm could not be found be found.", e);
        } catch (UnrecoverableKeyException e) {
            Log.e(TAG, "Unrecoverable key error occurred.", e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "The key used to initialize signature might be invalid.", e);
        } catch (SignatureException e) {
            Log.e(TAG, "The signature object is not initialized properly.", e);
        }
        return null;
    }

    void writeCertsToFile() {
        String tag = TAG + "-writeCerts";
        try {
            Certificate[] certificates = mKeyStore.getCertificateChain(KEY_ALIAS);
            Log.d(tag, "There are " + certificates.length + " certs in this chain.");
            for (int i = 0; i < certificates.length; i++) {
                Certificate certificate = certificates[i];
                Log.d(tag, "Certificate " + i);
                Log.d(tag, certificate.toString());
                Log.d(tag, "--------------------");
                try {
                    Utilities.writeBytesToFile(getApplicationContext(), "certificate" + i + ".crt", certificate.getEncoded(), false);
                } catch (CertificateEncodingException e) {
                    Log.e(tag, "Failed to encode certificate: ", e);
                }
            }
        } catch (KeyStoreException e) {
            Log.e(tag, "Keystore has not been initialized?", e);
        }
        return;
    }
}