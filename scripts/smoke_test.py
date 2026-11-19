#!/usr/bin/env python3
"""Check the browser entry point and authenticated gameplay against a running server."""
import json
import sys
import urllib.error
import urllib.request
import uuid

BASE_URL = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080"


def request(method, path, data=None, expected=200, token=None, raw=False):
    headers = {"Content-Type": "application/json"} if data is not None else {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
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
    if raw or not body:
        return body or None
    return json.loads(body)


def main():
    page = request("GET", "/", raw=True)
    assert 'id="auth-form"' in page
    assert 'href="styles.css"' in page and request("GET", "/styles.css", raw=True)
    assert 'src="app.js"' in page and request("GET", "/app.js", raw=True)
    request("GET", "/api/players", expected=401)

    suffix = uuid.uuid4().hex[:10]
    credentials = {"username": "test_" + suffix, "password": uuid.uuid4().hex}
    signup = request("POST", "/api/auth/signup", credentials, expected=201)
    player_path = f"/api/players/{signup['playerId']}"
    token = signup["accessToken"]
    upgrade_path = None
    try:
        login = request("POST", "/api/auth/login", credentials)
        assert login["playerId"] == signup["playerId"]
        token = login["accessToken"]

        assert request("GET", "/api/players", token=token)["id"] == signup["playerId"]
        assert request("GET", player_path, token=token)["cookies"] == 0
        assert isinstance(request("GET", "/api/upgrades", token=token), list)
        request("GET", f"/api/players/{signup['playerId'] + 1}", expected=403, token=token)
        upgrade = request("POST", "/api/upgrades", {
            "name": "upgrade_" + suffix,
            "description": "Smoke test upgrade",
            "price": 2,
            "clickBonus": 2,
            "passiveBonus": 1,
        }, expected=201, token=token)
        upgrade_id = upgrade["id"]
        upgrade_path = f"/api/upgrades/{upgrade_id}"
        purchase_path = f"{player_path}/upgrades/{upgrade_id}/purchase"

        assert request("GET", upgrade_path, token=token)["name"] == upgrade["name"]
        request("PUT", player_path, {"username": "renamed_" + suffix}, token=token)
        request("PUT", upgrade_path, {
            "name": "updated_" + suffix,
            "description": "Updated",
            "price": 2,
            "clickBonus": 2,
            "passiveBonus": 1,
        }, token=token)
        request("POST", purchase_path, expected=409, token=token)
        request("POST", f"{player_path}/click", token=token)
        result = request("POST", f"{player_path}/click", token=token)
        assert result["cookies"] == 2
        result = request("POST", purchase_path, token=token)
        assert result["cookies"] == 0
        assert result["cookiesPerClick"] == 3
        assert request("POST", f"{player_path}/collect", token=token)["cookies"] == 1
        assert len(request("GET", f"{player_path}/purchases", token=token)) == 1
        assert isinstance(request("GET", "/api/players/leaderboard", token=token), list)
        request("DELETE", upgrade_path, expected=409, token=token)
    finally:
        try:
            request("DELETE", player_path, expected=204, token=token)
        finally:
            if upgrade_path:
                request("DELETE", upgrade_path, expected=204, token=token)
    print("Web smoke test: PASS")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Web smoke test: FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
