# Device Vitals

Device Vitals 1.6 is an offline dashboard for Android phones and tablets running Android 8.0 or later.

- **Memory.** See used and available RAM, and open Android’s app memory report. Android’s report averages usage over a selected period.
- **Storage.** See usage of the internal data volume and optionally rank the five largest app groups by app and data size. Apps sharing a system user ID are grouped and cache is included in data.
- **Battery.** See charge, power source, battery temperature, voltage, and the health status Android reports.
- **Cellular.** See the carrier, serving network, mobile data status and available signal readings for each active SIM or eSIM. The default data line is labeled. The active connection can show Wi-Fi, cellular, Ethernet, or VPN.
- **Satellite.** See whether Android reports satellite data and open Android’s satellite settings when available.
- **Device.** See uptime, model, Android version, security patch, processor architecture, core count, and display information.

The dashboard refreshes every two seconds while visible. Radio details depend on the device, Android version, and carrier. Enabled lines appear when Android reports them as active.

## Install the phone and watch apps

Download **DeviceVitals.apk** for Android and **DeviceVitals-Watch.apk** for Wear OS from [Releases](https://github.com/bizzarri/DeviceVitals/releases). Open the phone APK on your phone. To install on the watch, use [Wear OS wireless debugging](https://developer.android.com/training/wearables/get-started/debug-wifi) and run:

```powershell
adb -s WATCH_IP:DEBUG_PORT install -r DeviceVitals-Watch.apk
```

1. On the phone, open Device Vitals, tap **Connect Pixel Watch**, allow Nearby devices if prompted, and choose the paired watch.
2. On the watch, press the crown, open Device Vitals, tap **Choose phone**, allow Nearby devices if prompted, and choose the paired phone.
3. Keep the phone dashboard open. Tap **Refresh** on the watch for a fresh snapshot. Sharing stops when you leave the phone dashboard.

The companion uses authenticated Bluetooth between the selected paired devices. It shows the phone’s battery, RAM, storage, and cellular and satellite status.

## Privacy and permissions

Readings stay on your devices. The app has no internet permission or analytics.

- **Usage access** is optional. Enable it from the top storage users screen to read per-app storage sizes. Android also exposes installed-app visibility for that list.
- **Phone permission** is optional. Use **Allow access to show all active SIMs** in Cellular Connectivity to list active SIMs and eSIMs. Without it, the panel can display readings for the default data line when Android exposes them. The app does not read phone numbers or use SIM identifiers beyond selecting the local subscription for its readings.
- **Network state** lets the dashboard read the active connection and mobile data setting.

## Build and install

On Windows, install JDK 17 or later, Android SDK Platform 36, and Android SDK Build Tools 36.0.0. Use PowerShell from this directory:

```powershell
./build.ps1 -BuildTools 'C:/path/to/sdk/build-tools/36.0.0' -AndroidJar 'C:/path/to/sdk/platforms/android-36/android.jar' -JavaHome 'C:/path/to/jdk'
```

Repeat the build command with **-Watch** to produce **DeviceVitals-Watch.apk**. Use the same build directory to reuse the signing key.

The phone build writes **DeviceVitals.apk** one directory above the source. Copy the APK to an Android device, then open it to install. If Android prompts, allow installation from the app opening the APK. With Android Platform-Tools and USB debugging enabled, you can also use:

```powershell
adb install -r ../DeviceVitals.apk
adb shell am start -n com.mauri.devicevitals/.MainActivity
```

The script signs with a locally generated development key and verifies the APK signature and alignment. Keep your private development key to build future updates for that installation. An update must use the same signing certificate.

## Validation

Version 1.6 was compiled against API 36. The signed APKs were installed on a Pixel 9 and Pixel Watch 3. The watch received the phone snapshot and acknowledged it over Bluetooth. Battery and radio values vary by hardware.

## License

[MIT](LICENSE)
