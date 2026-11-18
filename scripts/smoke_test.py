#!/usr/bin/env python3
"""End-to-end Sprint 1 smoke test. Requires a running API, no extra packages."""
import json
import sys
import urllib.error
import urllib.request
import uuid

BASE_URL = "http://localhost:8080"


def request(method, path, data=None, expected=200):
    headers = {"Content-Type": "application/json"} if data is not None else {}
    payload = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(
        BASE_URL + path, data=payload, headers=headers, method=method
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            status = response.status
            body = response.read().decode()
    except urllib.error.HTTPError as error:
        status = error.code
        body = error.read().decode()
    if status != expected:
        raise AssertionError(
            f"{method} {path}: expected {expected}, got {status}: {body}"
        )
    return json.loads(body) if body else None


def main():
    suffix = uuid.uuid4().hex[:10]
    player = request("POST", "/api/players", {"username": "test_" + suffix}, 201)
    player_id = player["id"]
    upgrade = request(
        "POST", "/api/upgrades",
        {
            "name": "upgrade_" + suffix,
            "description": "Smoke test upgrade",
            "price": 2,
            "clickBonus": 2,
            "passiveBonus": 0,
        },
        201,
    )
    upgrade_id = upgrade["id"]
    try:
        assert request("GET", f"/api/players/{player_id}")["cookies"] == 0
        assert isinstance(request("GET", "/api/players"), list)
        assert isinstance(request("GET", "/api/upgrades"), list)
        assert request("GET", f"/api/upgrades/{upgrade_id}")["name"] == "upgrade_" + suffix
        request("PUT", f"/api/players/{player_id}", {"username": "renamed_" + suffix})
        request("PUT", f"/api/upgrades/{upgrade_id}", {
            "name": "updated_" + suffix,
            "description": "Updated",
            "price": 2,
            "clickBonus": 2,
            "passiveBonus": 0,
        })
        request("POST", f"/api/players/{player_id}/upgrades/{upgrade_id}/purchase", expected=409)
        request("POST", f"/api/players/{player_id}/click")
        result = request("POST", f"/api/players/{player_id}/click")
        assert result["cookies"] == 2
        result = request("POST", f"/api/players/{player_id}/upgrades/{upgrade_id}/purchase")
        assert result["cookies"] == 0
        assert result["cookiesPerClick"] == 3
        assert len(request("GET", f"/api/players/{player_id}/purchases")) == 1
        assert isinstance(request("GET", "/api/players/leaderboard"), list)
        request("DELETE", f"/api/upgrades/{upgrade_id}", expected=409)
        request("GET", "/api/players/999999999", expected=404)
        print("Sprint 1 smoke test: PASS")
    finally:
        request("DELETE", f"/api/players/{player_id}", expected=204)
        request("DELETE", f"/api/upgrades/{upgrade_id}", expected=204)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Sprint 1 smoke test: FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
