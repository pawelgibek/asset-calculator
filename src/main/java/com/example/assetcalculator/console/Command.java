package com.example.assetcalculator.console;

import java.util.Arrays;
import java.util.Optional;

/**
 * Commands accepted at the main prompt besides asset symbols.
 */
enum Command {

    REBALANCE,
    EXIT;

    /**
     * Case-insensitive match; empty when the input is not a command (e.g. an asset symbol).
     */
    static Optional<Command> parse(String input) {
        return Arrays.stream(values())
                .filter(command -> command.name().equalsIgnoreCase(input))
                .findFirst();
    }
}
