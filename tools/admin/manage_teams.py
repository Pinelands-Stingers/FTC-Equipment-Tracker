#!/usr/bin/env python3
"""Organizer tooling for team verification, reports, and reclaims.

All writes here use the Firebase Admin SDK, which bypasses Firestore security
rules. Keep the service account key private; never commit it.

Setup:
    pip install -r tools/catalog/requirements.txt   # provides firebase-admin

The default key path is tools/catalog/service-account.json. Pass --key to use
another copy.

Examples:
    python tools/admin/manage_teams.py claim-info --team-id 25310
    python tools/admin/manage_teams.py verify --team-id 25310
    python tools/admin/manage_teams.py set-enforcement --on
    python tools/admin/manage_teams.py list-reports
    python tools/admin/manage_teams.py resolve-report --report-id ABC123
    python tools/admin/manage_teams.py reclaim --team-id 25310 \
        --coach-email coach@example.com --confirm
"""

from __future__ import annotations

import argparse
import os
import secrets
import sys
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_KEY = os.path.normpath(os.path.join(HERE, "..", "catalog", "service-account.json"))

BATCH_LIMIT = 450
CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def now_millis() -> int:
    return int(datetime.now(timezone.utc).timestamp() * 1000)


def generate_code() -> str:
    return "".join(secrets.choice(CODE_ALPHABET) for _ in range(8))


def init_firebase(key_path: str):
    key_path = os.path.abspath(os.path.expanduser(key_path))
    if not os.path.exists(key_path):
        print(f"service account key not found: {key_path}", file=sys.stderr)
        print(
            "Pass --key <path> or place the key at tools/catalog/service-account.json.",
            file=sys.stderr,
        )
        raise SystemExit(1)

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("firebase-admin is not installed. Run:", file=sys.stderr)
        print("  pip install -r tools/catalog/requirements.txt", file=sys.stderr)
        raise SystemExit(1)

    firebase_admin.initialize_app(credentials.Certificate(key_path))
    return firestore.client()


def chunked(seq, size):
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


def delete_refs(db, refs) -> int:
    refs = list(refs)
    removed = 0
    for group in chunked(refs, BATCH_LIMIT):
        batch = db.batch()
        for ref in group:
            batch.delete(ref)
        batch.commit()
        removed += len(group)
    return removed


def cmd_set_enforcement(db, on: bool) -> int:
    db.document("appConfig/security").set(
        {
            "requireVerifiedTeams": on,
            "updatedAt": now_iso(),
        }
    )
    state = "ON" if on else "OFF"
    print(f"verification enforcement is now {state}")
    if on:
        print("Only teams in verifiedTeams/ can create accounts, join, or confirm loans.")
    return 0


def cmd_verify(db, team_id: str, name: str) -> int:
    payload = {
        "verified": True,
        "verifiedAt": now_iso(),
        "verifiedBy": "admin-script",
    }
    if name:
        payload["name"] = name
    db.document(f"verifiedTeams/{team_id}").set(payload)
    print(f"verified team #{team_id}")
    return 0


def cmd_unverify(db, team_id: str) -> int:
    db.document(f"verifiedTeams/{team_id}").delete()
    print(f"removed verification for team #{team_id}")
    return 0


def cmd_list_verified(db) -> int:
    docs = list(db.collection("verifiedTeams").stream())
    if not docs:
        print("no verified teams")
        return 0
    for doc in sorted(docs, key=lambda d: d.id):
        data = doc.to_dict() or {}
        name = data.get("name", "")
        suffix = f"  {name}" if name else ""
        print(f"#{doc.id}{suffix}")
    print(f"\n{len(docs)} verified team(s)")
    return 0


def cmd_list_reports(db, include_resolved: bool) -> int:
    query = db.collection("teamReports")
    if not include_resolved:
        query = query.where("status", "==", "open")
    reports = sorted(
        query.stream(),
        key=lambda doc: (doc.to_dict() or {}).get("createdAt", 0),
    )
    if not reports:
        print("no reports")
        return 0
    for doc in reports:
        data = doc.to_dict() or {}
        print(
            f"{doc.id}  team=#{data.get('teamId')}  status={data.get('status')}  "
            f"by={data.get('reportedByEmail')}"
        )
        print(f"  reason: {data.get('reason')}")
    return 0


def cmd_resolve_report(db, report_id: str) -> int:
    ref = db.document(f"teamReports/{report_id}")
    if not ref.get().exists:
        print(f"report {report_id} not found", file=sys.stderr)
        return 1
    ref.update({"status": "resolved", "resolvedAt": now_iso()})
    print(f"report {report_id} marked resolved")
    return 0


def cmd_claim_info(db, team_id: str) -> int:
    directory = db.document(f"teamDirectory/{team_id}").get()
    verified = db.document(f"verifiedTeams/{team_id}").get()
    users = list(db.collection("users").where("teamId", "==", team_id).stream())
    reports = [
        doc
        for doc in db.collection("teamReports").where("teamId", "==", team_id).stream()
        if (doc.to_dict() or {}).get("status") == "open"
    ]

    print(f"team #{team_id}")
    print(f"  verified : {verified.exists}")
    if directory.exists:
        data = directory.to_dict() or {}
        print(f"  claimed  : {data.get('createdByEmail') or 'unrecorded'}")
        print(f"  claimedAt: {data.get('createdAt')}")
        print(f"  claimedBy: {data.get('createdBy')}")
    else:
        print("  claimed  : no")
    print(f"  users    : {len(users)}")
    for user in users:
        data = user.to_dict() or {}
        print(
            f"    - {data.get('displayName', '?')} <{data.get('role', '?')}> "
            f"uid={user.id}"
        )
    print(f"  reports  : {len(reports)} open")
    for doc in reports:
        data = doc.to_dict() or {}
        print(f"    - {doc.id}: {data.get('reason')}")
    return 0


def cmd_reclaim(db, team_id: str, coach_email: str, confirm: bool) -> int:
    from firebase_admin import auth

    try:
        coach = auth.get_user_by_email(coach_email)
    except Exception as exc:  # noqa: BLE001 - surfaced to the organizer
        print(f"could not find an account for {coach_email}: {exc}", file=sys.stderr)
        print("Ask the coach to sign up in the app first, then run this again.", file=sys.stderr)
        return 1

    users = list(db.collection("users").where("teamId", "==", team_id).stream())
    reports = [
        doc
        for doc in db.collection("teamReports").where("teamId", "==", team_id).stream()
        if (doc.to_dict() or {}).get("status") == "open"
    ]

    print(f"reclaim plan for team #{team_id}")
    print(f"  set coach account : {coach_email} (uid {coach.uid})")
    print(f"  delete users docs : {len(users)}")
    print("  create fresh teamInvites and teamDirectory owned by the coach")
    print("  mark verifiedTeams")
    print(f"  resolve reports   : {len(reports)}")

    if not confirm:
        print("\nDry run only. Re-run with --confirm to apply.")
        return 0

    member_code = generate_code()
    coach_code = generate_code()
    while coach_code == member_code:
        coach_code = generate_code()
    created_at = now_millis()

    delete_refs(db, [doc.reference for doc in users])
    db.document(f"teamInvites/{team_id}").set(
        {
            "memberCode": member_code,
            "coachCode": coach_code,
            "createdBy": coach.uid,
            "createdAt": created_at,
            "updatedAt": created_at,
        }
    )
    db.document(f"teamDirectory/{team_id}").set(
        {
            "createdBy": coach.uid,
            "createdByEmail": coach_email,
            "createdAt": created_at,
        }
    )
    db.document(f"verifiedTeams/{team_id}").set(
        {
            "verified": True,
            "verifiedAt": now_iso(),
            "verifiedBy": "reclaim",
        }
    )
    db.document(f"users/{coach.uid}").set(
        {
            "teamId": team_id,
            "role": "COACH",
            "displayName": coach.display_name or coach_email.split("@")[0],
            "createdAt": created_at,
            "inviteCode": coach_code,
        }
    )
    for report in reports:
        report.reference.update({"status": "resolved", "resolvedAt": now_iso()})

    print(f"reclaimed team #{team_id} for {coach_email}")
    print(f"  member code: {member_code}")
    print(f"  coach code : {coach_code}")
    print("The coach can view or rotate these from Team account in the app.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Manage team verification and reclaims")
    parser.add_argument(
        "--key",
        default=DEFAULT_KEY,
        help=f"path to the Firebase service account JSON (default: {DEFAULT_KEY})",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    enforcement = sub.add_parser("set-enforcement", help="turn verification gating on or off")
    group = enforcement.add_mutually_exclusive_group(required=True)
    group.add_argument("--on", action="store_true")
    group.add_argument("--off", action="store_true")

    verify = sub.add_parser("verify", help="add a team to the allowlist")
    verify.add_argument("--team-id", required=True)
    verify.add_argument("--name", default="")

    unverify = sub.add_parser("unverify", help="remove a team from the allowlist")
    unverify.add_argument("--team-id", required=True)

    sub.add_parser("list-verified", help="list teams in the allowlist")

    reports = sub.add_parser("list-reports", help="list problem reports")
    reports.add_argument("--all", action="store_true", help="include resolved reports")

    resolve = sub.add_parser("resolve-report", help="mark a report resolved")
    resolve.add_argument("--report-id", required=True)

    info = sub.add_parser("claim-info", help="show who claimed a team and its reports")
    info.add_argument("--team-id", required=True)

    reclaim = sub.add_parser("reclaim", help="transfer a team to a real coach account")
    reclaim.add_argument("--team-id", required=True)
    reclaim.add_argument("--coach-email", required=True)
    reclaim.add_argument("--confirm", action="store_true", help="apply the destructive changes")

    args = parser.parse_args()
    db = init_firebase(args.key)

    if args.command == "set-enforcement":
        return cmd_set_enforcement(db, on=args.on)
    if args.command == "verify":
        return cmd_verify(db, args.team_id, args.name)
    if args.command == "unverify":
        return cmd_unverify(db, args.team_id)
    if args.command == "list-verified":
        return cmd_list_verified(db)
    if args.command == "list-reports":
        return cmd_list_reports(db, include_resolved=args.all)
    if args.command == "resolve-report":
        return cmd_resolve_report(db, args.report_id)
    if args.command == "claim-info":
        return cmd_claim_info(db, args.team_id)
    if args.command == "reclaim":
        return cmd_reclaim(db, args.team_id, args.coach_email, confirm=args.confirm)
    return 1


if __name__ == "__main__":
    sys.exit(main())
