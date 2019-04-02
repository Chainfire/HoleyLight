This is the sauce for the [Holey Light](https://play.google.com/store/apps/details?id=eu.chainfire.holeylight) app.

[LICENSE](./LICENSE) is GPLv3.

---

Holey Light is an S10 Notification LED emulation app. It animates the
edges of the camera cut-out as replacement for the sadly missing LED.

It will only light up for notifications that would have lit up the LED
if the S10 had one.

This is for S10/S10e/S10+ only! It will crash (wont-fix) on any other
device!

My personal test devices is the *plain* S10, if things are out of
whack on the other models, let me know.

### Features

- Emulates notification LED

It really is a one-trick pony :) 

### Questions you could and/or should ask

#### Can I get this or that feature?

Maybe. I am building this for me, and you get to use it if you want.
If I want your feature for myself, I may add it. If not, you could
always **pull request** :)

The issue tracker is the TODO list, you might ask for it there.

#### Aren't there other apps that do this?

Maybe, yeah. But I got the idea for this the day I got the device, and
started soon after. Maybe I'm not first to finish, but finish it I will.

I am building this primarily for me, it will be running on my device
every day all day. So I want to know and control exactly what it does,
since anything untoward in the handling of this will drain your battery
quite quickly (6-8 hours).

Someone pointed me at another app but it was a monstrous ad-fest so
I uninstalled before even trying. Nope!

This is free, and you get teh sauce.

#### I heard you built this entire thing just so you could call an app "Holey Light"?

That is absurd. Who would even do such a thing. Certainly not me. /s 

#### Something something ETA?

No.

#### Why doesn't the animation show in the lockscreen?

It is currently not possible to arbitrarily draw over the lockscreen
for a standard third-party app, unless we replace the lockscreen in
its entirety. 

It *may* be possible to do this with root.

#### When I'm using the Screen off feature, my lockscreen behaves weirdly

Please describe exactly what happens and what you expected to happen.
As we're *faking* the screen being off, various events of the devices
turning on and off have to be faked as well. It might not always work
so well.

#### So what about LED emulation when the device is on battery power?

This isn't done yet. I've been trying different things to see their
effects, but I'm not quite satisfied with how things have worked out.

Currently, the animation is only shown when the screen is on, or when
the screen is off and the device is charging.

See the battery use section below.

### Battery use 

To display an animation like the one we're doing, we obviously need
the screen to be in some sort of ON state, or we'd just be staring
at a blank screen.

During normal (screen on) phone operation, this is not a problem. What
follows is precisely about the situation where the screen is "off" and
Android is not displayed.

Ideally, we would be operating in the *doze* or one of the *suspended*
states. These are power saving states, that reduce power used by the
CPU and/or display directly. Aside from that, code running in the
background is restricted. Unfortunately, it is not possible for
third-party apps such as these to attain these power-saving states,
at least not without root.

The *Always On Display* mode (which is just Samsung-speak for 
Android's *Ambient Display* feature) does run in this mode, but 
decompiling AOD and several of it's plugins, I did not find a way
to hook into it (or overlay it) properly.

For the devs among us, if you were unaware, *Ambient Display* is quite
literally just a Dream (with a few extra hidden methods to trigger *doze*
mode). The path to the Dream that runs as *Ambient Display* is hardcoded
in an xml in framework-res. With root and an rro overlay (or something 
similar) we might be able to replace it. I am not currently planning on 
rooting my S10, but it seems doable in theory. Typical Google to lock
something like this down while it could so easily be used for nice
things.

Since we are not able to run in or trigger any of the power saving
states we want to, we are left only with the standard wakelocks 
(and equivalent flags/attrs) to keep the CPU and/or screen awake.

While those methods are tested and true, implementing as an *Ambient
Dream* would be simpler logic (if it worked). And aside from not
running the CPU and display in a lower power state, other code running
in the background is not particularly restricted either. For all 
intents and purposes, the device is *fully* awake, even though it's
showing a black screen with only a small animation it.

Of course, since it's an AMOLED screen, the black pixels themselves
use practically no power, but the chips are still in full power mode. 
We're still drawing relatively large amounts of power compared to 
full sleep or even the *doze*/*suspended* states. Enough so that it 
can drain your battery from full straight to zero overnight.

None of this really matters while the device is charging, but it does
when it's on battery power.

I'm still thinking about and testing how to handle that case. The
most obvious solution would be to run only for a while after a new
notification comes in, or show the animation periodically. The best
parameters for that take some testing, and that is slow going when
you need to wait many hours each round to be able to compare the
results.

Of course, you can recompile it yourself, enable the feature, and 
see what happens. 

### Freedom!

This app is free, without in-app purchases (there may be a donate button at some point), without ads, without tracking, but *with* GPLv3 [sauce](https://github.com/Chainfire/HoleyLight).

### Download

You can grab it from [Google Play](https://play.google.com/store/apps/details?id=eu.chainfire.holeylight).

[Screenshot#1](https://lh3.googleusercontent.com/jzDVR2wFkO8rd9dgEP_Pg6PKo5EjlL-O8fjLR5Widw5b-M5sxBujj_gh8QEBcaxMfBk)

### Feedback

It puts it in the [XDA thread](https://forum.xda-developers.com/galaxy-s10/themes/app-holey-light-t3917675) or in the [GitHub issue tracker](https://github.com/Chainfire/HoleyLight/issues).

The workings of the app are quite intricate, so describe what is happening in minute detail.

### TODO

You can find the TODO list in the [issue tracker](https://github.com/Chainfire/HoleyLight/issues?utf8=%E2%9C%93&q=is%3Aissue).

### Enjoy!
Or not.
