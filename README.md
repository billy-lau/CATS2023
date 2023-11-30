# CATS2023
A repository for all material presented during `The Odyssey: Android's First Firmware Binary Transparency via Pixel.` talk at the 1st Workshop on Cryptography Applied to Transparency Systems ([CATS2023](https://catsworkshop.dev/program/)).

A brief summary of what this repository contains:
- [slides](https://docs.google.com/presentation/d/e/2PACX-1vRdqJPSuBAJRqqwamXcDguAKv8xJQIn1HNKLgMF8VqrZmOaqXLstilj0OY5oJGefUBtUz6RFGCVeNUk/pub?start=false&loop=false)
- [demo (youtube)](https://youtu.be/r-Hpd3FvnT0)
- [a patch](patches/print_hex_encoded_string_instead_of_base64.patch) that can be applied to the copy of the [android-key-attestation](https://github.com/google/android-key-attestation) repository.
- link to the [Android Verified Boot (AVB)](https://android.googlesource.com/platform/external/avb) repository from AOSP.
- [directory](KeyAttestationIdExtractor) containing example implementation of an app that makes use of [Android Key Attestation](https://developer.android.com/privacy-and-security/security-key-attestation).
  - contains source code that can be built using Android Studio.
  - contains a pre-built **debug** [app](KeyAttestationIdExtractor/prebuilt/KeyAttestationIdExtractor-debug.apk) that can be a installed straight to an Android device using the following command `adb install -t KeyAttestationIdExtractor-debug.apk`.
  - the app can be invoked/launched after installation using this command: `adb shell am start -a android.intent.action.SEND -t text --es nonce [insert_random_string] -n com.test.keyattestationidextractor/.MainActivity`.
  - the certificate chain can be pulled from the device (if Pixel) using this command: `adb pull /storage/emulated/0/Android/data/com.test.keyattestationidextractor/files/response attestation_certs`.
