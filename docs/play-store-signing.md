# Play Store Signing

This project signs Play Store release bundles with a local upload key.

Do not commit these files:

- `release-upload-key.jks`
- `*.jks`
- `*.keystore`
- `keystore.properties`

They are intentionally ignored by Git. Keep the real keystore password in a password manager or another private store.

The file that can be uploaded to Google Play for an upload-key reset is the public certificate:

- `upload_certificate.pem`

To check which key Google Play should expect:

```bash
keytool -printcert -file upload_certificate.pem
```

To check which key Gradle is using:

```bash
./gradlew signingReport
```

To build the release Android App Bundle:

```bash
./gradlew bundleRelease
```

The generated bundle is:

```text
app/build/outputs/bundle/release/app-release.aab
```

If building in GitHub Actions, store the keystore and passwords as GitHub Actions Secrets, not as repository files.
