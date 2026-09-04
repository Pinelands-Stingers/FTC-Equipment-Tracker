# Engineering Scoping Plan: FTC Parts Loan App

**FTC Team #25310 — Pinelands Stingers**

*Prepared August 25, 2026 — season kickoff is September 12, 2026, about 2.5 weeks out.*

## How to use this document

This builds on the architecture review already done earlier in this conversation (the Firebase recommendation and feature-by-feature notes) and adds a decision-anchored architecture call, a phase plan tied to your actual FIRST South Africa season calendar, a dependency map, and a risk register. A handful of facts from the earlier pass have changed or needed sharper detail since — those are marked **[Updated]**.

Written so you can drop it into your engineering notebook close to as-is — FTC judging rewards documented process, and "here's how we scoped, sequenced, and risk-assessed a season-long software project" is exactly that kind of evidence.

## 1. Architecture decision — make this first, on day one

**Client:** Kotlin + Jetpack Compose in Android Studio. Compose is now the default new-project template and has the better-documented current tutorial path — worth the (small) extra learning curve over the older XML View system, since most current guides assume it.

**Backend — the real decision:**

Your spec says "store the log on a server we host." Weigh that against Firebase honestly before locking it in:

|                                                | **Firebase (managed)**                                                                                       | **Self-hosted (Node/Express or Supabase+Postgres on Render/Railway/Fly)**                                                    |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| Keeps running after this year's team graduates | Google's problem                                                                                             | Whoever inherits the repo — the classic student-project failure mode                                                         |
| Ongoing ops work                               | Near zero                                                                                                    | Real: dependency updates, uptime, backups                                                                                    |
| Cost                                           | Auth, Firestore, and Cloud Messaging stay free, no card. Storage now needs a linked card — see callout below | Free tiers exist but are limited (sleep on inactivity, small caps); a small always-on Postgres often lands around $5–7/month |
| "We built a custom backend" story for judging  | Weaker — it's a managed service                                                                              | Stronger, if your award category specifically rewards it                                                                     |
| Learning curve for a beginner team             | Lower — best-documented path for exactly this stack                                                          | Higher — you're also now responsible for API design, auth, and deployment                                                    |

**[Updated] The free-tier picture changed since the earlier pass in this conversation.** Since February 2026, Firebase projects on the no-cost Spark plan have lost access to Cloud Storage entirely — buckets return errors until the project is upgraded to the pay-as-you-go Blaze plan. Practically:

- Firestore, Auth, and Cloud Messaging are still genuinely free, no card required.
- The **photo-of-each-part feature** specifically needs Blaze, which means someone — ideally a mentor's or the team's own account, not a student's personal card — links a valid card to the Google Cloud billing account.
- This doesn't mean it costs money: Blaze keeps the same free quotas (several GB of storage before any charge), and you can set a budget alert in Google Cloud Billing so nobody's surprised. But "no card, ever" is no longer true for this one feature.
- If a linked card is a hard no for your team or school, keep Firestore/Auth/Messaging on Firebase and route just the loan photos to a separate image host with its own no-card free tier (Cloudinary is a common choice for exactly this). Worth 20 minutes of comparison before committing either way.

**Recommendation:** Firebase + Blaze with a budget alert, unless your judged award category specifically requires a demonstrated self-built backend — in which case Supabase (hosted Postgres, still low-ops, more "real database" flavor than Firestore) is a better middle ground than raw Node/Express, since it keeps the maintenance risk low while still giving you a backend you designed.

**Version control:** GitHub, as planned. Create a **team-owned** GitHub org and Google/Firebase account today — not a personal account belonging to whoever's driving the project right now. One-line decision that saves real pain in two years when that student graduates and the whole backend is tied to an email nobody can access.

## 2. Team directory: FTC Events API — confirmed details

The official FTC Events API is real, free, and open to any team or developer — confirmed directly. A few specifics worth knowing before building against it:

**Auth [Updated]:** not a simple API key — access requires a username/token pair issued through self-service registration (HTTP Basic Auth in practice). Registration is automated, so there shouldn't be a long approval wait.

**Attribution requirement:** FIRST asks that any app displaying their data link back to the API info page — e.g., in your About screen. Add this to the Phase 0 checklist; two minutes now, easy to forget later.

**Non-commercial only** — fine for you, just don't bolt on paid features that touch this data later.

**Data freshness:** cache the team list locally and refresh on a schedule (daily is plenty) rather than calling it live on every screen. Kickoff on September 12 is when this season's roster genuinely starts moving — right now, most of what you'd pull is still last season's list.

**Also check firstsa.org directly:** South African teams register both with FIRST globally and with FIRST South Africa as the regional affiliate, so the regional site's own team list may update faster than the global API right around kickoff.

## 3. Parts catalog: what's actually available

Confirmed directly:

**REV Robotics and Studica:** no public catalog API. A curated list is your only real option for these two.

**goBilda [Updated]:** still no *official* developer API, but a third-party service now offers an unofficial goBilda catalog wrapper (built by parsing their site, not an official partnership). It could save some initial data-entry time on goBilda SKUs specifically, but treat it as a one-time bootstrapping shortcut, not infrastructure — it's a paid layer sitting on top of a site that owes it nothing. Use it (if at all) to seed your own list once, then own that list going forward.

**Net result:** your original instinct — a curated seed list of the ~100–200 parts teams actually swap, plus custom-part creation with per-team aliases for the long tail — is confirmed as basically the *only* robust option, not just one option among several.

## 4. Phases

You're scoping this **2.5 weeks before kickoff**, inside FIRST South Africa's actual season shape: design and build runs September through January, with tournaments starting as early as October and continuing through April. That matters for sequencing — robot build starts eating team bandwidth the moment kickoff hits, so front-load what you can into the next two weeks, and expect later phases to stretch out opportunistically rather than run on a tight linear clock. Assign a small, consistent sub-team to this rather than "everyone helps" — it's meant to run alongside build season, not compete with it.

### Phase 0 — Foundations (target: before Sept 12)

- Decide Firebase+Blaze vs. self-hosted (Section 1) and write down the decision and why
- Create the team-owned GitHub org and Google/Firebase account
- Register for FTC Events API access — self-service, no reason to wait
- Draft the Firestore data model (Section 7 is a starting point)
- Draft a short POPIA-lite privacy notice — first check with your coach whether **existing team/school registration paperwork already covers app-based data use**; if so, this needs one added line, not a new process from scratch (see risk #4)
- Write the actual sentence, now, stating the captain's "signature" is a good-faith confirmation gesture, not a legally binding e-signature — have it ready for the UI later
- Register a Google Play Console account if you'll distribute via Play Store: one-time $25 fee, ID verification can take 1–2 days, doesn't block anything else so do it early

### Phase 1 — MVP (first few weeks post-kickoff)

- Auth (simplest workable version — team-code or email based)
- Team directory synced from the FTC Events API, cached locally
- Manual custom-part creation with team-specific aliases
- Single-sided loan record: parts, teams, date/time, location, duration — no dual confirmation yet
- "What's out, what's borrowed" list view, filtered by team and status

### Phase 2 — Confirmation & trust (rolling, through build season)

**Clarify roles first:** your spec asks for both coach confirmation *and* a captain's signature / device verification. Decide explicitly whether coach and captain act on the same device/session, or whether one loan needs sign-off from four distinct people (both coaches plus both captains) — this shapes the state machine, so settle it before writing code.

- Dual-device confirmation state machine (pending → confirmed), built server-authoritative (risk #2)
- Photo capture + upload, compressed client-side before it ever leaves the device
- Edit/dispute path — an amendment trail, not silent edits
- Decide and build the coach-identity verification mechanism — the piece that actually makes "prevent loans logged without both teams' knowledge" true (risk #9, the most important open gap right now)
- Once you have a testable build: start the Play Console 12-tester/14-day closed-test clock in the background (Section 5) — it's calendar time, not developer effort, so let it run while Phase 3 continues

### Phase 3 — Reminders, signatures, catalog polish (target: whenever build-season intensity next dips — you'll know that better than I can guess)

- WorkManager-based due-date reminders (local, so venue wifi doesn't matter)
- Signature capture inside the confirmation flow
- Seed the curated goBilda/REV/Studica catalog; add search-as-you-type de-duplication before letting someone create a new custom part
- Account recovery / re-association flow for coach and captain turnover
- Finalize the POPIA notice with an actual mentor or parent sign-off

### Phase 4 — Hardening & season readiness (target: ahead of/through tournament season)

- Deliberately test the offline confirmation flow under bad wifi, not just at your usual meeting space
- Apply for Play Store production access once the closed-testing window is done — or skip production entirely and keep coaches as permanent closed-testers (Section 5)
- Set up a periodic Firestore export so a season's history doesn't live only in one database
- Engineering-notebook write-up of the decisions above (architecture call, POPIA approach, the trust-model gap and its fix)
- Team-authorship and about-screen housekeeping (FTC #25310 Pinelands Stingers, FTC API attribution link)

## 5. Dependency map

**Can start immediately, blocks nothing else:**

- FTC Events API registration
- Team-owned GitHub/Google account creation
- Play Console account registration (the $25 fee and ID verification can run in the background)
- Parts catalog curation — this is data entry, not code, so non-coding teammates can run it in parallel starting Phase 0, not gated on the app existing

**Blocks almost everything downstream:**

- Firestore schema + security rules — auth, loan CRUD, and the confirmation flow all sit on top of this; get it right before writing feature code, not alongside it
- The architecture decision itself (Section 1) — changes what Phase 1 code even looks like

**Sequenced:**

- Auth → loan creation (need to know who's creating it) → confirmation flow (need to know who's confirming) → signature capture (part of the confirm step)
- A loan record with a due-date field → reminders — nothing to remind on before that field exists
- A testable build → the Play Console 12-tester/14-day clock — can't start until Phase 2 produces something installable, but once started it runs on its own timeline alongside further dev
- POPIA notice existing → any pilot testing with real names, real photos, real signatures (fake/test data is fine before this; real team data shouldn't flow through the app before it)

**Explicitly not blocking:**

- The curated parts catalog doesn't need to be complete, or even started, for Phase 1 — custom parts and aliases cover the gap from day one
- Signatures and reminders (Phase 3) don't block Phase 1 or 2 — resist building them early just because they're in the original spec

## 6. Risk register

### Technical

**Firebase Storage now needs a linked card (Blaze).** Mitigation: an adult links the card, set a Cloud Billing budget alert, keep usage inside the free quota with client-side image compression *and* a server-side Storage rule capping file size — don't rely on client compression alone, since a modified client could skip it.

**Dual-confirmation race conditions.** Two devices confirming near-simultaneously, or one offline for hours, can leave an inconsistent status if state is merged client-side. Mitigation: make the loan's status field server-authoritative via a Firestore transaction or Cloud Function, never something either client decides alone.

**Notification reliability at competition venues.** Venue wifi is notoriously bad. Mitigation: WorkManager for due-date reminders (local, survives offline) rather than depending on live push for anything time-sensitive.

### Legal / compliance

**POPIA applies to your entire user base.** FTC is grades 7–12, meaning every user of this app is legally a "child" under POPIA, and your spec specifically asks for a signature from the team **captain** — a student — not just the coach. Processing a child's personal information generally needs prior consent from a parent, guardian, or other "competent person," with a few other lawful-processing bases available. Mitigation: don't build a consent flow from scratch — check first whether your team or school's existing registration paperwork already covers "use of a team app storing names, photos, and a captain's signature for loan tracking." If not, that's a short addition, not a new process. Minimize what you store (does a team-linked username work instead of a full name?), and decide up front whether photos and signatures need to outlive the season or can be purged. (Not legal advice — worth a short conversation with a mentor about your existing consent paperwork, not a lawyer, for something at this scale.)

**"Signature" read as legally binding.** Mitigation: explicit in-app copy stating it's a good-faith confirmation gesture, written in Phase 0.

### Team / organizational

**Infrastructure tied to a personal account.** Whoever currently owns the Firebase project or GitHub org graduates, and the team loses access. Mitigation: team-owned accounts from day one (Section 1) — the single highest-leverage five-minute decision in this whole plan.

**Season bandwidth competition.** Build season runs September through January with at least weekly meetings, and the same students building the robot are likely building this app. Mitigation: a genuinely minimal MVP, phases treated as rolling rather than a hard schedule, a small dedicated sub-team rather than "everyone helps."

**Key-person knowledge concentration.** One student understands the confirmation state machine or security rules; they graduate or get busy. Mitigation: this document plus a living decisions log, pairing on the trickiest pieces.

### Product / trust

**Coach-identity spoofing — the biggest open gap.** The entire point of dual confirmation is preventing a loan from being logged without both teams' knowledge. But nothing in the current spec stops someone from registering as "coach of Team 1234" and confirming a fake loan on that team's behalf. This needs an actual mechanism before the trust guarantee is real: options include checking new coach registrations against a known contact list, an invite-code system where an already-verified coach vouches for the next, or a lightweight manual approval step for new team accounts. Decide this in Phase 2 — not at an event, after the fact.

**Single-team vs. multi-team adoption, undecided.** The app's usefulness depends on *other* SA FTC teams' coaches actually using it, which isn't guaranteed, and changes your registration/auth model substantially (open registration vs. invite-only, support burden). Worth an explicit team decision rather than an implicit default.

**No data-export path.** If the Firebase project ever needs to move, or a season's records need handing off, there's currently no plan for getting data out. Mitigation: a periodic Firestore export job, documented in the README.

## 7. Data model sketch (starting point, not final)

```
teams/{teamId}
  name, ftcTeamNumber, region, coachContactIds[]

parts/{partId}
  sku (nullable — null for custom/3D-printed parts)
  name, vendor (gobilda | rev | studica | custom)
  aliasesByTeam: { teamId: [alias strings] }

loans/{loanId}
  lenderTeamId, borrowerTeamId
  partIds[], photoUrls[]
  location, createdAt, dueDate
  status: pending | confirmed | returned | disputed
  confirmations: {
    lenderCoachConfirmedAt, borrowerCoachConfirmedAt,
    captainSignatureUrl (lender + borrower)
  }

users/{userId}
  role: coach | captain | member
  teamId
```

Enough to build Phase 1 against. Security rules (who can write to what) are their own piece of work — happy to draft those once this schema feels right.

## 8. Can I actually build this with you?

Yes, with a clear division of labor: I can write and explain Kotlin/Compose code, design the Firestore schema and security rules, write the FTC Events API integration, work through the confirmation state machine logic, and draft the POPIA-lite notice text. I can't open Android Studio, click through an emulator, or test on a phone — so the loop is: I write and explain code, you build and run it, paste back errors or behavior you're seeing, and we iterate from there.

---

## Sources checked while writing this

- FTC Events API — official info & registration: https://ftc-events.firstinspires.org/services/API
- FIRST South Africa — FTC team basics & season calendar: https://firstsa.org/ftc-team-basics/
- Firebase Cloud Storage Spark-plan changes (official FAQ): https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024
- POPIA sections 34–35, children's personal information overview: https://usercentrics.com/knowledge-hub/south-africa-popia-protection-of-personal-information-act-overview/
- Google Play Console closed-testing requirement: https://support.google.com/googleplay/android-developer/answer/14151465

*Worth re-checking before you build: exact FTC Events API rate limits and filter parameters (confirm in the live docs once registered), and current Cloudinary/Supabase free-tier terms if you go that route for photos instead of Firebase Blaze.*
