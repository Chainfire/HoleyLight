**2021.03.02 - v1.00** - [RELEASE NOTES](./notes_100.md)
- (c) 2021
- Compatibility
    - Leverage Samsung firmware-specific camera area size, position, and animation: adds basic support for all Samsung devices
    - Added support for Google Pixel devices; currently 4A and 5 tested, 4A 5G needs a tester!
    - Specific adjustments for Samsung Galaxy Fold 2
- Donations
    - Added In-App Purchase donations - completely optional without nags. These do not unlock any additional features, change functionality in any way, or entitle the user to anything. Note that these only work if the app installed through Google Play.
- Android 11
    - Updated cutout positioning
    - Updated scaling math
    - Updated targetSdk
    - Added newly required permissions
    - Fixed accessibility service token error
    - Fixed Unholey Light not working at all
    - Split several code paths into Android 9, 10 and 11 specific versions
- Unholey Light
    - Add option to display icons inside circle (enabled by default)
    - Add option to display clock (disabled by default)
    - Smoother transitions
    - Rework of TSP area detection
    - Improved stability/reliability
    - Improve update speed on new notification
    - Fixed several (rare) internal crashes
    - Reduce display jumping around
- Hide AOD
    - Improved bottom area detection for partial hide
    - Added overlay linger option to reduce AOD flashing when going from screen off to lockscreen
- Notifications
    - Colors: Added fast scrolling capability
    - Colors: Added (long-press) option to respect or ignore (default) notification color state
    - Colors: Added save/load functionality
    - Timeouts: Added option to track timeouts separately for screen on and off states
    - Timeouts: Seekbars: Show value in title bar to improve UX
    - Persist seen state across resolution and density changes
    - Added black fill option to hide small misalignments in camera animations
    - Added tuning option to increase camera animation thickness
    - Rework dp adjustments math
    - Disable animation during phone calls to prevent some weird behavior
    - Detect and handle "silent" conversations
    - Detect and handle bubbles correctly
    - Detect and handle groups
    - Changed default color for notifications to the app icon's dominant color, with brightness and saturation maxed out
- AOD Helper
    - Complete overhaul
    - Now directly advertised and integrated into main settings rather than a hidden package deep in the XDA thread
    - Samsung: Still quirky and not fully compatible with all options - use with care!
    - Pixel: Added AOD brightness improvement; automatic AOD control not implemented (see AOD Helper source for notes)
    - Pixel: Automatically fix permissions with root, if available
- Translations
    - Many translations are now horribly outdated :( Please help!
    - Added German
    - Added Greek
    - Updated Chinese
    - Added option to switch language
- Debugging
    - Added master-switch long-press to enable super-secret debug mode
    - Added overlay debugging mode
    - Added (app-only) logcat dump
    - Built state tester (devs only)
- Notices and popups
    - Reworked various parts of the setup wizard
    - Added notice about location detection needing to be enabled for companion device permission
    - Samsung: Added option to preload black image into AOD rather than going the theme route
    - Samsung: Added informational notices about fingerprint icon on AOD
    - Samsung: Added informational notices about battery status on AOD
    - Pixel: Added informational notice about AOD brightness
- Miscellaneous
    - Reduced CPU and battery usage
    - Improve AOD handling in tap-to-show mode
    - Reworked resolution and density change detection and handling
    - Several small tweaks in settings display to improve uniformity
    - Improve performance of tuning adjustments
    - Fix broken dark mode support
    - Enabled ProGuard for release builds
    - Updated AndroidX dependencies
    - Updated graphics and screenshots for Google Play

**2019.12.27 - v0.67**
- Fixed Unholey Light display on Android 10
- Fixed build issue
- Translations: updated fr; added cn

**2019.04.25 - v0.66** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79406856&postcount=995)
- Adjust screen-off rendering (should fix animation disappearing for some users)
- Adjust AOD schedule handling
- Adjust default tune for S10+/X
- Fix color picker in night mode
- Translations: updated ru, pt, it; addes es; removed fr (outdated)

**2019.04.20 - v0.65** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79371779&postcount=866)
- Colors, new long-press options: disable, edit hex value, set color as default for app, apply color to entire app, copy/paste
- AOD hide: pick between full and partial hide, the latter showing charging information
- Seen: option to ignore notifications that arrive while the screen is on
- Seen: timeout options
- Schedule: show AOD schedule
- Do not disturb: option added to not show notifications when DND mode is enabled (default: enabled)
- Added Android night mode support
- Reduce animation jumping around in Unholey Light startup
- Fix some CPU usage issues in odd circumstances

**2019.04.18 - v0.61 - RELEASE NOTES (non-Play test version)**
- Translation updates: ru, pt
- Improved AOD blackout performance
- Lower CPU use with screen on
- (Experimental) landscape switch prevention

**2019.04.15 - v0.60** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79337377&postcount=639)
- Added setup wizards and assistants
- Collapsed several options into single options (less cluttered UI)
- Colors: add notification tickertext to display if available
- Colors: split into currently active and inactive notifications
- Split "Screen On" setting into battery and charging variants
- Added link to GitHub in app
- Show (user)name of translators in app
- Added (Brazilian) Portuguese (already outdated :))
- Adjusted AOD position size and follow logic
- Various crash fixes

**2019.04.13 - v0.55 - FULLDOZER** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79324816&postcount=555)
- Full doze mode "Unholey Light"

**2019.04.11 - v0.51** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79317643&postcount=481)
- AOD hiding

**2019.04.11 - v0.50** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79317016&postcount=471)
- Support for legacy notifications
- Fix 'phantom' notification
- Reduce Accessibility permissions to minimum
- Independent control of screen on/off modes
- Tuning now has 0.25dp granularity (all previous tuning is lost!), gained reset button
- Colors are now configured by channel rather than by package (all previous color config is lost!), gained refresh button
- Adjusted 'reverse portrait' rendering
- Added Italian and Russian languages
- Support resolution change
- Various efficiency, crash and deadlock fixes

**2019.04.10 - v0.40** - [RELEASE NOTES (XDA)](https://forum.xda-developers.com/showpost.php?p=79303152&postcount=319)
- Screen off + battery functionality enabled
- Migration from fake lockscreen to AOD-base
- Migration from application to accessibility overlay (Google frowns on this, though)
- Any notification can now be used for LED. The ones that don't ask for LED show up under Colors as black by default.
- Lottie renderer has been replaced by a sprite-sheet based rendered (up to 5x CPU reduction)
- Battery saving animations added (up to 3x CPU reduction), blinking and pie-chart (decided based on full or on-tap AOD)
- Several leaks and crashes have been fixed (and undoubtedly new ones created)

**2019.04.04 - v0.25**
- Notifications are now ignored if you set their color to black
- Battery optimization exemption permission is now required
- New icon/banner
- Fixed some CPU usage bug where the animation was running even if invisible
- Introduce "mark as seen" functionality, event and movement-based
- Added helper button to turn off overlay notification
- Rewording and repositioning of various UI elements

**2019.04.03 - v0.20**
- Fix compatibility with Qualcomm devices
- Add animation size/position/speed fine-tuning
- Add notification color override
- Clarify various error messages, popups, descriptions
- Show a test notification while the app's UI is open

**2019.04.02 - v0.10**
- Initial release