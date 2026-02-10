from dataclasses import dataclass


@dataclass(frozen=True)
class ConfigModel:
    """Runtime thresholds used by disappearance tracking transitions."""

    unknown_after_scans: int = 1
    unknown_after_minutes: int = 0
    missing_after_scans: int = 2
    missing_after_minutes: int = 5
    sold_after_scans: int = 3
    sold_after_minutes: int = 30
    expired_after_scans: int = 6
    expired_after_minutes: int = 120
