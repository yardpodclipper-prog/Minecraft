from analytics.PriceAnalyzer import PriceAnalyzer, PriceAnalyzerConfig


class StubPriceDAO:
    def __init__(self, pokemon_prices, item_prices):
        self.pokemon_prices = pokemon_prices
        self.item_prices = item_prices

    def get_pokemon_prices_since(self, pokemon_name: str, cutoff_timestamp: int):
        return self.pokemon_prices.get((pokemon_name, cutoff_timestamp), [])

    def get_item_prices_since(self, item_name: str, cutoff_timestamp: int):
        return self.item_prices.get((item_name, cutoff_timestamp), [])


def test_analyze_pokemon_computes_window_stats_and_percent_differences():
    dao = StubPriceDAO(
        pokemon_prices={("Pikachu", 1000): [100, 200, 300, 400]},
        item_prices={},
    )
    analyzer = PriceAnalyzer(dao=dao, config=PriceAnalyzerConfig(minimum_samples=3))

    stats = analyzer.analyze_pokemon("Pikachu", 1000, current_price=300)

    assert stats.sample_count == 4
    assert stats.min_price == 100
    assert stats.max_price == 400
    assert stats.average_price == 250
    assert stats.median_price == 250
    assert stats.percent_diff_from_average == 20.0
    assert stats.percent_diff_from_median == 20.0
    assert stats.meets_minimum_sample_threshold is True


def test_analyze_item_applies_minimum_sample_threshold():
    dao = StubPriceDAO(
        pokemon_prices={},
        item_prices={("Potion", 1000): [90, 100]},
    )
    analyzer = PriceAnalyzer(dao=dao, config=PriceAnalyzerConfig(minimum_samples=3))

    stats = analyzer.analyze_item("Potion", 1000, current_price=95)

    assert stats.sample_count == 2
    assert stats.min_price == 90
    assert stats.max_price == 100
    assert stats.average_price == 95
    assert stats.median_price == 95
    assert stats.percent_diff_from_average == 0.0
    assert stats.percent_diff_from_median == 0.0
    assert stats.meets_minimum_sample_threshold is False


def test_empty_window_returns_safe_defaults():
    dao = StubPriceDAO(pokemon_prices={}, item_prices={})
    analyzer = PriceAnalyzer(dao=dao, config=PriceAnalyzerConfig(minimum_samples=1))

    stats = analyzer.analyze_item("Revive", 1000, current_price=100)

    assert stats.sample_count == 0
    assert stats.min_price == 0.0
    assert stats.max_price == 0.0
    assert stats.average_price == 0.0
    assert stats.median_price == 0.0
    assert stats.percent_diff_from_average is None
    assert stats.percent_diff_from_median is None
    assert stats.meets_minimum_sample_threshold is False
