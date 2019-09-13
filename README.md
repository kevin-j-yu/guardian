
# rideOS Android SDK
## Overview
The rideOS Android SDK provides foundations for running rider and driver applications for ride-hail using the rideOS APIs. These applications are intended to work out of the box, but with flexibility to customize easily.

## Layout  
The SDK is broken up into several libraries. There are 3 core libraries that are used:  
- `common` - the common library contains utilities and shared resources between the rider and driver apps  
- `rider` - the rider library has all of the view, controller, and navigation classes to run a rider app. Note that this doesn't actually create or run an app.
- `driver` - the driver library has all the view, controller, and navigation classes to run a driver app. It also doesn't actually run anything.
- `google` - the google library includes implementations for classes using the Google API and Google Maps products  

## Running the Apps
Provided in this library are the `example_rider_app` and `example_driver_app`. These should serve as a jumping off point for creating an application.

### Creating a fleet
Before you start hailing rides, you'll need to create a default fleet for your riders and drivers to operate on. The easiest way to do this is to grab your rideOS API key from [https://app.rideos.ai/profile](https://app.rideos.ai/profile). Once you have the API key, fill it into the following request, along with your intended fleet's ID.

```bash
curl -H "Content-Type: application/json" \
    -H "X-Api-Key: YOUR_API_KEY" \
    --data '{"id": "YOUR_FLEET_ID"}' \
    https://api.rideos.ai/ride-hail-operations/v1/CreateFleet
```
Then, fill in your default fleet id in [example_driver_app/src/main/AndroidManifest.xml](example_driver_app/src/main/AndroidManifest.xml) and [example_driver_app/src/main/AndroidManifest.xml](example_rider_app/src/main/AndroidManifest.xml)

### Creating an Auth0 client
To run either of these applications, you will need an Auth0 client ID and database connection. We use Auth0 to authenticate users into our ride-hail endpoints, and we don't currently support 3rd party authentication. Please [contact our team](mailto:support@rideos.ai) to create the ID and user database.

Once you have this information, fill in the relevant string resources in [example_rider_app/src/main/res/values/strings.xml](example_rider_app/src/main/res/values/strings.xml) and [example_driver_app/src/main/res/values/strings.xml](example_driver_app/src/main/res/values/strings.xml)

### Creating necessary 3rd-party API keys
Additionally, the example rider and driver app will require a Google API key. This key is used to use Google services such as location autocompletion, geocoding, and the map. Please follow [these instructions](https://developers.google.com/maps/documentation/android-sdk/get-api-key) to get a key. Then, add these key to [example_rider_app/src/main/AndroidManifest.xml](example_rider_app/src/main/AndroidManifest.xml) and [example_driver_app/src/main/AndroidManifest.xml](example_driver_app/src/main/AndroidManifest.xml).

Lastly, the example driver app requires a Mapbox access token for turn-by-turn navigation. Please follow [these instructions](https://docs.mapbox.com/help/how-mapbox-works/access-tokens/#creating-and-managing-access-tokens) to get an access token. Then, add this key to [example_driver_app/src/main/AndroidManifest.xml](example_driver_app/src/main/AndroidManifest.xml). 

With these parameters set, you can run the app, log in, and start servicing rides!

## Customizing the app

### Colors & Icons

The `common` library exposes [common themes](common/src/main/res/values/styles.xml) for the application. You can change the colors and images of the app by extending these themes and applying it to the relevant activity.

The following themes exist:
- `RideOS.MainTheme`- This theme covers all screens after the user has logged in, including requesting rides in the rider app and completing rides in the driver app. It can be applied to the [main rider activity](rider_app/src/main/java/ai/rideos/android/rider_app/MainFragmentActivity.java) or the [main driver activity](driver_app/src/main/java/ai/rideos/android/driver_app/MainFragmentActivity.java)
- `RideOS.LoginTheme`- This theme covers the launch flow of the apps, including the splash screen icon. It can be applied to the [rider launch activity](rider_app/src/main/java/ai/rideos/android/rider_app/launch/LaunchActivity.java) or the [driver launch activity](driver_app/src/main/java/ai/rideos/android/driver_app/launch/LaunchActivity.java)
- `Lock.Theme` - This theme is imported from the Auth0 Lock Library and applies to the Auth0 login screen. It is already implemented in the `example_rider_app` and `example_driver_app`, and it applies to `com.auth0.android.lock.LockActivity`

As an example, if you want to customize the color of an active route to purple in the `example_rider_app`, you would add the following to your `styles.xml` file.

```xml
<style name="YourCustomTheme" parent="RideOS.MainTheme">  
    <item name="rideos.route_color">@android:color/holo_purple</item>  
</style>
```

Then, in the application's `AndroidManifest.xml` file, change the declaration of the `MainFragmentActivity` to:
```xml
<activity android:name="ai.rideos.android.rider_app.MainFragmentActivity"  
  android:theme="@style/YourCustomTheme"  
  android:screenOrientation="portrait"  
>  
</activity>
```

### Feature Flags
There are a few feature flags we expose for common customizations to the behavior of the app. These feature flags should be added as metadata values to the appropriate app.

| Key Name                      | Application(s) | Values     | Description                                                                               |
|-------------------------------|----------------|------------|-------------------------------------------------------------------------------------------|
| `enable_developer_options`      | Rider & Driver | true/false | Enables the developer options menu item in the side menu                                  |
| `rideos_disable_seat_selection` | Rider          | true/false | Disables the rider from selecting how many seats the trip requires                        |
| `rideos_fixed_locations`        | Rider          | true/false | Forces the rider app to use discrete, fixed stop locations instead of normal coordinates. |

## Architecture

Please see the separate [architecture documentation](docs/architecture.md).

## Licensing

### Code

All code is distributed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

### Assets

All assets (`*.png`, `*.jpg`, etc.) are distributed under the [Creative Commons Attribution 4.0 International](http://creativecommons.org/licenses/by/4.0/) license.

* Artist: Yen Ma
* Copyright: rideOS

