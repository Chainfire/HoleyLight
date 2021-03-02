Notice: the following texts were written some time ago with Samsung
devices in mind, and may be somewhat outdated. While things largely
translate to other manufacturers, there are some differences.

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

This app tries to piggyback on AOD smartly, and only keeps wakelocks
for a couple of milliseconds when screen updates are needed.
The code that does run is pretty
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

### Rendering

The camera-centric animations (swirl, blink, pie) render the original
AfterEffects animation (taken from Samsung ROM) using Lottie to a
spritesheet.

When the screen is on, all is well with that by default. In AOD mode,
with the screen in *doze* or *suspend* state, we require a *draw*
wakelock to have the screen updated, and we render continuously
or our animations disappear. It should be noted that we really shouldn't
even be able to hold a *draw* wakelock in the first place, but it *does*
work.

The *Unholey Light* display is different. AOD registers a section of
the screen that the display will keep visible (TSP) even when in *suspend*
state. This is not something we can achieve ourselves as we do not
have the correct access. We use *Accessibility Services* to detect
which section of the screen is registered, and render the *Unholey Light*
in that position. This way we do not need to keep rendering/updating
the screen continuously, and the CPU can go to sleep without the
display being lost.

Unfortunately, we have no control over the display area that is 'saved'
this way. That is why we cannot use a display around the camera area
in the same way, and why the camera-centric animations aren't nearly
as easy on the battery as *Unholey Light* - they need to keep the CPU
on and keep updating the screen.

As the CPU is asleep most of the time, this is also why *Unholey Light*
isn't really (and cannot be) animated.

It is interesting to note that Samsung is able to animate the display
while the CPU is asleep to some extent, but this uses a custom format
I haven't investigated at this time, and probably we don't have the
required access to upload it to the display anyway.

### Burn-in

Anything rendering in the same area runs the risk of burn-in on AMOLED
displays. While the situation is not as dire these days as the AMOLED
displays of old, the risk is still there.

Using the timeout feature to have notifications go away automatically
after some time is advised. It's rather wonky on short timeframes still,
but setting it to minutes/hours instead of seconds works well enough
for the time being.

*Unholey Light* has some protection against burn-in. First, AOD moves
the display area around the screen about once every minute. Usually its
just a few pixels, but every 10 minutes or so it moves the display to
a completely different area. Additionally, the app itself subtly changes
the radius of the ring, every time the CPU awakes (again about once
every minute on battery power, and continuously when charging), which
changes the pixels being used. You have to look very closely to observe
this effect. If multiple colors (notifications) are part of the display,
it also rotates them inside the circle, again hitting different
(sub)pixels.
