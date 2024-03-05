# Trading System

## Overview

This project implements a simple trading system that matches buy and sell orders for financial instruments. It consists of several components:

- `Order`: Represents an order to buy or sell a financial instrument.
- `Instrument`: Represents a financial instrument (e.g., stock, bond).
- `CompositeInstrument`: Represents a composite instrument composed of multiple individual instruments.
- `OrderManager`: Manages the lifecycle of orders, including adding, canceling, and retrieving orders.
- `MarketDataProvider`: Provides market data such as prices for instruments.
- `TradingEngine`: Matches buy and sell orders based on certain criteria.

## Installation

To build and run the project, ensure you have Maven installed on your system. Then, clone the repository and navigate to the project directory:

```
git clone <repository-url>
cd trading-system
```

Use Maven to compile and package the project:

```
mvn clean package
```

This will generate a JAR file containing the compiled code and dependencies.

## Usage

The main entry point of the trading system is the `SimpleTradingEngine` class. You can create an instance of this class, providing implementations of `OrderManager` and `MarketDataProvider` interfaces.

```
// Example usage
MarketDataProvider marketDataProvider = new MockMarketDataProvider(/* market data */);
OrderManager orderManager = new InMemoryOrderManager();
TradingEngine tradingEngine = new SimpleTradingEngine(orderManager, marketDataProvider);
```

You can then use the `TradingEngine` instance to match orders and execute trades.

```
// Example: Match orders for a specific instrument
tradingEngine.matchOrders("AAPL");
```

## Testing

The project includes unit tests written using JUnit. To run the tests, use the following Maven command:

```
mvn test
```

Additionally, you can generate a test coverage report using the following Maven command:

```
mvn clean test jacoco:report
```

This will generate a coverage report in HTML format under the `target/site/jacoco` directory.

## License

This project is licensed under the [MIT License](LICENSE).
