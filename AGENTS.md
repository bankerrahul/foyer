# Foyer development

- Scope is the home screen. Do not add debloating, firmware changes, projector controls, analytics, network permissions, or automatic package removal as part of routine changes.
- Preserve `LICENSE`, including the original copyright notice.
- Keep signing keys, passwords, `.secrets/`, `.state/`, personal device addresses, and generated outputs out of Git.
- Run `sh test.sh` after changing build/install/recovery behaviour. Run `sh build.sh` for Android source, resources, or manifest changes.
- Device changes require an explicit device target and user authorization. Preview before activation. Never disable a package automatically or work around a signing error by uninstalling.
- Preserve the previous launcher and state file until recovery is verified. Report simulated checks separately from physical-device testing.
- Confirm before committing, pushing, or creating a PR. When committing, follow the user's issue-number and staged-diff approval workflow. Do not add commit trailers.
