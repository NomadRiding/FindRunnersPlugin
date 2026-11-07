# Find Runners

A RuneLite plugin for friends-chat bone running, by 102Foxtrot.

This project is a renamed derivative of [Elrol/Run4LessPlugin](https://github.com/Elrol/Run4LessPlugin), which is licensed under BSD 2-Clause. The original copyright notice is kept in `LICENSE`.

## Features

- `!bones afk {amount}` and `!bones tick {amount}` price estimates in friends chat
- `@runner` screen flash for ranked runners
- Right-click **Set as Client** and **Remove as Client**
- Optional friends-chat overlay above the chat box
- Trade screen left-click set to Offer-all
- Side panel with a bone calculator, run manager, and local run history

Run history is saved on this computer, in the plugin's own data folder.

The original plugin also showed a host list by signing into a private database. That connection is not part of Find Runners.

## Run it

From this folder:

```
gradlew.bat run
```

That opens a RuneLite client in developer mode. Log in with your Jagex account, then enable **Find Runners** in the plugin list. Set **Friends chat owner** to the owner of the chat you run in.
