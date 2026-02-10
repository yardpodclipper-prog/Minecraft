from __future__ import annotations

from dataclasses import dataclass
from statistics import median
from typing import Iterable, Protocol

from database.models.PriceStats import PriceStats


class PriceDAO(Protocol):
    """DAO contract used by PriceAnalyzer to retrieve historical prices."""

    def get_pokemon_prices_since(self, pokemon_name: str, cutoff_timestamp: int) -> Iterable[float]:
        ...

    def get_item_prices_since(self, item_name: str, cutoff_timestamp: int) -> Iterable[float]:
        ...


@dataclass(frozen=True)
class PriceAnalyzerConfig:
    minimum_samples: int = 1


class PriceAnalyzer:
    """Compute price metrics from DAO query outputs for pokemon and items."""

    def __init__(self, dao: PriceDAO, config: PriceAnalyzerConfig):
        self._dao = dao
        self._config = config

    def analyze_pokemon(self, pokemon_name: str, cutoff_timestamp: int, current_price: float) -> PriceStats:
        prices = self._dao.get_pokemon_prices_since(pokemon_name, cutoff_timestamp)
        return self._build_price_stats(prices=prices, current_price=current_price)

    def analyze_item(self, item_name: str, cutoff_timestamp: int, current_price: float) -> PriceStats:
        prices = self._dao.get_item_prices_since(item_name, cutoff_timestamp)
        return self._build_price_stats(prices=prices, current_price=current_price)

    def _build_price_stats(self, prices: Iterable[float], current_price: float) -> PriceStats:
        values = [float(value) for value in prices]
        sample_count = len(values)

        if sample_count == 0:
            return PriceStats(
                sample_count=0,
                min_price=0.0,
                max_price=0.0,
                average_price=0.0,
                median_price=0.0,
                percent_diff_from_average=None,
                percent_diff_from_median=None,
                meets_minimum_sample_threshold=False,
            )

        min_price = min(values)
        max_price = max(values)
        average_price = sum(values) / sample_count
        median_price = float(median(values))

        percent_diff_from_average = self._percent_difference(current_price, average_price)
        percent_diff_from_median = self._percent_difference(current_price, median_price)

        return PriceStats(
            sample_count=sample_count,
            min_price=min_price,
            max_price=max_price,
            average_price=average_price,
            median_price=median_price,
            percent_diff_from_average=percent_diff_from_average,
            percent_diff_from_median=percent_diff_from_median,
            meets_minimum_sample_threshold=sample_count >= self._config.minimum_samples,
        )

    @staticmethod
    def _percent_difference(current_price: float, baseline: float):
        if baseline == 0:
            return None
        return ((current_price - baseline) / baseline) * 100.0
