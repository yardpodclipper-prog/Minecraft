package com.yourname.gtstracker.command;

/**
 * @deprecated Non-runtime legacy command shim.
 * <p>
 * Command registration is canonically owned by {@code com.yourname.gtstracker.ui.CommandHandler},
 * which wires both {@code /gts} and {@code /gtstracker} aliases in one place.
 */
@Deprecated(forRemoval = true)
public final class GtsGuiCommand {
    private GtsGuiCommand() {
        throw new UnsupportedOperationException("Legacy class is non-runtime. Use ui.CommandHandler.");
    }
}
