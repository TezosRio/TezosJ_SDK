# TezosJ SDK library

The **TezosJ SDK library** enables Android Java developers to create apps that communicates with Tezos blockchain.

The library is written in Java and is based on Android framework. This repository contains the library source-code as well as a demo wallet application.

## Getting started

This is the TezosJ_SDK_Android source-code. This code GENERATES the TezosJ library. To create Android APPs (wallets) that uses this library you just have to add the line below to the build.gradle file on your Android APP (please check latest version):

compile 'com.milfont.tezos:tezosj_android:0.9.95'


## Disclaimer

This software is at Beta stage. It is currently experimental and still under development.
Many features are not fully tested/implemented yet. This version uses Tezos Mainnet.

## Resources
<!---
- [Full project documentation][doc-home] — To have a comprehensive understanding of the workflow and get the installation procedure
- [TezoJ Support Center][tezosj-help] — To get technical help from TezosJ
-->
- [Issues][project-issues] — To report issues, submit pull requests and get involved (see [MIT License][project-license])
- [Change log][project-changelog] — To check the changes of the latest versions

## Features

- Create valid Tezos wallet address
- Get account balance
- Send funds
- Originate KT addresses
- Delegate to a baker
- Undelegate
- Interact with POKT-Network API


The main purpose of TezosJ SDK library is to foster development of applications in Java / Android that interacts
with Tezos ecosystem. This might open Tezos to a whole world of software producers, ready to collaborate with the platform.
TezosJ is to play the role of a layer that will translate default Java method calls to Tezos's network real operations
(create_account, transfer_token, etc.).
-->

## Credits

- TezosJ is based on Stephen Andrews' EZTZ Javascript library  [https://github.com/stephenandrews/eztz](https://github.com/stephenandrews/eztz).  
- TezosJ is also based on ConseilJS from Cryptonomic [https://github.com/Cryptonomic/ConseilJS](https://github.com/Cryptonomic/ConseilJS)
- TezosJ uses Libsodium-JNI from Joshjdevl  [https://github.com/joshjdevl/libsodium-jni](https://github.com/joshjdevl/libsodium-jni).  
- TezosJ uses BitcoinJ Java Library  [https://github.com/bitcoinj/bitcoinj](https://github.com/bitcoinj/bitcoinj).   
- TezosJ uses NicoToast fat-aar plugin [https://github.com/NicoToast/fat-aar] (https://github.com/NicoToast/fat-aar)
- Special thanks to Tezzigator ([https://twitter.com/@tezzigator](https://twitter.com/@tezzigator)) for providing the code for Tezos Key Generation in Java. 


## License

The **TezosJ SDK library** is available under the **MIT License**. Check out the [license file][project-license] for more information.

[doc-home]: https://github.com/LMilfont/TezosJ/wiki

[tezosj-help]: http://help.android.com

[project-issues]: https://github.com/LMilfont/TezosJ/issues

[project-license]: LICENSE.md
[project-changelog]: CHANGELOG.md

