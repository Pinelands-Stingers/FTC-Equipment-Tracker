# Team verification admin tooling

Organizer-only tooling for the middle-path trust model:

- keep trust-on-first-use by default;
- maintain a `verifiedTeams/{teamId}` allowlist;
- flip `appConfig/security.requireVerifiedTeams` on when the allowlist has good coverage;
- review in-app reports and reclaim impersonated teams.

The script defaults to `tools/catalog/service-account.json`. Pass `--key <path>` if
your service account key is elsewhere. Never commit the key.

## Day-to-day

```powershell
# See who claimed a team, its users, and any open reports
python tools/admin/manage_teams.py claim-info --team-id 25310

# Add a team to the allowlist
python tools/admin/manage_teams.py verify --team-id 25310

# Remove it
python tools/admin/manage_teams.py unverify --team-id 25310

# See what is currently on the allowlist
python tools/admin/manage_teams.py list-verified
```

## Turning enforcement on

Enforcement is off until `appConfig/security.requireVerifiedTeams` is true. While off,
the app works exactly as before but still shows claim/verification badges and allows
reports. Before turning it on, verify every team you expect to use the app.

```powershell
python tools/admin/manage_teams.py set-enforcement --on
python tools/admin/manage_teams.py set-enforcement --off
```

When on:

- unverified teams cannot create an account, join, or rotate codes;
- unverified teams cannot confirm loans or sign for them;
- verified teams behave normally.

## Reports and reclaims

```powershell
python tools/admin/manage_teams.py list-reports
python tools/admin/manage_teams.py list-reports --all
python tools/admin/manage_teams.py resolve-report --report-id ABC123
```

To move a team to its real coach, the coach must first sign up in the app so an auth
account exists for their email. Then:

```powershell
# dry run
python tools/admin/manage_teams.py reclaim \
  --team-id 25310 --coach-email coach@example.com

# apply
python tools/admin/manage_teams.py reclaim \
  --team-id 25310 --coach-email coach@example.com --confirm
```

A reclaim deletes every `users` doc for that team, creates fresh invite codes and a
`teamDirectory` owned by the coach, marks the team verified, creates the coach's
`users` doc as `COACH`, and resolves open reports. The new member and coach codes are
printed so you can pass them to the coach; the coach can rotate them from Team account
in the app.
