package uk.co.finleyofthewoods.pickaxedrill.utils;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class DrillLogic {
    /// Define drill directions
    public enum DrillDirection{
        /// --- Horizontal Drilling ---
        NORTH, EAST, SOUTH, WEST,
        /// --- Upward Drilling ---
        UP_NORTH, UP_SOUTH, UP_EAST, UP_WEST,
        /// --- Downward Drilling ---
        DOWN_NORTH, DOWN_SOUTH, DOWN_EAST, DOWN_WEST
    }

    public record DrillParams(
            int directionSign, /// Direction (Negative = -1, Positive = 1) - Affects only the `depth` field
            Axis depth,        /// Depth
            Axis height,       /// Height
            Axis width         /// Width
    ) {}

    public static DrillParams getDrillConfig(DrillDirection direction) {
        return switch (direction) {
            /// --- Horizontal Drilling ---
            case NORTH                  -> new DrillParams(-1, Axis.Z, Axis.Y, Axis.X);
            case EAST                   -> new DrillParams( 1,Axis.X, Axis.Y, Axis.Z);
            case SOUTH                  -> new DrillParams( 1,Axis.Z, Axis.Y, Axis.X);
            case WEST                   -> new DrillParams(-1,Axis.X, Axis.Y, Axis.Z);
            /// --- Upward Drilling ---
            case UP_NORTH, UP_SOUTH     -> new DrillParams(1,Axis.Y, Axis.Z, Axis.X);
            case UP_EAST, UP_WEST       -> new DrillParams(1,Axis.Y, Axis.X, Axis.Z);
            /// --- Downward Drilling ---
            case DOWN_NORTH, DOWN_SOUTH -> new DrillParams( -1,Axis.Y, Axis.Z, Axis.X);
            case DOWN_EAST, DOWN_WEST   -> new DrillParams( -1,Axis.Y, Axis.X, Axis.Z);
        };
    }

    public static DrillDirection getDrillDirection(Direction direction, Axis axis, float pitch) {
        /// Return NORTH
        if (axis == Axis.Z && direction == Direction.NORTH) {
            return DrillDirection.NORTH;
        }
        /// Return EAST
        if (axis == Axis.X && direction == Direction.EAST) {
            return DrillDirection.EAST;
        }
        /// Return SOUTH
        if (axis == Axis.Z && direction == Direction.SOUTH) {
            return DrillDirection.SOUTH;
        }
        /// Return WEST
        if (axis == Axis.X && direction == Direction.WEST) {
            return DrillDirection.WEST;
        }

        /// Return UP_NORTH
        if (axis == Axis.Y && direction == Direction.NORTH && pitch < 0) {
            return DrillDirection.UP_NORTH;
        }
        /// Return UP_EAST
        if (axis == Axis.Y && direction == Direction.EAST && pitch < 0) {
            return DrillDirection.UP_EAST;
        }
        /// Return UP_SOUTH
        if (axis == Axis.Y && direction == Direction.SOUTH && pitch < 0) {
            return DrillDirection.UP_SOUTH;
        }
        /// Return UP_WEST
        if (axis == Axis.Y && direction == Direction.WEST && pitch < 0) {
            return DrillDirection.UP_WEST;
        }

        /// Return DOWN_NORTH
        if (axis == Axis.Y && direction == Direction.NORTH && pitch > 0) {
            return DrillDirection.DOWN_NORTH;
        }
        /// Return UP_EAST
        if (axis == Axis.Y && direction == Direction.EAST && pitch > 0) {
            return DrillDirection.DOWN_EAST;
        }
        /// Return UP_SOUTH
        if (axis == Axis.Y && direction == Direction.SOUTH && pitch > 0) {
            return DrillDirection.DOWN_SOUTH;
        }
        /// Return UP_WEST
        if (axis == Axis.Y && direction == Direction.WEST && pitch > 0) {
            return DrillDirection.DOWN_WEST;
        }
        throw new IllegalArgumentException("Invalid directions provided. direction: "+direction+", axis: "+axis+", pitch: "+pitch);
    }
}
