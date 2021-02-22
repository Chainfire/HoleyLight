# Holey Light AOD Helper

The **AOD helper** package assists **Holey Light** in changing system
settings when needed. This provides additional options to the app,
depending on device.

This functionality cannot be built directly into the main app due to
both Android technical limitations and the relevant code violating
Google Play policy.

This package is aimed at power users and is **experimental**, some
things will certainly not work as expected.

## Installation

Download the APK (link below) and install it on your device.

You may get a popup that this app is built for an old Android version
and is dangerous. If you are worried about this, the source is available
right here on GitHub for your review.

You may need to enable permissions to install APKs in Android Settings
for Chrome, your Download Manager, your File Manager, or Developer
Options before Android allows you to install the file. The exact
permissions you need to change differ per device.

As this package is for power users, detailing all these possibilities
is beyond the scope of these instructions.

After installation, please close **Holey Light** and open it again.

## Upgrading

Usually, just downloading the APK (link below) and installing it
suffices. Sometimes you may need to manually uninstall the old version
of **Holey Light AOD Helper** (not **Holey Light** itself) for things
to work.

After upgrading, please close **Holey Light** and open it again.

## Permissions

On **Samsung** devices, no further permissions are necessary.

On **Google** devices, there are permissions that need to be granted.
If your device is rooted, the **Holey Light** app should provide you
with an option to use root to fix the permissions. If your device is
not rooted, you will need **adb** to set the permission manually.

How to setup **adb** is beyond the scope of this document (again, you're
supposed to be a power user). The command you need to execute in
**adb shell** is:

`pm grant eu.chainfire.holeylight.aodhelper android.permission.WRITE_SECURE_SETTINGS`

## Download

[Click/tap to download](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/apks/HoleyLight-AODHelper.apk)

## Implementation details

# Samsung

Currently allows control of **AOD** on/off state so the app can
completely shut it off to conserve power when no notifications are
active.

Note that your **AOD** configuration in Android Settings may be
disabled while this feature is on in the app.

# Google

Currently allows some control over the **AOD brightness**. It
doesn't go to full bright, but it does allow **AOD** to be
significantly more visible than without.

Control of **AOD** on/off state has been investigated but is currently
not working properly.

For the coders, my current thoughts on it can be found in [AODReceiver.java](https://github.com/Chainfire/HoleyLight/blob/master/helper/src/main/java/eu/chainfire/holeylight/aodhelper/AODReceiver.java#L100)
around line 100 at time of writing this.
