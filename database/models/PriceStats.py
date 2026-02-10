from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class PriceStats:
    """Aggregated pricing metrics over a cutoff window."""

    sample_count: int
    min_price: float
    max_price: float
    average_price: float
    median_price: float
    percent_diff_from_average: Optional[float]
    percent_diff_from_median: Optional[float]
    meets_minimum_sample_threshold: bool
