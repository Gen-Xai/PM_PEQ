# PM_PEQ

PEQ Configurator for CrinEar Protocol Max.

A native Android application for configuring and saving the PEQ settings of the CrinEar Protocol Max via USB OTG.

## Supported Device

- CrinEar Protocol Max only

## Requirements

| Item | Requirement |
|---|---|
| minSdk | 26 (Android 8.0 Oreo) |
| targetSdk | 36 (Android 16) |
| USB | USB OTG-capable Android device required |

## Installation

- ~~Download the APK from [Releases](../../releases).~~ The APK is currently being prepared.
- To build from source, download the source ZIP and build the project.

## Usage

1. Connect the Protocol Max via USB OTG.
2. Adjust the parameters for each PEQ band.
3. Press the Save button to write the settings to the device and persist them to flash memory.

## Technical Details

- **Filter calculation:** Biquad transfer functions based on the RBJ Audio EQ Cookbook (`EqEngine.kt`)
- **Curve interpolation:** Fritsch–Carlson PCHIP for response curve rendering
- **Import/Export:** Equalizer APO text format compatible (`EqProfile.kt`)
- **USB communication:** USB HID Vendor Request with Report ID `0x01` and a 64-byte payload (`UsbHidTransport.kt`)

## Credits

The USB PEQ protocol implementation was developed with reference to [jeromeof/devicePEQ](https://github.com/jeromeof/devicePEQ), licensed under the 0BSD License.

## License

0BSD. See [LICENSE](LICENSE) for details.
