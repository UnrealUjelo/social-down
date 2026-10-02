# Security Policy

## Supported versions

Security fixes currently target the latest `main` branch and the newest tagged release.

## Reporting a vulnerability

Do not open a public issue for a vulnerability. Use GitHub's private security advisory flow for this repository, or contact the repository maintainers privately if advisories are unavailable.

Include the affected version, reproduction steps, expected impact, and any suggested mitigation. Please avoid accessing data that is not yours, disrupting services, or publishing exploit details before a fix is available.

## Project boundaries

Social Down intentionally does not implement DRM circumvention, Widevine bypass, paywall bypass, credential theft, encrypted-content cracking, or access-control bypass. Reports or contributions that add those capabilities are out of scope and will be rejected.

Media titles, extractor output, URLs, and downloaded files are treated as untrusted input. Relevant protections include strict URL validation, fixed yt-dlp options, filename sanitization, canonical private work paths, scoped MediaStore output, and no arbitrary shell construction from user data.
