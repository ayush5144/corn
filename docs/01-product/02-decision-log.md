# 02 — Decision Log

Every product question asked and the founder's answer, in order. Settled answers flow into [01-prd.md](01-prd.md). New rounds are appended at the bottom.

**Format:** question → recommendation given → founder's answer → status.

---

## Round 0 — 2026-11-01

### Q0.1 Who and where?
- **Recommended:** Android-heavy markets, short-to-medium sessions.
- **Answer:** Not answered yet.
- **Status:** 🟡 Open → carried to Round 1.

### Q0.2 What happens when the app is closed?
- **Recommended:** Flush after a short timer the user can change, with the Flush button always one tap away.
- **Answer:** The user decides. All options exist:
  - Flush immediately via a button (a power or toilet/flush button, like DuckDuckGo's Fire Button).
  - "Flush after X minutes" / "flush after X minutes of inactivity" as a setting.
  - "Keep the session" as a setting.
- **Interpretation:** "Keep the session until the user encrypts" was read as *keep the session until the user flushes*. Confirm in Round 1.
- **Status:** ✅ Settled (options). 🟡 Default and button design open.

### Q0.3 What goes in the Vault for v1?
- **Recommended:** Links, titles, tags and optional thumbnails; files later.
- **Answer:** Links and titles; tags if needed; thumbnails to be discussed. Users must be able to group items.
- **Interpretation:** "walls" in the voice note = **Vault**.
- **Status:** ✅ Links + titles + grouping. 🟡 Tags detail, thumbnails, files.

### Q0.4 How discreet?
- **Recommended:** PIN plus blank recents in v1; disguised icon later.
- **Answer:** PIN to open, as a setting. Black screen in recent apps (liked). Notifications only appear while the app is open; defaults plus settings.
- **Status:** ✅ Settled. 🟡 Disguised icon not addressed; notification detail open.

### Q0.5 Default search engine?
- **Recommended:** DuckDuckGo with safe search off; user can change.
- **Answer:** DuckDuckGo, Google, etc. Multiple engines, switchable from a single dropdown in the search bar.
- **Status:** ✅ Settled. 🟡 Out-of-box default and safe-search handling open.

### Q0.6 How does Corn make money?
- **Answer:** It doesn't, for now. Free browser, funded by donations. Idea: a paid Cloud Vault at $3–5/month.
- **Status:** ✅ Free + donations. 🟡 Cloud Vault is an idea, not v1.

### Q0.7 What's out of v1?
- **Recommended:** Desktop, iPhone, sync, file downloads and extensions all out.
- **Answer:** Extensions are out. "Other things should be in."
- **Status:** ✅ Extensions out. 🟡 Conflicts with Android-first; needs splitting in Round 1.

### Q0.8 "What do you mean by sync?"
- **Answer given:** Sync means the same Vault (and possibly settings) on multiple devices, which requires storing it on a server. The founder's Cloud Vault idea is the same thing.
- **Status:** 🟡 Folded into the Cloud Vault question.

---

## Round 1 — 2026-11-01

### Q1 Where are your users?
- **Recommended:** Android-heavy markets; Android first.
- **Answer:** Mobile first: Android **and** iPhone, not only Android. Build on Android first, but other platforms are definite.
- **Status:** ✅ Android built first; iPhone committed next.

### Q2 Split "everything else is in" into versions
- **Recommended:** v1 = Android only with every PRD feature.
- **Answer:** Android only while building; other platforms definitely later.
- **Status:** ✅ v1 = Android.

### Q3 Why would someone switch to Corn?
- **Recommended:** Flush as the centre, a private Vault, blocking tuned for adult sites.
- **Answer:** Corn is cool, funny and modern. It helps you save links, organise your stuff and not get lost. DuckDuckGo has no tab grouping: in Corn you open a link into a new Tab Group, browse, swipe or click around, see previews and switch instantly.
- **Status:** ✅ Differentiators: Tab Groups + Vault/Collections + Flush on your terms + blocking + personality.

### Q4 What does "closed" mean?
- **Recommended:** Leaving Corn, with a 30-second grace period.
- **Answer:** When the app is closed, the user chooses how long until it flushes, or never.
- **Status:** ✅ "Leaving Corn" trigger with a user-chosen delay or never.

### Q5 Inactivity while a video plays
- **Recommended:** A playing video counts as activity.
- **Answer:** Yes. If a video is playing, the session stays active.
- **Status:** ✅

### Q6 Out-of-box default
- **Recommended:** Flush when you leave Corn (30-second grace).
- **Answer:** **Rejected.** Default is keeping the session until the user flushes. Keep three preset choices.
- **Status:** ✅ Default = Kept Session. 🟡 What the three presets are (Round 2).

### Q7 Kept Session protection
- **Recommended:** Android sandbox + App Lock; Corn encrypts only the Vault.
- **Answer:** Asked whether to switch engines if Corn can't encrypt cookies and site data, and what to do instead.
- **Response given:** Don't switch engines; no engine solves this (see "Q7 follow-up" below).
- **Status:** 🟡 Recommendation pending acceptance (Round 2).

### Q8 Flush Button design
- **Recommended:** Flush icon in the toolbar, one tap, no confirmation.
- **Answer:** A toilet/flush button, not a power button. Styling is flexible.
- **Status:** ✅ Toilet/flush button; one tap. 🟡 Visual styling.

### Q9 Vault groups
- **Recommended:** "Collections", one level, one Collection per item, tags cut across.
- **Answer:** "Collections" or "Vault groups"; the recommendation was right.
- **Status:** ✅ **Collections** (glossary picks one term).

### Q10 Vault thumbnails
- **Recommended:** Screenshot at save time, encrypted, can be turned off.
- **Answer:** Optional: screenshot, no thumbnails, or a default placeholder.
- **Status:** ✅ Optional. 🟡 Which is the default.

### Q11 Vault lock vs App Lock
- **Answer:** Vault offers PIN or biometrics by default; App Lock is optional.
- **Status:** ✅

### Q12 Notifications
- **Answer:** Fine with no notifications while closed: "actually good."
- **Status:** ✅

### Q13 Default search engine
- **Status:** ✅ Accepted (DuckDuckGo default, dropdown switcher). Carried from Round 0.

### Q14 File downloads in v1
- **Answer:** Not a priority. Skip for now, add to the checklist.
- **Status:** ⏭️ Backlog.

### Q15 Keep List
- **Recommended:** v1.1.
- **Answer:** **Rejected.** Needed in v1: a list of sites that stay logged in across Flushes, or saved passwords that survive a Flush.
- **Status:** ✅ v1. 🟡 Design (Round 2).

### Q16 Cloud Vault
- **Answer:** Not v1; add to the checklist.
- **Status:** ⏭️ Backlog.

### Q17 18+ gate and disguised icon
- **Status:** ✅ Accepted by silence; flag if wrong.

### New: Tab Groups
- **Answer (from Q3):** Open links in a new Tab Group, swipe, previews, instant switching.
- **Status:** ✅ Core v1 feature. 🟡 Details (Round 2).

### New: Checklist
- **Answer:** Create a checklist with tasks and goals assigned properly.
- **Status:** ✅ Created: [../03-planning/02-checklist.md](../03-planning/02-checklist.md).

### Q7 follow-up — "Should we switch engines so Corn can encrypt cookies?"

**No.** GeckoView (Firefox) and a Brave fork store cookies the same way: in the app's private folder, without an extra app-level key. Chrome, Brave and DuckDuckGo on Android all rely on the same protection. What matters is *who* could read the data:

| Threat | What protects Kept Session data |
|---|---|
| Someone picks up your unlocked phone and opens Corn | **App Lock** (optional), Privacy Screen |
| Another app on the phone | Android **app sandbox**: apps can't read each other's files |
| Phone stolen or seized while locked | Android **file-based encryption** (mandatory on modern phones), tied to your phone's lock |
| Phone / Google cloud backup | **Corn opts out of backups** entirely, so Session data is never copied off the phone |
| Rooted or hacked phone | Out of scope; no browser can protect against this |

**Proposed:** (1) don't switch engines; (2) Corn encrypts everything *it* stores about the Session (tab list, Tab Groups, URLs, previews) with its own key, the way the Vault is encrypted; (3) exclude all app data from backups; (4) when the user turns on Kept Session, suggest turning on App Lock.

---

## Round 2 — pending
