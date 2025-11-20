package uk.co.finleyofthewoods.pickaxedrill.utils;

import net.minecraft.util.math.Direction;

public class DrillLogic {
    // Define drill directions
    public enum DrillDirection{
        // --- Horizontal Drilling ---
        NORTH, EAST, SOUTH, WEST,
        // --- Upward Drilling ---
        UP_NORTH, UP_SOUTH, UP_EAST, UP_WEST,
        // --- Downward Drilling ---
        DOWN_NORTH, DOWN_SOUTH, DOWN_EAST, DOWN_WEST
    }

    public record DrillConfig(
            int directionSign, // Direction (Negative = -1, Positive = 1) - Affects only the `depth` field
            String depth,      // Depth
            String height,     // Height
            String width       // Width
    ) {}

    public static DrillConfig getDrillConfig(DrillDirection direction) {
        return switch (direction) {
            // --- Horizontal Drilling ---
            case NORTH                  -> new DrillConfig(-1,"Z", "Y", "X");
            case EAST                   -> new DrillConfig( 1,"X", "Y", "Z");
            case SOUTH                  -> new DrillConfig( 1,"Z", "Y", "X");
            case WEST                   -> new DrillConfig(-1,"X", "Y", "Z");
            // --- Upward Drilling ---
            case UP_NORTH, UP_SOUTH     -> new DrillConfig(-1,"Y", "Z", "X");
            case UP_EAST, UP_WEST       -> new DrillConfig(-1,"Y", "X", "Z");
            // --- Downward Drilling ---
            case DOWN_NORTH, DOWN_SOUTH -> new DrillConfig( 1,"Y", "Z", "X");
            case DOWN_EAST, DOWN_WEST   -> new DrillConfig( 1,"Y", "X", "Z");
        };
    }

    public static DrillDirection getDrillDirection(Direction direction, Direction.Axis axis, float pitch) {
        // Return NORTH
        if (axis == Direction.Axis.Z && direction == Direction.NORTH) {
            return DrillDirection.NORTH;
        }
        // Return EAST
        if (axis == Direction.Axis.X && direction == Direction.EAST) {
            return DrillDirection.EAST;
        }
        // Return SOUTH
        if (axis == Direction.Axis.Z && direction == Direction.SOUTH) {
            return DrillDirection.SOUTH;
        }
        // Return WEST
        if (axis == Direction.Axis.X && direction == Direction.WEST) {
            return DrillDirection.WEST;
        }

        // Return UP_NORTH
        if (axis == Direction.Axis.Y && direction == Direction.NORTH && pitch < 0) {
            return DrillDirection.UP_NORTH;
        }
        // Return UP_EAST
        if (axis == Direction.Axis.Y && direction == Direction.EAST && pitch < 0) {
            return DrillDirection.UP_EAST;
        }
        // Return UP_SOUTH
        if (axis == Direction.Axis.Y && direction == Direction.SOUTH && pitch < 0) {
            return DrillDirection.UP_SOUTH;
        }
        // Return UP_WEST
        if (axis == Direction.Axis.Y && direction == Direction.WEST && pitch < 0) {
            return DrillDirection.UP_WEST;
        }

        // Return DOWN_NORTH
        if (axis == Direction.Axis.Y && direction == Direction.NORTH && pitch > 0) {
            return DrillDirection.DOWN_NORTH;
        }
        // Return UP_EAST
        if (axis == Direction.Axis.Y && direction == Direction.EAST && pitch > 0) {
            return DrillDirection.DOWN_EAST;
        }
        // Return UP_SOUTH
        if (axis == Direction.Axis.Y && direction == Direction.SOUTH && pitch > 0) {
            return DrillDirection.DOWN_SOUTH;
        }
        // Return UP_WEST
        if (axis == Direction.Axis.Y && direction == Direction.WEST && pitch > 0) {
            return DrillDirection.DOWN_WEST;
        }
        throw new IllegalArgumentException("Invalid directions provided. direction: "+direction+", axis: "+axis+", pitch: "+pitch);
    }
}
