# you have stumbled upon the compose version of echo
still a wip

## Linux AppImage

Build the optimized, stripped x86_64 AppImage with:

```shell
./gradlew :desktop:packageReleaseAppImage
```

The output is written to `desktop/build/distributions/Echo-<version>-x86_64.AppImage`.
The build requires `appimagetool` and `strip` on the host.
