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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utilities {

    /**
     * Computes the SHA256 digest of a given input string
     * @param input a String containing data to be hashed
     * @return A byte array representing the SHA256 digest of the content of the input string.
     * A <code>null</code> may be returned if any intermediate process failed.
     */
    @Nullable
    public static byte[] computeSHA256DigestOfString(String input) {
        final String TAG = "SHA256-Str";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA256");
            md.reset();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA256 isn't implemented on this device.");
            return null;
        }

        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get a storage directory suitable to write result files to, without acquiring additional
     * permissions.
     * @param context The current execution context.
     * @return a {@link File} object that points to the desired directory. NOTE: Remember to check
     *         if the directory exists before opening for read/write.
     */
    public static File getResultStorageDirectory(@NotNull Context context) {
        File rootDir = context.getExternalFilesDir(null);
        File resultDir = new File(rootDir, "response");
        return resultDir;
    }

    /**
     * Utility function to help flush contents to file within the app's external data dir.
     * @param context The execution context.
     * @param filename The name of the file to write to.
     * @param content A string containing the data to write.
     * @param append A boolean on whether to write to the end of the file or to overwrite the file.
     * @return <code>true</code> if the write operation is successful. <code>false</code> otherwise.
     */
    public static boolean writeStringToFile(@NotNull Context context,
                                            @NotNull String filename, String content,
                                            boolean append) {
        return writeBytesToFile(context, filename, content.getBytes(), append);
    }

    public static boolean writeBytesToFile(@NonNull Context context, @NonNull String filename, @NonNull byte[] content, boolean append) {
        final String TAG = "writeBytesToFile";
        File resultDir = getResultStorageDirectory(context);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }
        File dataFile = new File(resultDir, filename);

        try (FileOutputStream fos = new FileOutputStream(dataFile, append)) {
            fos.write(content);
        } catch (FileNotFoundException e) {
            Log.e(TAG, String.format("File %s is not found for writing: %s", dataFile.getAbsolutePath(),
                    e.getMessage()));
            return false;
        } catch (IOException e) {
            Log.e(TAG, String.format("Error while writing to file: %s", dataFile.getAbsolutePath(),
                    e.getMessage()));
            return false;
        }
        return true;
    }


    /**
     * Utility function to convert/encode bytes into hex string.
     * @param bytes The byte array to be converted.
     * @return A string corresponding to the @param bytes input.
     */
    @NonNull
    public static String convertBytesToHexString(@NonNull byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
