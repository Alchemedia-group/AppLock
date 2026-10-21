# AppLock: Google Play submission pack

Everything below is ready to paste into Play Console. Items in **[BRACKETS]** are details only you know.
Policies change, so check the current wording in Play Console as you go.

---

## 1. App identity

| Field | Value |
|---|---|
| App name | AppLock - Lock Any App (see `listing/` for each language) |
| Package name (applicationId) | `com.alchemedia.applock` (**cannot be changed once published**) |
| Version | 1.0.0, version code 1 |
| Minimum Android | 8.0 (API 26) |
| Target Android | API 37 (Android 17), which meets Google's current target requirement |
| App or game | App |
| Free or paid | **Free**, with an in-app purchase |
| Default language | English (United Kingdom) |
| Other languages | Spanish (Spain), German, French, Italian, Portuguese (Portugal), Polish |

**Files to upload**
- App bundle: `AppLock-1.0.0.aab` (signed with your upload key)
- Icon: `app-icon-512-black.png` (512×512, pure black) or `app-icon-512.png` (dark green)
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots (1080×2160): `01-apps.png`, `02-lock-screen.png`, `03-intruder-photos.png`, `04-settings.png`
- Listing text: `listing/en-GB.txt` and the six translations

**Upload key** (use Play App Signing; Google keeps the real signing key)
- Keystore: `applock-upload.jks`, alias `applock-upload`; passwords are in `KEEP-SAFE.txt`
- Upload certificate SHA-256: `23:A5:AF:1C:E3:94:71:18:69:9B:44:1C:AB:7B:56:1F:6A:F2:97:4B:DB:94:47:0B:ED:9D:A8:67:0B:D5:68:F4`

> Keep two copies of the keystore in different safe places. If it is lost, Google can reset your upload key, but it needs a support request.

---

## 2. Store listing

- **Short description, full description, what's new:** see `listing/<language>.txt` (all within the limits).
- **App category:** Tools
- **Tags (pick up to 5 that Play offers):** choose the ones closest to *Security*, *Privacy*, *Utilities*.
- **Contact email:** **[CONTACT EMAIL]** (required, shown publicly)
- **Website:** **[WEBSITE URL]**
- **Phone (optional):** leave blank
- **Privacy policy URL (required):** **[URL OF YOUR PRIVACY POLICY PAGE]**
- **Screenshots:** the four phone screenshots. They are rendered from a simulation of the app. **Replace them with real screenshots from a phone before you submit**, because Play requires screenshots to reflect the actual app.

---

## 3. App content declarations (Policy > App content)

| Question | Answer |
|---|---|
| Privacy policy | Add the URL above |
| Ads | **No**, the app contains no ads |
| App access | **All functionality is available without special access.** Add the reviewer instructions in section 6 |
| Content rating | Complete the questionnaire (see below) |
| Target audience | **18 and over** (recommended; keeps the app out of the Families policy) |
| Appeals to children | **No** |
| News app | No |
| COVID-19 contact tracing or status | No |
| Data safety | See section 4 |
| Government app | No |
| Financial features | None |
| Health | None |
| Advertising ID | **No**, the app does not use the advertising ID (the manifest has no `AD_ID` permission) |

**Content rating questionnaire (IARC)**
- Category: *Utility, Productivity, Communication, or Other*
- Violence, sexual content, bad language, controlled substances, gambling: **No** to all
- Users can interact or share user-generated content: **No**
- Shares user location: **No**
- Allows users to purchase digital goods: **Yes** (the Pro purchase)
- Expected result: lowest age band (PEGI 3 / Everyone)

---

## 4. Data safety form

AppLock itself sends no data off the device: no accounts, ads, analytics or crash reporting.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is data encrypted in transit? | Not applicable (no data is collected) |
| Can users request data deletion? | Not applicable (nothing is collected); uninstalling removes all on-device data |

Why "No": data that stays on the device is not "collected" under Google's definition. The Pro purchase is handled by Google Play Billing, whose data handling is Google's, not yours.

On-device only, and never transmitted: PIN or pattern (stored as a salted hash), recovery code (hash), locked-apps list and settings, intruder photos.

> If Play Console's wizard asks about photos, app activity or device identifiers, the answer for AppLock is "not collected". Re-read the question wording: if it says "collected" it means leaves the device.

---

## 5. Permission declarations

### 5a. AccessibilityService API declaration (the one most likely to be reviewed closely)

*Is the AccessibilityService API core to the app's functionality?* **Yes.**

*Describe how your app uses the AccessibilityService API (about 500 characters):*

> AppLock is an app-lock. It uses the Accessibility Service only to detect which app has just opened (window-state change events), so it can display its PIN, pattern or fingerprint lock screen in front of apps the user chose to protect. It does not read, record or collect screen content, text, passwords or messages (canRetrieveWindowContent is false), does not perform gestures, and sends no data off the device. Without this service the app cannot lock other apps.

*Is the app designed to help people with disabilities?* **No.** Do not mark it as an accessibility tool.

*Prominent disclosure:* shown inside the app before the user is sent to Android's Accessibility settings, with **Agree and continue** and **No thanks** buttons. The same text is in the store description under "Accessibility service disclosure".

*Demo video (30 to 60 seconds, required; upload to YouTube as unlisted and paste the link).* Record on a real phone:
1. Open AppLock; show the first-run flow and the disclosure screen with its explanation.
2. Tap **Agree and continue**.
3. In Android's Accessibility settings, switch **AppLock** on and accept Android's prompt.
4. Return to AppLock, turn on a protected app (for example Messages).
5. Open that app; show AppLock's lock screen appearing in front of it.
6. Enter the PIN; the app opens.

### 5b. Camera
No special declaration form. The camera is used only for the Pro intruder photo after failed unlock attempts, and is disclosed in the store description and privacy policy.

### 5c. Notification access (optional feature)
No declaration form. It is optional and off by default; it dismisses notifications from locked apps and shows a generic "New notification". The content is never read, stored or sent. Disclosed in the privacy policy.

### 5d. Other permissions in the app
Biometric (Android's own prompt), post notifications, and network or billing permissions that Google's billing libraries add. No location, contacts, storage, phone, SMS, or advertising ID.

---

## 6. App access instructions (for the reviewer)

> No login is needed. On first launch choose a language, pick any PIN or pattern, and save the recovery code. To test protection, turn on the AppLock service when prompted: Settings > Accessibility > Installed apps > AppLock, switch on. Then, in AppLock, switch on any app (for example Messages) and open it: the AppLock lock screen appears in front of it. To test Pro, use a licensed test account: Settings > AppLock Pro > Unlock Pro; after purchase, the intruder photo is taken after a failed unlock attempt.

---

## 7. In-app product (Monetize > Products > In-app products)

| Field | Value |
|---|---|
| Product ID | `applock_pro` (**must match exactly**) |
| Type | One-time product (managed, non-consumable) |
| Name | AppLock Pro |
| Description | Unlock the intruder photo: AppLock takes a photo of anyone who fails to unlock. One payment, no subscription. |
| Price | **£4.99 GBP** (set the base price; Google converts other currencies) |
| Status | **Active** |

You also need a payments (merchant) profile and a bank account set up in Play Console.

---

## 8. Test before you release

1. **Internal testing:** create a release, upload the `.aab`, add yourself and a few people as testers. Pro purchases can only be tested once a build is uploaded.
2. **License testers:** Settings > License testing, add test accounts. Test purchases then cost nothing.
3. **Closed testing:** if your developer account is a **personal** account created after late 2023, Google requires a closed test with about 12 testers for 14 continuous days before you can apply for production. Organisation accounts are usually exempt. Check Play Console for your account's requirement.
4. **Pre-launch report:** review the automated crash and accessibility report Play generates.
5. **Test on at least two real phones** (ideally Samsung plus another brand): lock overlay, camera photo, a test purchase, language switching, the accessibility steps.
6. The release build is minified (R8). It builds cleanly but has not been run on a device. Test the internal-testing build before promoting it.

---

## 9. Release steps

1. Create the app in Play Console (name, default language, App, Free, accept declarations).
2. Complete the sections above (store listing, app content, Data safety, permission declaration with video).
3. Create the `applock_pro` product and activate it.
4. Production or testing > Create release > upload `AppLock-1.0.0.aab`.
5. Paste the release notes from `listing/<language>.txt` for each language.
6. Select countries, then **Send for review**. First reviews can take several days, and accessibility apps often take longer.

---

## 10. Risks to know about

- **Accessibility API policy.** Google restricts this API to specific uses. An app lock is a legitimate security use, but reviewers look closely. Keep the disclosure, the declaration text and the video accurate and consistent. If it is rejected, read the reason and reply to the reviewer; the alternative is a different detection method, which is a larger rework.
- **The fake crash screen** is opt-in and described in the listing; keep that description honest.
- **The name "AppLock"** is very common. A more distinctive name avoids clashes with existing apps.
- **The package name** is permanent once published.
- **Billing price** is shown from Google Play at runtime; the £4.99 fallback only appears before the store responds.
