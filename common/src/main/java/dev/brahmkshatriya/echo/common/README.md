# Common Package for Echo Extensions

This package contains common classes and interfaces that are used by all the extensions of Echo.

## Getting Started

### Template

To create a new extension, you can use the following template repo:

https://github.com/brahmkshatriya/echo-extension-template

### Manual

- Add the dependency in your project.

  settings.gradle.kts
  ```kotlin
   repositories {
      //...
      maven { url = uri("https://jitpack.io") }
  }
  ```
  build.gradle.kts
    ```kotlin
    implementation("com.github.brahmkshatriya:echo:-SNAPSHOT")
    ```

## How does it work?

Currently, the common package is JVM only. The app recognizes an App or an `apk` file as an extension by its `AndroidManifest.xml` file.
To determine what type of extension it is, the app looks for the following feature in the manifest file:
- For Music Extension:
  ```xml
  <uses-feature android:name="dev.brahmkshatriya.echo.music"/>
  ```
- For Lyrics Extension:
  ```xml
  <uses-feature android:name="dev.brahmkshatriya.echo.lyrics"/>
  ```
- For Tracker Extension:
  ```xml
  <uses-feature android:name="dev.brahmkshatriya.echo.trackers"/>
  ```

There are 2 ways the app can import an extension:
- File: The app stores the apk file in the internal storage and loads the class from the dex file.
- Installed Package: The app loads the class from the installed apps on the device.

Installed packages have priority over file based, if both extensions have same the id.

The `AndroidManifest.xml` file of the extension should contain the following metadata:
- class: The class path of the `ExtensionClient`.
```xml
<meta-data
    android:name="class"
    android:value="com.example.MyExtension" />
```

- id: The unique id of the extension.(Avoid using special characters or spaces)
```xml
<meta-data
    android:name="id"
    android:value="my_example" />
```
and [others...](https://github.com/brahmkshatriya/echo-extension-template/blob/main/app/src/main/AndroidManifest.xml)

### Flow of the app:
1. Load the extensions from the installed packages and internal storage using [Plugger](https://github.com/JeelPatel231/plugger).
2. Dynamically load the [`ExtensionClient`](./clients/ExtensionClient.kt) instance using the class path from the metadata.
3. Inject the extension with [`Settings`](./settings/Setting.kt) 
4. Inject the extension with other [provider interfaces](./providers)
5. The extension is available to use in the app.