# Vibrate Awake Security Policy

## Privacy

Vibrate Awake has a deliberately small attack surface. It is an offline timer that
vibrates your phone: it collects nothing, sends nothing, and shares nothing, and it
declares no internet permission, so it cannot send data off your device even in
principle. There are no accounts, no analytics, no advertising, and no third-party
services beyond the local Google Play Services link that reaches a paired Wear OS
watch. For the full data story, see the [Privacy Policy](./PRIVACY.md).

---

## Supported versions

| Version | Status | Summary |
| --- | --- | --- |
| v1.2 | ✓ supported  | Current release: phone app plus the Wear OS companion. Security fixes land here. |
| v1.0 | ✗ deprecated | Initial release. Superseded by v1.2; no further fixes. |

Versions track GitHub releases. v1.1 was never cut here (it existed only on Google
Play), so this policy skips from v1.0 to v1.2.

## Reporting a vulnerability

> [!IMPORTANT]
> Do not open a public issue for security vulnerabilities.

### Private advisory (preferred)

Use GitHub's private vulnerability reporting form: [https://github.com/xero/vibrateawake/security/advisories/new](https://github.com/xero/vibrateawake/security/advisories/new)

This opens a private channel between you and the maintainer, and you will receive a response promptly. If the vulnerability is confirmed, we collaborate to fully understand the issue, including a review of proposed fixes, so you can track and validate firsthand. Before any public advisory publishes, we agree on a coordinated disclosure timeline. After disclosure, you are encouraged to publish your own write-up, blog post, or research notes for full hacker scene credit.

### Direct contact

If you prefer direct contact:

- **Email:** x﹫xero.style · PGP: [0xAC1D0000](https://0w.nz/pgp.pub)
- **Matrix:** x0﹫rx.haunted.computer

> [!NOTE]
> Encrypted communication is welcome and preferred for sensitive reports.
