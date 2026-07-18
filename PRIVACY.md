# Privacy Policy

> [!NOTE]
> Vibrate Awake collects nothing, sends nothing, and shares nothing. It is an
> offline timer that vibrates your phone. Everything it does stays on your
> device.

Last updated: 2026-07-18

---

## The short version

Vibrate Awake is a vibration timer to help keep a drowsy driver awake. It does
not collect, store off-device, use, sell, or transfer any personal data. It has
no accounts, no analytics, no advertising, and no network access.

---

## What we collect

Nothing. The app gathers no personal information, no usage data, no device
identifiers, and no location. It contains no analytics, no crash reporting, no
advertising, and no tracking SDK of any kind.

The app declares no internet permission, so it cannot send anything to the
internet, even in principle. The one component that talks to another device is
the Google Play Services Wearable link used to reach a paired Wear OS watch,
described below. It carries only your vibration settings, only to your own
watch, and never to any server.

---

## What stays on your device

Your four settings (interval, vibration rhythm, intensity, and length) are saved
locally so the app remembers your last choices. This preference file lives in
the app's private storage on your phone and is removed when you uninstall the app.

If you use the Wear OS companion, those same settings are sent to your paired
watch so it can vibrate on the same schedule. They travel over the local Google
Play Services device-to-device link, are stored in the watch app's private
storage, and go no further. No settings, and no other data, ever reach a server
or any third party.

---

## Permissions and why

Each permission serves the core timer, not data collection.

- **Vibrate.** Drives the vibration motor.
- **Wake lock.** Keeps the processor awake so the timer fires on schedule while the screen is off.
- **Foreground service (and special-use type).** Lets the alert keep running while the app is in the background or the phone is locked.
- **Post notifications.** Shows the ongoing "Vibrate Awake is active" notification while the timer runs.
- **Request ignore battery optimizations.** Lets the app ask you to exempt it from battery restrictions so the system does not throttle the timer. You can decline, and the app still runs.

None of these permissions read contacts, files, location, or any personal data.

The Wear OS companion uses the same kinds of permissions on the watch (vibrate,
wake lock, foreground service, and an exact-alarm permission that drives its own
timer), all for the same purpose and none for data collection.

---

## Ads, tracking, and third parties

There are no ads, no tracking, no in-app purchases, and no analytics or
advertising services. The only third-party component is Google Play Services,
and only its Wearable API, used solely to relay your vibration settings to a
paired watch over the local device link. No data is shared with any server or
outside party, because none is collected and nothing leaves your own devices.

---

## Children

Vibrate Awake is rated for Everyone and is safe for all ages. It contains no
age-restricted content and collects no data from anyone, including children
under 13.

---

## Changes to this policy

If the app ever changes what it does with data, this policy will be updated
before that change ships, and the "Last updated" date above will change with it.

---

## Contact

Questions about this policy can go to the project's GitHub issues, or by email to
x@xero.style.
