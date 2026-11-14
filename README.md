# Device Vitals

Device Vitals 1.3 is an offline dashboard for Android phones and tablets running Android 8.0 or later.

- **Memory.** See used and available RAM, and open Android’s app memory report. Android’s report averages usage over a selected period.
- **Storage.** See usage of the internal data volume and optionally rank the five largest app groups by app and data size. Apps sharing a system user ID are grouped and cache is included in data.
- **Battery.** See charge, power source, battery temperature, voltage, and the health status Android reports.
- **Cellular.** See the carrier, serving network, mobile data status and available signal readings for each active SIM or eSIM. The default data line is labeled. The active connection can show Wi-Fi, cellular, Ethernet, or VPN.
- **Device.** See uptime, model, Android version, security patch, processor architecture, core count, and display information.

The dashboard refreshes every two seconds while visible. Radio details depend on the device, Android version, and carrier. Enabled lines appear when Android reports them as active.

## Privacy and permissions

Readings stay on the device. The app has no internet permission or analytics.

- **Usage access** is optional. Enable it from the top storage users screen to read per-app storage sizes. Android also exposes installed-app visibility for that list.
- **Phone permission** is optional. Use **Allow access to show all active SIMs** in Cellular Connectivity to list active SIMs and eSIMs. Without it, the panel can display readings for the default data line when Android exposes them. The app does not read phone numbers or use SIM identifiers beyond selecting the local subscription for its readings.
- **Network state** lets the dashboard read the active connection and mobile data setting.

## Build and install

On Windows, install JDK 17 or later, Android SDK Platform 36, and Android SDK Build Tools 36.0.0. Use PowerShell from this directory:

```powershell
./build.ps1 -BuildTools 'C:/path/to/sdk/build-tools/36.0.0' -AndroidJar 'C:/path/to/sdk/platforms/android-36/android.jar' -JavaHome 'C:/path/to/jdk'
```

The build writes **DeviceVitals.apk** one directory above the source. Copy the APK to an Android device, then open it to install. If Android prompts, allow installation from the app opening the APK. With Android Platform-Tools and USB debugging enabled, you can also use:

```powershell
adb install -r ../DeviceVitals.apk
adb shell am start -n com.mauri.devicevitals/.MainActivity
```

The script signs with a locally generated development key and verifies the APK signature and alignment. Keep your private development key to build future updates for that installation. An update must use the same signing certificate.

## Validation

Version 1.3 was compiled against API 36. The signed APK was installed on a Pixel 9 and verified to report both a physical SIM and an eSIM. Battery and radio values vary by hardware.

## License

[MIT](LICENSE)
