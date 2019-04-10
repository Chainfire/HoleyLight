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

There is a link to XDA thread at the bottom, which is the place for
discussing this app.

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
at least not without root. I've made some progress using shell runners
(which need to be started through adb, or root if you have it), but
nothing too convincing just yet.

The *Always On Display* mode (which is just Samsung-speak for 
Android's *Ambient Display* feature) does run in this mode, but 
decompiling AOD and several of it's plugins, I did not find a way
to *properly* hook into it. We *can* currently overlay it, but not
without it's own problems: we need to keep the CPU and screen
semi-active. In other words, it's not a true AOD tap-in, and uses
(significantly) more battery power than just AOD does.

For the devs among us, if you were unaware, *Ambient Display* is quite
literally just a Dream (with a few extra hidden methods to trigger *doze*
mode). The path to the Dream that runs as *Ambient Display* is hardcoded
in an xml in framework-res. With root and an rro overlay (or something 
similar) we might be able to replace it. I am not currently planning on 
rooting my S10, but it seems doable in theory. Typical Google to lock
something like this down while it could so easily be used for nice
things.

This app tries to piggyback on AOD smartly, and does not currently
keep any CPU wakelocks itself. The code that does run is pretty
heavily profiled, and the effects on other processes (such as 
system_server and surfaceflinger) closely monitored to pick the
best paths. Some things might seem slow in the code, but there are
actual reasons for most of these things.
  
Since the app cannot currently tap into the power management features 
directly, nor into the true AOD surface (hol*e*y grail material), it 
has to make do with several work-arounds and indirect triggers. This 
means at this level it can never be as efficient as AOD itself is 
(which impresses at sub-1% per hour).
 
Here's a potentially interesting and somewhat [in-depth post on XDA](https://forum.xda-developers.com/showpost.php?p=79303152&postcount=319).

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

### Translations

[app/src/main/res/values/strings.xml](./app/src/main/res/values/strings.xml) is the master English file that needs to be translated.  

### Enjoy!
Or not.
