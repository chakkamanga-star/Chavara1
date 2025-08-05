# Video Setup Instructions

To properly set up the splash screen video:

1. Move the file `app/src/main/res/drawable/11.mp4` to `app/src/main/res/raw/`
2. Rename it to `splash_video.mp4`
3. Update the SplashScreen.kt file to reference `R.raw.splash_video`

## Why this is needed:
- MP4 files should be placed in the `res/raw/` folder, not `res/drawable/`
- The `raw` folder allows direct access to the file without Android processing it
- Numeric filenames like "11" are not recommended for Android resources

## Current State:
The splash screen will show a white background. Once the video is moved and the reference is updated, it will display the video as intended.

## Code change needed in SplashScreen.kt:
Replace:
```kotlin
val uri = Uri.parse("android.resource://${context.packageName}/drawable/11")
```

With:
```kotlin
val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
```
