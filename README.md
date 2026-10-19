<div align="center">
  <img src="docs/assets/foyer.svg" width="88" height="88" alt="Foyer logo">
  <h1>Foyer</h1>
  <p><strong>A quieter home for your TV.</strong></p>
  <p>Your apps. Your layout. Straight to watching.</p>
  <p>Set it up with Codex or Claude Code. Use it with your remote.</p>
  <p>Android TV &amp; Google TV · Free and open source</p>
  <p>
    <a href="#before-and-after">See the difference</a> ·
    <a href="#get-started">Get started</a> ·
    <a href="docs/INSTALL.md">Install &amp; recovery</a> ·
    <a href="https://github.com/bankerrahul/foyer/issues">Report an issue</a>
  </p>
</div>

![Foyer home screen on an XGIMI HORIZON Ultra, showing Plex, TiviMate and YouTube above a row of utility shortcuts](docs/assets/home.png)

Foyer replaces the home screen on your Android TV or Google TV. Choose the apps you want to see, arrange them with your remote, and get straight to watching. Large tiles and shortcuts to HDMI inputs and settings keep everything within reach.

**No ads, account sign-ups, or tracking.** Once installed, Foyer works offline. You only need the AI assistant for setup.

## Before and after

| Original Home | Foyer 0.5.0 |
| :---: | :---: |
| [![Original XGIMI home screen with promotional content and recommendation rows](docs/assets/home-before.png)](docs/assets/home-before.png) | [![The same XGIMI with Foyer, three chosen apps and utility shortcuts](docs/assets/home.png)](docs/assets/home.png) |
| Promotional banners and content rows | Your chosen apps and shortcuts |

Real screenshots from the same XGIMI HORIZON Ultra, before and after Foyer. Click either image to enlarge it. The three apps shown are a personal choice—Foyer finds the apps already on **your** TV.

## Make room for what you watch

| Feature | What you can do |
| --- | --- |
| Choose your Home | Show the apps you want, reorder them with the remote, and hide unwanted tiles without uninstalling anything. |
| Keep every app within reach | Open **All apps** for a scrollable logo grid, including apps hidden from Home. Add them back whenever you like. |
| Make it yours | Adjust tile size, spacing, background, app names, date, and clock. Choose **Minimal** for large tiles on black. |
| Jump to HDMI | Open the HDMI inputs your TV makes available. Give their shortcuts names and icons you recognize, or hide the ones you do not use. |
| Keep your setup | Your app choices and layout are saved on your TV and kept when Foyer is updated. |
| Return to the previous Home | Ask your AI assistant to bring back the home screen you used before Foyer. |

<table>
  <tr>
    <td width="50%"><img src="docs/assets/minimal.png" alt="Minimal preset with large app tiles, visible names and a black background"><br><strong>Minimal</strong><br>Just the essentials, with the clock and date hidden.</td>
    <td width="50%"><img src="docs/assets/manage-home.png" alt="Manage Home settings with app visibility checkboxes, move controls and undo"><br><strong>Manage Home</strong><br>Choose apps, change their order, and undo a removal.</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/assets/appearance.png" alt="Appearance settings for tiles, spacing, background, app names, date and clock"><br><strong>Appearance</strong><br>Make the layout comfortable from your sofa.</td>
    <td width="50%"><img src="docs/assets/inputs.png" alt="Inputs settings showing a registered HDMI port"><br><strong>Inputs</strong><br>Arrange shortcuts for your available HDMI inputs.</td>
  </tr>
</table>

Settings and Minimal screenshots were captured on a Sony BRAVIA VH1 running Foyer 0.5.0. Apps and available inputs vary by device.

## Get started

**Enable debugging on your TV, then let Codex or Claude Code handle setup.** The assistant takes care of the computer commands and walks you through any prompts on your TV.

1. **Prepare your TV.** Open Settings → About (sometimes under System or Device Preferences), select **Build** seven times, then enable **USB debugging** in Developer options. Enable **Network debugging** or **Wireless debugging** too if your TV offers it. Leave the TV on and note its IP address from network settings.
2. **Open Codex or Claude Code on your computer.** Run it locally so it can connect to the TV, and keep both devices on the same home network. Wi-Fi or Ethernet is fine. The assistant will help with pairing or a USB connection if your TV needs one.
3. **Paste this prompt.** Fill in your TV details, or let the assistant help you find them.

```text
Set up Foyer as the home screen on my Android TV.
Repository: https://github.com/bankerrahul/foyer
TV model: <brand and model>
TV address: <IP address, or help me find it>

Follow the repository's AI setup guide in docs/AI-SETUP.md.
Handle the commands and guide me through any TV prompts.
Only change my home screen; keep my apps, data and TV settings.
Ask before turning off the TV's existing home screen app.
Check that my apps and Home button work, and save instructions
for switching back to my previous home screen.
```

Approve the connection prompt on your TV when it appears. Some TVs need an extra step to make the Home button open Foyer; the assistant will explain it and guide you through it.

**Keep the Foyer setup folder on your computer.** The assistant saves what is needed there for future updates and for switching back to your old home screen. If the TV menus look different, tell the assistant your model and ask for help.

## Use your remote

| Action | Control |
| --- | --- |
| Navigate and open an app | Arrow keys, then **OK** |
| Reorder Home | Hold **OK** on an app → **Move** → arrows → **OK** to save, **Back** to cancel |
| Hide a Home tile | Hold **OK** → **Remove from Home** |
| Restore a hidden app | **All apps** → hold **OK** on it → **Add to Home** |
| Undo the last removal | **Customize** → **Manage Home** → **Undo removal** |
| Reset focus on Home | Press **Back** to focus the first visible app |
| Change the layout or inputs | **Customize** in the utility row |
| Open Android's device settings | **Settings** in the utility row |

Your changes are saved automatically. Hidden apps stay available in **All apps**, and newly installed apps appear automatically. Pressing **Back** in All apps takes you home; pressing it during a move cancels that move.

## Update Foyer

Open the same Codex or Claude Code conversation on the computer you used for setup. Keep the TV on with debugging enabled, then paste:

```text
Check whether a newer version of Foyer is available for my TV.
Repository: https://github.com/bankerrahul/foyer
Use my existing setup folder and follow the update instructions
in docs/AI-SETUP.md. If there is an update, install it while keeping
my app order, hidden apps, layout, HDMI shortcuts and Home button
working as before. Keep my saved recovery information.
Do not uninstall or reset anything. If something needed for the
update is missing, explain it before making changes.
```

The assistant checks for an update, handles the commands, and checks that Foyer still works afterward. Keep your original setup folder so it can update Foyer without losing your settings.

## Return to your previous Home

Want your old home screen back? Open the same Codex or Claude Code conversation on the computer you used for setup, keep the TV on with debugging enabled, and paste:

```text
Switch my TV back to the home screen it used before Foyer.
Use the saved setup record and Foyer's recovery guide.
Turn off Foyer's Home button redirect if enabled, restore the
previous Home, and check that the remote's Home button opens it.
Leave my apps, their data, and Foyer installed.
If the saved record is missing, ask me before making changes.
```

Keep the Foyer setup folder on your computer—it contains the saved recovery information. The assistant handles the restore commands and guides you through any TV prompts.

Prefer to do it yourself? See the [manual recovery guide](docs/INSTALL.md#recovery) and [Google TV Home button details](docs/INSTALL.md#optional-google-tv-redirect).

## Compatibility

Foyer is made for Android TV and Google TV, including projectors that use them. Version 0.5.0 has been checked on:

| Device | Android version |
| --- | --- |
| XGIMI HORIZON Ultra | 11 |
| Sony BRAVIA VH1 | 12 |
| Sony BRAVIA VU3 | 14 |

Other models may need extra setup. The Home button, starting up after a restart, and available HDMI shortcuts can vary by TV. Foyer can only show the inputs the TV makes available.

Samsung TVs using Tizen and LG TVs using webOS cannot run this Android app. Fire TV has not been tested. See the [testing notes](docs/VALIDATION.md) for details about the devices and features checked so far.

Foyer changes your home screen. Your other apps stay installed, and your picture and HDMI settings stay as they are. Ads inside streaming apps are unaffected. A simpler home screen does not guarantee a faster TV.

## Help and development

Need help or have an idea? [Open an issue](https://github.com/bankerrahul/foyer/issues) with your TV model, Android version, and what happened. Leave personal information out of screenshots or logs.

Want to install manually or work on Foyer? Use the [build and developer guide](docs/BUILD.md), [installation and recovery guide](docs/INSTALL.md), and [AI setup guide](docs/AI-SETUP.md).

## License

Foyer is open source under the [MIT license](LICENSE). Keep the original license and copyright notice when sharing the code. Third-party app names, logos, and promotional artwork in screenshots belong to their respective owners; their appearance does not imply endorsement.
