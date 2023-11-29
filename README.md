# CATS2023
A repository for all material presented during `The Odyssey: Android's First Firmware Binary Transparency via Pixel.` talk at the 1st Workshop on Cryptography Applied to Transparency Systems ([CATS2023](https://catsworkshop.dev/program/)).

A brief summary of what this repository contains:
- [slides](https://docs.google.com/presentation/d/1FGEpyknEcqsjllEjNXN5I56c4BDSspFOlISKDIyj3gc/edit?usp=sharing&resourcekey=0-cQBC3P28CdYFsB2tvDIWPg)
- [demo (youtube)](https://youtu.be/r-Hpd3FvnT0)
- patch for the [android-key-attestation](https://github.com/google/android-key-attestation) repository.
- link to the [Android Verified Boot (AVB)](https://android.googlesource.com/platform/external/avb) repository
- directory containing example implementation of an app that makes use of Android Key Attestatation.
  - contains source code that can be built using Android Studio
  - contains a pre-built **debug** app that can be a installed straight to an Android device using the following command `adb install -t KeyAttestationIdExtractor-debug.apk`
  - the app can be invoked/launched after installation using this command: `adb shell am start -a android.intent.action.SEND -t text --es nonce [insert_random_string] -n com.test.keyattestationidextractor/.MainActivity`
