This is the sauce for the [Holey Light](https://play.google.com/store/apps/details?id=eu.chainfire.holeylight) app.

[LICENSE](./LICENSE) is GPLv3.

---

**Holey Light** is a LED emulation app. It animates the edges of the
camera cut-out (AKA punch-hole) as replacement for the sadly missing
LED on many modern devices.

Additionally, it provides a notification display for when the screen is
"off", replacing - or working in conjunction with - the *Always-On Display*
feature. As this display is not around the camera hole, it is aptly named
the ***Un*holey Light**.

While it was originally written for the Samsung Galaxy S10, the latest
version at the time of writing supports *all* Samsung devices and
several Google Pixels as well (as long as they have a camera hole inside
the screen).

My personal test devices is the *plain* S10, if things are out of
whack on the other models, let me know.

There is a link to XDA thread at the bottom, which is the place for
discussing this app. There's also a [Telegram channel](https://t.me/joinchat/I8HshhIHzwrJalUJ)
where I sometimes even respond.

### Features

- Emulates notification LED
- Four different display modes: *Swirl*, *Blink*, *Pie*, *Unholey Light*
- Configurable animation size, position, and speed
- Customizable color for each notification channel
- Selects initial notification color by analyzing dominant color of app icon
- Displays during screen "off", sub-1% battery use per hour in *Unholey Light* mode
- Separate configuration modes for different power and screen states
- Ability to mark notifications as seen based on various triggers
- Respects Do-Not-Disturb and AOD schedules
- Can hide AOD completely, partially, and/or keep the clock visible
- Additional *AOD Helper* package for further battery usage reduction

### Questions you could and/or should ask

#### Can I get this or that feature?

Maybe. I am building this for me, and you get to use it if you want.
If I want your feature for myself, I may add it. If not, you could
always **pull request** :)

The issue tracker is the TODO list, you might ask for it there.

#### Aren't there other apps that do this?

Yes there are. **Holey Light** was one of the first few, and certainly
*the* first to exploit Samsung-specific power saving modes.

This was years ago now. Today there are probably a dozen apps that do
the same thing with various amounts of success, compatibility, tracking,
ads, and payment.

This one is mine, this one is free, and you get teh sauce.

#### I heard you built this entire thing just so you could call an app "Holey Light"?

That is absurd. Who would even do such a thing. Certainly not me. /s 

#### Something something ETA?

No.

### Battery use

**Holey Light** tries to use as little battery as possible. Various
different modes use different amounts. The **Unholey Light** (which
as the name implies, is *not* centered around the camera hole) uses
least by staying on-screen without the CPU having to be awake.

See [tech.md](./docs/tech.md) for a more in-depth description.

### Rendering

Entire section moved to [tech.md](./docs/tech.md)

### Burn-in

Both the app and Android shift pixels around to reduce risk of burn-in.

See [tech.md](./docs/tech.md) for a more in-depth description.

### Freedom!

This app is 100% free, without ads, without tracking, but *with* GPLv3 [sauce](https://github.com/Chainfire/HoleyLight).

In-app purchases exist to donate if you are so inclined, this does not unlock additional features, change app behavior, or entitle you to anything.

### Changelogs and release notes

See the [changelog](./docs/changelogs.md) page.

### Download

You can grab it from [Google Play](https://play.google.com/store/apps/details?id=eu.chainfire.holeylight).

Alternatively, APKs are available from the [APK](./apks) directory, both
for the main APK (which may become outdated here) and the [AOD Helper](./apks/AODHelper.md)
package (which must stay updated because this is its main distribution
channel).

Screenshots:
[Full options](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/full_options.jpg) -
[1](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_1.png) -
[2](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_2.png) -
[3](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_3.png) -
[4](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_4.png) -
[5](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_5.png) -
[6](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_6.png) -
[7](https://raw.githubusercontent.com/Chainfire/HoleyLight/master/graphics/play/screenshot_v1.00_7.png)

### Feedback

It puts it in the [XDA thread](https://forum.xda-developers.com/galaxy-s10/themes/app-holey-light-t3917675) or in the [GitHub issue tracker](https://github.com/Chainfire/HoleyLight/issues).

The workings of the app are quite intricate, so describe what is happening in minute detail.

### TODO

You can find the TODO list in the [issue tracker](https://github.com/Chainfire/HoleyLight/issues?utf8=%E2%9C%93&q=is%3Aissue).

### Translations

[app/src/main/res/values/strings.xml](./app/src/main/res/values/strings.xml) is the master English file that needs to be translated.

I add strings in chronological orders, so you can just look at the bottom of the file for new entries.

### Enjoy!
Or not.
