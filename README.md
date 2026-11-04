# FTC Equipment Tracker

An Android app for FIRST Tech Challenge teams to track parts loaned to and borrowed
from other teams. Built by **FTC Team #25310 Pinelands Stingers**.

Pick a part from the catalog (or add a custom one), record the two teams, dates,
location, notes, and an optional photo. Each team's coach or captain confirms the
share and signs for it from their own phone, so both teams have a shared record.

**Quick start**

1. Install the APK your coach shares.
2. Sign up, verify your email, and set up or join your team.
3. Log a share with the **+** button.
4. Have a coach or captain from each team confirm and sign.

Jump to: [Using the app](#using-the-app) ·
[Common problems](#common-problems) ·
[For developers](#for-developers)

---

## Using the app

This section is for team members, coaches, and captains.

### What you can do

- **Log a share** when your team lends a part to another team.
- **Confirm a share** from each team's side, so nothing is recorded without both
  teams knowing.
- **Sign for a share** with a finger on the screen.
- **See who has what** in one loan list, with due dates and status.
- **Search the parts catalog** by name, SKU, tag, or your team's own alias.
- **Add custom parts** for 3D-printed or one-off items.
- **Join your team** with an invite code, and share those codes with teammates.
- **Report a suspicious team account** to the organizer.

### Before you start

- An Android phone running **Android 8.0 or newer**.
- An internet connection.
- An email address you can check.
- Your FTC team number.
- Either you are the first person setting up your team, or you have your team's
  invite code.

### Install the app

Your coach or organizer will share the APK file, or you can download the latest version from releases on the Github repository. To install it:

1. Open the APK on your phone.
2. If Android asks, allow your browser or file manager to install unknown apps.
3. Tap **Install**, then **Open**.

> If you are building the app yourself, see [For developers](#for-developers).

### Step 1: Create your account

1. Open the app and tap **New team? Create an account**.
2. Enter your email and a password.
3. Tap **Sign up**.
4. Check your email for the verification link and open it.
5. Come back to the app and tap **I've verified my email**.

If the email does not arrive, check your spam folder, then tap **Resend verification
email**. You must verify your email before you can continue.

### Step 2: Set up or join your team

After verifying your email, the app asks you to choose your team and role.

**If nobody from your team has used the app yet, and you are a coach or captain:**

1. Select your team from the list.
2. Choose **Coach** or **Captain**. (Only a coach or captain can create a team.)
3. Enter your name.
4. Tap **Create team account**.
5. The app shows a **member code** and a **leader code**. Save them and share them
   with your team.

**If your team already has an account:**

1. Select your team from the list.
2. Choose your role.
3. Enter your name.
4. Enter the invite code:
   - **Coach or captain:** use the **leader code**.
   - **Member:** use the **member code**.
5. Tap **Join team**.

**Keep the codes safe.** Share the member code in your team's private group chat or
at a build meeting. Share the leader code only with coaches and captains. Never post
either code publicly.

To view the codes later, open the **team icon** (group of people) on the Loans screen.
Coaches and captains can also regenerate the codes there. Regenerating makes the old
codes stop working immediately; people who already joined keep their accounts.

> If an organizer has turned on team verification, your team must be on the verified
> list before you can create or join its account. Ask your organizer to verify it.

### Step 3: Log a share

1. On the **Loans** screen, tap the **+** button.
2. Choose the part:
   - **Catalog part:** search by name, SKU, tag, or alias, then pick the part.
   - **Custom / 3D printed:** type the part name and, optionally, a SKU.
3. Select the **borrower** (the team receiving the part). Your team is the lender.
4. Pick the **loan date** and the **return / due date**.
5. Add a **location** and any **notes**.
6. Tap **Take photo** if you want a picture of the item. You can retake it.
7. Tap **Save share**.

The loan appears in the list with the status **Awaiting confirmation**.

### Step 4: Confirm and sign the share

Open the loan from the **Loans** list.

- A **coach or captain** from each team taps **Confirm** on their own team's line.
  When both sides have confirmed, the loan becomes **Active**.
- A **coach or captain** from each team signs their own team's signature box. Draw
  with a finger and tap **Confirm signature**.
- You can only confirm or sign for your own team's side.

> The signature is a **good-faith confirmation gesture**, not a legally binding
> electronic signature.

### Step 5: Mark the item as returned

When the part comes back, open the loan and tap **Mark as returned**. Returned loans
stay in the list for your records.

### Parts catalog

Open the **Parts** screen from the wrench icon on the Loans screen.

- Search by name, SKU, tag, or alias.
- Filter by vendor: goBILDA, REV Robotics, AndyMark, or Studica.
- Tap the **pencil** on a part to add your team's own nicknames (aliases). Aliases are
  private to your team and make searching faster.
- Use **Custom / 3D printed** when logging a one-off part that is not in the catalog.

### Team directory

Open the **Teams** screen from the list icon on the Loans screen.

- Shows registered FTC teams in South Africa for the current season.
- Tap the **refresh** icon to update the list.
- The list is cached on your phone and refreshed about once a day.

### Trust badges and reports

Team accounts show helpful trust information:

- **Verified:** an organizer has added this team to the verified list.
- **Unverified:** the team has not been verified yet.
- **Not verified:** the team is unverified and verification is currently enforced.

The loan and team screens show which account claimed a team and when. If something
looks wrong, tap **Report a problem** and describe the issue (at least 10 characters).
An organizer will review the report.

### Common problems

| Problem | What to do |
| --- | --- |
| No verification email | Check spam, make sure the address is correct, then tap **Resend verification email**. |
| "That invite code is not valid" | You may have used the wrong code for your role, or the code was regenerated. Ask your coach for the current code. |
| "Team not verified" | Ask an organizer to verify your team before creating or joining. |
| My team is not in the list | Tap **refresh** on the Teams screen. The team must be registered with FTC for the current season in South Africa. |
| I cannot confirm or sign | Only a coach or captain can confirm and sign, and only for their own team's side. Your team may also be paused if verification is enforced. |
| Camera will not open | Allow camera permission in Android settings. You can still save the share without a photo. |
| Nothing loads | Check your internet connection, then try again. |
| Android blocks the install | Allow installs from your browser or file manager, then open the APK again. |
| I changed phones or email | Sign in with the same account. Account recovery is handled by the organizer if you are locked out. |

### Privacy and your data

- The app stores your email, display name, role, team, loan details, optional item
  photo, and confirmation signatures in a Firebase database.
- People on both teams involved in a loan can see that loan. Organizers can review
  team, report, and verification data through admin tools.
- Do not photograph anything private. Only photograph the loaned part.
- The app is used by school-age students. Consent and data-retention questions should
  go through your coach, team, or school under POPIA.
- The signature is a good-faith confirmation, not a legal e-signature.

---

## For developers

### Tech stack

| Area | Used |
| --- | --- |
| Language | Kotlin `2.0.20` |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Backend | Firebase Authentication and Cloud Firestore |
| Networking | OkHttp |
| Serialization | kotlinx.serialization |
| Images | Coil |
| Build | Gradle `8.13`, Android Gradle Plugin `8.13.2` |
| Android | `minSdk 26`, `targetSdk 34`, `compileSdk 34` |
| Java | JVM 17 |

### Requirements

- Android Studio (current stable) with its bundled JDK 17 or newer.
- Android SDK Platform 34.
- Access to the team Firebase project if you need to change backend config.
- Python 3.10+ only if you are working on the catalog or admin tools.
- Firebase CLI only for deploying Firestore rules.

### Open and run

1. Open the repository root in Android Studio.
2. Let Gradle sync finish.
3. Confirm `app/google-services.json` exists. It is already tracked in this repo.
4. `local.properties` with your SDK path is generated by Android Studio and is not
   committed.
5. Pick a device or emulator and press **Run**.

Command-line build:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release build: `.\gradlew.bat assembleRelease`

Release signing uses `keystore.properties` and a keystore that are deliberately not
committed. Builds without them still produce an unsigned release APK. See the
maintainer runbook kept outside this repository.

Lint:

```powershell
.\gradlew.bat lint
```

There is currently no automated test suite.

### Project layout

```text
app/src/main/java/za/org/team25310/ftcequipmenttracker/
  data/                 Repositories, Firebase access, FTC Events API
  data/model/           Serializable models and enums
  ui/                   ViewModels
  ui/navigation/        Navigation graph and routes
  ui/screens/           Compose screens and shared components
  ui/theme/             Material 3 theme
app/src/main/assets/    Bundled fallback teams and parts catalog
app/src/main/res/       Android resources and manifest assets
firestore.rules         Firestore security rules
firebase.json           Firestore deploy config
tools/catalog/          Catalog scraping and seeding scripts
tools/admin/            Organizer verification/report tooling
```

### How data is stored

| Collection | Purpose |
| --- | --- |
| `users/{uid}` | Team, role, display name for each signed-in account. |
| `teamDirectory/{teamId}` | Non-secret record that a team account has been claimed, and by whom. |
| `teamInvites/{teamId}` | Member and leader invite codes. Readable only by the team's leadership. |
| `loans/{loanId}` | Loan details, confirmations, signatures, and compressed item photo. |
| `partAliases/{aliasId}` | Team-specific part nicknames. |
| `parts/{partId}` | Public parts catalog, written by admin tooling only. |
| `catalogMeta/current` | Catalog version used by the app to refresh its cache. |
| `verifiedTeams/{teamId}` | Organizer-managed verification allowlist. |
| `appConfig/security` | Organizer switch for `requireVerifiedTeams`. |
| `teamReports/{reportId}` | In-app reports for organizer review. |

Photos and signatures are compressed and stored as base64 strings inside the loan
document. No Cloud Storage bucket is used.

### Firestore rules

Deploy the rules in `firestore.rules` after changing them:

```powershell
firebase deploy --only firestore:rules
```

Rules enforcement summary:

- Accounts must have a verified email.
- Invite codes are only readable by the team's coach/captain (or the original creator).
- A user can only confirm or sign the side belonging to their own team.
- Catalog writes are blocked for app users; only the admin SDK can write parts.
- When `appConfig/security.requireVerifiedTeams` is true, unverified teams cannot
  create accounts, join, rotate codes, confirm loans, or sign.

### Team verification and admin tooling

Organizer tooling lives in `tools/admin/`. It covers verifying teams, enforcing the
allowlist, reviewing reports, and reclaiming a team for its real coach.

```powershell
pip install -r tools/catalog/requirements.txt
python tools/admin/manage_teams.py claim-info --team-id 25310
```

See [`tools/admin/README.md`](tools/admin/README.md) for the full command reference.

### Parts catalog tooling

The catalog is scraped from AndyMark, goBILDA, and REV Robotics, then seeded into
Firestore. The app falls back to `app/src/main/assets/parts.json` when Firestore is
unreachable.

```powershell
# Refresh the scraped data
python tools/catalog/scrape_catalog.py

# Preview what would be written
python tools/catalog/seed_firestore.py --key path\to\service-account.json --dry-run

# Seed Firestore
python tools/catalog/seed_firestore.py --key path\to\service-account.json
```

`seed_firestore.py` bumps `catalogMeta/current.version`, which tells installed apps to
download the new catalog. Use `--prune` to remove parts that are no longer in the
scraped data.

### FTC Events API

Team data comes from the official FTC Events API (South Africa, current season). The
credentials and season live in
`app/src/main/java/za/org/team25310/ftcequipmenttracker/data/FtcApiConfig.kt`.

- Access uses HTTP Basic Auth with an API username and token.
- Teams are cached for 24 hours and can be refreshed from the Teams screen.
- Data attribution is shown on the app's About screen.

### Contributing

- Keep changes focused and match the existing Compose/Kotlin style.
- Run `.\gradlew.bat lint` and `.\gradlew.bat assembleDebug` before committing.
- Never commit service account keys, keystores, `keystore.properties`, or
  `local.properties`.
- Do not change `firestore.rules` without testing both the allowed and denied paths.

### Known limitations

- No push notifications or due-date reminders yet.
- No offline-first support; account setup, team actions, and refreshes need a connection.
- No automated tests.
- No automated account recovery; a coach or organizer handles reclaims.
- Loan photos and signatures are stored in the loan document, so very large images
  are intentionally compressed.

---

## License

See [LICENSE](LICENSE).
