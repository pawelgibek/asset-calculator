# Asset Calculator

A console application for managing a portfolio's target allocation. It shows how far
the current allocation is from the target, how many shares to buy or sell to reach it,
and lets you rebalance the portfolio.

## Features

- Portfolio overview: target and current allocation, variance, unit price, shares,
  invested amount and the suggested trade (BUY / SELL) for each asset.
- Changing the target allocation of a selected asset, with validation.
- Rebalancing: simulates setting current allocations to their targets and reports any unallocated funds. It does not place real trades.

The initial portfolio is worth **100,000 USD** and holds these assets:

| Symbol | Unit price (USD) |
|--------|-----------------:|
| IBM    | 150 |
| MSFT   | 90  |
| ORCL   | 220 |
| AAPL   | 450 |
| HD     | 70  |

## Requirements

- JDK 25 or newer
- Maven (the bundled Maven Wrapper is enough)

Built with Java 25, Spring Boot 4.1, JUnit 5 and AssertJ.

## Running

```bash
./mvnw package
java -jar target/asset-calculator-0.0.1-SNAPSHOT.jar
```

On Windows, use:

```bat
mvnw.cmd package
java -jar target\asset-calculator-0.0.1-SNAPSHOT.jar
```

## Usage

After it starts, the application prints the portfolio and waits for input:

```text
Security   Target %     Current %    Variance     Unit Price   Shares       Invested Amount    Shares to Buy / Sell
IBM        20%          10%          +10%         150          66.67        10000.00           BUY 66.67 shares
ORCL       20%          30%          -10%         220          136.36       30000.00           SELL 45.45 shares
...

Available assets: IBM, MSFT, ORCL, AAPL, HD
Commands: REBALANCE | EXIT
Select an asset or enter a command:
```

| Input | Action |
|-------|--------|
| asset symbol, e.g. `IBM` | set a new target allocation (%) for that asset |
| `REBALANCE` | set current allocations to their targets and print a summary |
| `EXIT` | quit the application |

Input is case-insensitive. Amounts and share counts are rounded to 2 decimal places (`HALF_UP`).

### Validation rules

- The target must be a number with up to 2 decimal places and a `.` separator, e.g. `12.5`.
- Each target must be between 0 and 100%.
- The sum of all targets cannot exceed 100%. It can be lower; the remaining part is
  reported as unallocated funds after rebalancing.

## Project structure

```text
com.example.assetcalculator
├── catalog   supported assets and their prices
├── model     domain logic: Portfolio, Position, validation rules
├── console   user interface: input loop, commands, output formatting
└── config    initial portfolio definition
```

All business rules live in the `model` package, so they apply regardless of the user interface.

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `asset-calculator.console.enabled` | `true` | set to `false` to start without the interactive console |

## Tests

```bash
./mvnw test
```

The tests cover the domain model (calculations, rounding, validation), console
input/output, and the Spring configuration.

## Limitations

- Asset prices and the portfolio value are hardcoded.
- The portfolio state is not saved between runs.
