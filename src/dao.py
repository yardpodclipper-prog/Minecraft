from __future__ import annotations

import sqlite3
from datetime import datetime, timedelta, timezone
from typing import Iterable

from .config_model import ConfigModel


class ListingsDAO:
    def __init__(self, connection: sqlite3.Connection):
        self.connection = connection
        self.connection.row_factory = sqlite3.Row

    def ensure_schema(self) -> None:
        self.connection.execute(
            """
            CREATE TABLE IF NOT EXISTS listings (
                id TEXT PRIMARY KEY,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                last_seen_at TEXT,
                expires_at TEXT,
                missing_since TEXT,
                missing_scans INTEGER NOT NULL DEFAULT 0,
                unknown_at TEXT,
                missing_at TEXT,
                sold_at TEXT,
                expired_at TEXT,
                updated_at TEXT
            )
            """
        )

        # migration-safe column additions for existing tables.
        required_columns: dict[str, str] = {
            "missing_since": "TEXT",
            "missing_scans": "INTEGER NOT NULL DEFAULT 0",
            "unknown_at": "TEXT",
            "missing_at": "TEXT",
            "sold_at": "TEXT",
            "expired_at": "TEXT",
            "updated_at": "TEXT",
        }

        existing = {
            row["name"]
            for row in self.connection.execute("PRAGMA table_info(listings)").fetchall()
        }
        for column, ddl in required_columns.items():
            if column not in existing:
                self.connection.execute(f"ALTER TABLE listings ADD COLUMN {column} {ddl}")

        self.connection.commit()

    def upsert_seen_listing(
        self,
        listing_id: str,
        now: datetime,
        expires_at: datetime | None = None,
    ) -> None:
        now_iso = _to_iso(now)
        expires_iso = _to_iso(expires_at) if expires_at else None
        self.connection.execute(
            """
            INSERT INTO listings (
                id,
                status,
                last_seen_at,
                expires_at,
                missing_since,
                missing_scans,
                updated_at
            ) VALUES (?, 'ACTIVE', ?, ?, NULL, 0, ?)
            ON CONFLICT(id) DO UPDATE SET
                status='ACTIVE',
                last_seen_at=excluded.last_seen_at,
                expires_at=COALESCE(excluded.expires_at, listings.expires_at),
                missing_since=NULL,
                missing_scans=0,
                updated_at=excluded.updated_at
            """,
            (listing_id, now_iso, expires_iso, now_iso),
        )

    def mark_absent_listings(
        self,
        seen_listing_ids: Iterable[str],
        now: datetime,
        config: ConfigModel,
    ) -> None:
        now_iso = _to_iso(now)
        seen_listing_ids = list(seen_listing_ids)
        placeholders = ",".join("?" for _ in seen_listing_ids)
        params: list[str] = [now_iso, now_iso]

        base_query = """
            UPDATE listings
            SET
                missing_since = COALESCE(missing_since, ?),
                missing_scans = missing_scans + 1,
                updated_at = ?
            WHERE status IN ('ACTIVE', 'UNKNOWN', 'MISSING')
        """

        if placeholders:
            base_query += f" AND id NOT IN ({placeholders})"
            params.extend(list(seen_listing_ids))

        self.connection.execute(base_query, params)
        self.transition_absence_states(now, config)
        self.connection.commit()

    def transition_absence_states(self, now: datetime, config: ConfigModel) -> None:
        now_iso = _to_iso(now)
        self._transition_to_unknown(now, now_iso, config)
        self._transition_to_missing(now, now_iso, config)
        self._transition_to_terminal(now, now_iso, config)

    def _transition_to_unknown(self, now: datetime, now_iso: str, config: ConfigModel) -> None:
        cutoff = _to_iso(now - timedelta(minutes=config.unknown_after_minutes))
        self.connection.execute(
            """
            UPDATE listings
            SET
                status = 'UNKNOWN',
                unknown_at = COALESCE(unknown_at, ?),
                updated_at = ?
            WHERE status = 'ACTIVE'
              AND missing_since IS NOT NULL
              AND missing_scans >= ?
              AND missing_since <= ?
            """,
            (now_iso, now_iso, config.unknown_after_scans, cutoff),
        )

    def _transition_to_missing(self, now: datetime, now_iso: str, config: ConfigModel) -> None:
        cutoff = _to_iso(now - timedelta(minutes=config.missing_after_minutes))
        self.connection.execute(
            """
            UPDATE listings
            SET
                status = 'MISSING',
                missing_at = COALESCE(missing_at, ?),
                updated_at = ?
            WHERE status IN ('ACTIVE', 'UNKNOWN')
              AND missing_since IS NOT NULL
              AND missing_scans >= ?
              AND missing_since <= ?
            """,
            (now_iso, now_iso, config.missing_after_scans, cutoff),
        )

    def _transition_to_terminal(self, now: datetime, now_iso: str, config: ConfigModel) -> None:
        sold_cutoff = _to_iso(now - timedelta(minutes=config.sold_after_minutes))
        expired_cutoff = _to_iso(now - timedelta(minutes=config.expired_after_minutes))

        self.connection.execute(
            """
            UPDATE listings
            SET
                status = 'EXPIRED',
                expired_at = COALESCE(expired_at, ?),
                updated_at = ?
            WHERE status IN ('UNKNOWN', 'MISSING')
              AND missing_since IS NOT NULL
              AND missing_scans >= ?
              AND missing_since <= ?
            """,
            (now_iso, now_iso, config.expired_after_scans, expired_cutoff),
        )

        # Listings not old enough for EXPIRED become SOLD after sold thresholds.
        self.connection.execute(
            """
            UPDATE listings
            SET
                status = 'SOLD',
                sold_at = COALESCE(sold_at, ?),
                updated_at = ?
            WHERE status IN ('UNKNOWN', 'MISSING')
              AND missing_since IS NOT NULL
              AND missing_scans >= ?
              AND missing_since <= ?
              AND status != 'EXPIRED'
            """,
            (now_iso, now_iso, config.sold_after_scans, sold_cutoff),
        )


def _to_iso(dt: datetime) -> str:
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc).isoformat()
