#!/usr/bin/env python3
"""Builds the Cloud Run environment file (YAML) from environment variables set by the deploy workflow.

Usage: render-cloud-run-env.py <output-file>
Fails with a plain-language message when something is missing, so a forgotten GitHub secret is easy to spot.
"""
import json
import os
import sys

REQUIRED = [
    "DB_HOST",
    "DB_NAME",
    "DB_USERNAME",
    "DB_PASSWORD",
    "ADMIN_USERNAME",
    "ADMIN_PASSWORD_HASH",
    "JWT_SECRET",
    "TMDB_ACCESS_TOKEN",
    "PROXY_SHARED_SECRET",
    "FRONTEND_ORIGIN",
]


def main(output_path: str) -> None:
    missing = [name for name in REQUIRED if not os.environ.get(name, "").strip()]
    if missing:
        sys.exit("Faltam secrets ou variáveis no GitHub: " + ", ".join(missing) + " (veja docs/deploy.md)")

    host = os.environ["DB_HOST"].strip()
    if "-pooler" in host:
        sys.exit("DB_HOST usa o endereço 'pooler' do Neon. Use o endereço direto (sem '-pooler'): o Flyway precisa dele.")
    if jwt_too_short(os.environ["JWT_SECRET"]):
        sys.exit("JWT_SECRET precisa ter pelo menos 32 caracteres.")
    if not os.environ["ADMIN_PASSWORD_HASH"].startswith("$2"):
        sys.exit("ADMIN_PASSWORD_HASH deveria começar com $2 (é o hash BCrypt, não a senha em texto).")

    values = {
        "DB_URL": f"jdbc:postgresql://{host}/{os.environ['DB_NAME'].strip()}?sslmode=require",
        "DB_USERNAME": os.environ["DB_USERNAME"].strip(),
        "DB_PASSWORD": os.environ["DB_PASSWORD"],
        "ADMIN_USERNAME": os.environ["ADMIN_USERNAME"].strip(),
        "ADMIN_PASSWORD_HASH": os.environ["ADMIN_PASSWORD_HASH"].strip(),
        "JWT_SECRET": os.environ["JWT_SECRET"],
        "TMDB_ACCESS_TOKEN": os.environ["TMDB_ACCESS_TOKEN"].strip(),
        "PROXY_SHARED_SECRET": os.environ["PROXY_SHARED_SECRET"],
        "FRONTEND_ORIGIN": os.environ["FRONTEND_ORIGIN"].strip().rstrip("/"),
    }

    # JSON strings are valid YAML double-quoted scalars, so no value needs special escaping rules.
    with open(output_path, "w", encoding="utf-8") as output:
        for name, value in values.items():
            output.write(f"{name}: {json.dumps(value)}\n")


def jwt_too_short(secret: str) -> bool:
    return len(secret) < 32


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("uso: render-cloud-run-env.py <arquivo-de-saida>")
    main(sys.argv[1])
