import sqlite3
from datetime import datetime, timedelta, timezone

from src.config_model import ConfigModel
from src.dao import ListingsDAO


UTC = timezone.utc


def _row(conn: sqlite3.Connection, listing_id: str):
    return conn.execute("SELECT * FROM listings WHERE id = ?", (listing_id,)).fetchone()


def test_schema_migration_columns_added():
    conn = sqlite3.connect(":memory:")
    conn.execute("CREATE TABLE listings (id TEXT PRIMARY KEY, status TEXT NOT NULL DEFAULT 'ACTIVE')")

    dao = ListingsDAO(conn)
    dao.ensure_schema()

    cols = {r[1] for r in conn.execute("PRAGMA table_info(listings)").fetchall()}
    assert {"missing_since", "missing_scans", "unknown_at", "missing_at", "sold_at", "expired_at"}.issubset(cols)


def test_absence_transitions_unknown_missing_sold():
    conn = sqlite3.connect(":memory:")
    dao = ListingsDAO(conn)
    dao.ensure_schema()

    cfg = ConfigModel(
        unknown_after_scans=1,
        unknown_after_minutes=0,
        missing_after_scans=2,
        missing_after_minutes=0,
        sold_after_scans=3,
        sold_after_minutes=0,
        expired_after_scans=100,
        expired_after_minutes=999,
    )
    now = datetime(2026, 1, 1, tzinfo=UTC)

    dao.upsert_seen_listing("A", now)
    dao.mark_absent_listings([], now + timedelta(minutes=1), cfg)
    assert _row(conn, "A")["status"] == "UNKNOWN"

    dao.mark_absent_listings([], now + timedelta(minutes=2), cfg)
    assert _row(conn, "A")["status"] == "MISSING"

    dao.mark_absent_listings([], now + timedelta(minutes=3), cfg)
    row = _row(conn, "A")
    assert row["status"] == "SOLD"
    assert row["sold_at"] is not None


def test_absence_transitions_to_expired():
    conn = sqlite3.connect(":memory:")
    dao = ListingsDAO(conn)
    dao.ensure_schema()

    cfg = ConfigModel(
        unknown_after_scans=1,
        unknown_after_minutes=0,
        missing_after_scans=1,
        missing_after_minutes=0,
        sold_after_scans=10,
        sold_after_minutes=999,
        expired_after_scans=2,
        expired_after_minutes=0,
    )

    now = datetime(2026, 1, 1, tzinfo=UTC)
    dao.upsert_seen_listing("B", now)
    dao.mark_absent_listings([], now + timedelta(minutes=1), cfg)
    dao.mark_absent_listings([], now + timedelta(minutes=2), cfg)

    row = _row(conn, "B")
    assert row["status"] == "EXPIRED"
    assert row["expired_at"] is not None
