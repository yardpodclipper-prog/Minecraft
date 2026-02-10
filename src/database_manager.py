from __future__ import annotations

import sqlite3
from datetime import datetime
from typing import Iterable

from .config_model import ConfigModel
from .dao import ListingsDAO


class DatabaseManager:
    def __init__(self, db_path: str, config: ConfigModel | None = None):
        self.connection = sqlite3.connect(db_path)
        self.dao = ListingsDAO(self.connection)
        self.config = config or ConfigModel()

    def initialize(self) -> None:
        self.dao.ensure_schema()

    def record_scan_window(
        self,
        seen_listing_ids: Iterable[str],
        now: datetime,
    ) -> None:
        seen_listing_ids = list(seen_listing_ids)
        for listing_id in seen_listing_ids:
            self.dao.upsert_seen_listing(listing_id, now)
        self.dao.mark_absent_listings(seen_listing_ids, now, self.config)

    def transition_disappearance_states(self, now: datetime) -> None:
        self.dao.transition_absence_states(now, self.config)
        self.connection.commit()
