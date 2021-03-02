## Holey Light v1.00 release notes

This is by far the biggest update to *Holey Light* since its inception
in early 2019. Early February '21 my S10 decided to update itself to
Android 11 and completely broke the app for me. As I use this app myself
every day, it had to be updated. What was supposed to be a small tweak
however quickly became a massive update as testers came in, features
and device-compatibility were requested, and new ways of doing things
were discovered.

The full and rather vast changelogs can be found [here](./changelogs.md); these are
the highlights:

- Android 11 support
- Support for all Samsung devices rather than just the S10 family;
testers have reported success on the S20, Note20, and S21 families, as
well as the Fold2 and various budget models
- Support for Google Pixel devices (4A and 5 completed, 4A 5G still needs
a tester *and feedback*)
- Added options to show app icons and the AOD clock to *Unholy Light* mode
- Thickness of the camera notification animation is now configurable
- Added handling of conversation and bubble notifications
- Initial notification color now based on app icon's dominant color
- Improved stability and reliability, and further reduced CPU and battery usage
- Added completely optional and nag-free donation option
- Dark mode support now actually works
- German and Greek translations were added, and the Chinese translation
was updated; the other translations are unfortunately horribly outdated
now (send help!)
- Moved from *beta* to *production* on Google Play. About time after
two years, though I guess this leaves the app with 0 review and ratings...

A special thanks to the 100+ testers who gave my works-in-progress a go
over the past month, and in particular the dozen or so truly hardcore
supporters who stuck with me through the entire process and/or updated
translations. You know who you are!

Downloads: [GitHub direct](https://raw.githubusercontent.com/Chainfire/HoleyLight/22c8a05fda5292115daa9413267fd33bdbe7c746/apks/HoleyLight.apk) - [XDA](https://forum.xda-developers.com/t/app-2019-02-10-holey-light-s10-notification-led-v0-70-beta.3917675/) - [Google Play](https://play.google.com/store/apps/details?id=eu.chainfire.holeylight&hl=en&gl=US)

Following is a far more in-depth story about the changes, covering some
technical details as well. Perhaps a little long and relatively dry, but
writing it out feels like *finishing* a release to me.

### Initial update

The initial update to support Android 11 was done relatively quickly. A
few days fighting Android's API and permission changes and deprecations,
handling Samsung's AOD changes, and that was that (or so I thought at
the time).

Since Android 10, there was however an issue that the *Unholey Light*
display would stop rendering correctly after a while. I decided to see
once again if I could pin down the cause and fix it. This led me down
a rabbit hole of Samsung firmware internals and I eventually found the
fix (which as to be expected ended up being a one-liner).

Meanwhile, several testers had come in with newer Samsung devices, and
some Pixels users too; the latter of whom I *rightfully* chastised for
expecting a Samsung S10 specific app to work on a completely different
device by a different manufacturer, this could *never* work... :)

#### Supporting 'all' Samsungs

I wasn't planning to hardcode support for all these other Samsung
devices as I had done for the S10, as that sounds suspiciously like
work. However, during my trip down decompiler's lane to fix that
*Unholey* rendering issue, I ran into Samsung's internal API to display
the face detection swirl on the lockscreen again (where *Holey Light*'s
animation originally comes from).

I had seen this code before, but for whatever reason at the time it
didn't click how to leverage it (camera punch-hole Samsungs were a
relatively new thing then, or insert another excuse here). This time
around I decided to properly investigate.

I ended up with the solution to load the relevant classes from the
Samsung in-firmware APK into *Holey Light* at runtime, and querying them
for the information the app needs to display the right animation in the
right place.

This is not without its issues. It relies on reflection (Google no like)
and the relevant code wants to resolve classes and fields that are not
publicly accessible and blocked by Android's newest safeguards. It also
seems for the newest firmwares, Samsung rewrote the relevant code in
Kotlin which changes some handling.

With some copy/pasting (with minor adjustments) of some decompiled
methods, smart reflection, and creating a few shims (exploiting
Android's runtime class linking) this actually ended up working
remarkably well.

It should however be noted that this can break at any time and any
Samsung firmware update. Additionally, at this point only the normal
(and by far most used) circular and (S10+) circurectangular (//TODO add
to dictionary) animations have been tested. The decompiled source implies
there are other deviants that have not been tested and may not work well.

I could not have done this without the army of testers trying this
unstable-by-definition code on many different devices and firmwares and
reporting where it breaks.

However, Samsung's provided values do not actually cause the animation
to hug the camera with pixel precision: the result was less *tight*
than *Holey Light*'s old hardcoded values. Rather than testing every
single device and firmware and hardcoding adjustments or relying on
every user to manually tune their settings, the *black fill* option was
created (enabled by default) which fills the inside of the circular
animation with black pixels, so you don't actually see if it's off by
a few pixels. So actually, it doesn't work that well, I'm just deceiving
you into believing that it does :)

Advanced users can of course go into the *Tune* section of the app and
manually improve the situation (*black fill* is automatically disabled
during tuning).

Honorary mention for tester *Mohamed LastNameCensored*, who armed with a
Fold2 single-handledly discovered such a large number of edge-case
issues in all-Samsung support, that the frustration-induced hair-pulling
during analysis and code adjustment might just have negated the need
for a post-COVID-lockdown de-yeti-fication haircut on my end.

#### Supporting Pixels

After relentlessly chastising Pixel users for their entitlement issues
and inability to read my notice about the app being both *Samsung* and
*S10* only, one of them dared ask *but why* the Pixel 5 wasn't
supported. It was quickly determined all my *reasons* were in fact
*excuses* and the challenge was accepted.

Samsung's *Always-On Display* is of course largely based on Google's
*Ambient Display* feature, so ultimately the amount of code that had to
be adjusted wasn't that large. It mostly came down to testing.

For Pixels, there is no way (that I know of) to get the *exact* camera
position as there is (now) for Samsung, which means I once again
resorted to hardcoding tuning values. For the Pixel 4A and 5 these
values are included in the app already. Other Pixels (such as the 4A 5G)
will require users to *Tune* manually. If they inform me of the values
that work best for them, I can include them in the next update.

I tested myself using a Pixel2XL modded to present itself to the app as
a Pixel 4A. Curiously in testing I noticed that the P2XL uses
significantly less power than my S10 to keep AOD running (tested
without *Holey Light* active), even though the Pixels persist the entire
screen in *doze* mode while Samsungs only persist a small area (I'm not
even sure that specific difference should matter for power draw though).
Of course that is just *one* specific observation, it might not hold up
comparing for example an S21 to a Pixel 5.

#### Supporting other devices

Now that Pixel support is implemented, supporting other brands - as long
as their *Ambient Display* implementation is close to Google's original -
should not be terribly difficult. This will however require *some* code
changes and thus an active tester. The app as-is will refuse to do its
thing.

There was also a request to allow *Holey Light* to work in *Unholey
Light* mode even for devices that do not have a camera punch-hole but
this is currently not supported nor on my todo list.

#### Changes to notification handling

I thought I was done with my Android 11 fixes, but a number of testers
reported issues with a number of notifications they had.

This already started on Android 10, but Android 11 changed even more
subtle things in how notifications work. Some apps also changed how
they used the notification light in the past year or two.

Quite a bit of work went into figuring out how grouping, conversations,
and bubbles operated. Ultimately the fixes were small in lines of code,
but large in testing time.

Particularly the handling of bubbles (looking at you, Messenger!) took
a number of tries to get right (I hope). As I only have one friend and
he sneers at all social media use, some of these issues were not
apparent in my own tests. Luckily, tester *Thibault
LastNameCensored* (no relation to *Mohamed* as far as I know) and his
vast collection of chatty friends ran into *all of the issues* and
relentlessly slammed my dreams of things actually working for once.

#### Changes to notification colors

The initial color for previously unknown notifications has changed. The app
in general already didn't respect notifications' provided LED colors as
an absurdly large amount of apps use the same color (namely, white). In
previous versions of *Holey Light* in a number of situations the
notification's accent color was used instead of the LED color.

For this release I've taken it a step further and analyze the app's
launcher icon for (averaged) dominant color, then dial up both the
brightness and saturation of that base color to the maximum level. I
feel this generally creates a better looking and somewhat more diverse
palette of initial notification colors. The code is also more aggressive
in applying this color than in previous versions.

This more or less results in a notification's LED color to be ignored
and replaced by our own calculated color *by far* most of the time. For
the *power* users who customize notification LED colors in (for example)
WhatsApp itself to differentiate between different chats this is
slightly inconvenient as those LED colors are lost (though I still think
this is a better setup for the *average* user). However, these
customized notifications each create their own *notification channel*,
and thus they can be manually overridden again in the *Colors* section
of *Holey Light*.

#### Further issues

No doubt a massive update like this one will see more issues we haven't
run into during testing. Report them and maybe I'll look them :)

### EOF

Thank you for coming to my TED talk!
