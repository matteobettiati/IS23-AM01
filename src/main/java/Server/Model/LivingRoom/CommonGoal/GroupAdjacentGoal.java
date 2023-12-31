package Server.Model.LivingRoom.CommonGoal;
import Enumeration.Color;
import Exception.CommonGoal.NullPlayerException;
import Server.Model.Player.Player;
import Server.Model.Player.Shelf;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * The GroupAdjacentGoal class represents a goal where players must create groups of adjacent tiles with the same color.
 * It extends the CommonGoal class and contains a number of required groups, and the number of tile adjacent for group.
 */
public class GroupAdjacentGoal extends CommonGoal {

    /**
     * The number of required groups.
     */
    private final int numGroup;

    /**
     * The number of the same tile each group must have.
     */
    private final int numAdjacent;

    /**
     Create a new GroupAdjacentGoal instance with the provided token list and JSON object.
     @param tokenList The list of scoring tokens earnable by players, based on how many players are in the game.
     @param jsonObject The JSON object containing the properties for this goal.
     It must have "enum", "description", "numGroup", and "numAdjacent" properties.
     @throws NullPointerException if the jsonObject parameter is null.
     */
    public GroupAdjacentGoal(List<Integer> tokenList, @NotNull JsonObject jsonObject) {
        super();
        this.description = jsonObject.get("description").getAsString();
        this.enumeration = jsonObject.get("enum").getAsInt();
        this.numGroup = jsonObject.get("numGroup").getAsInt();
        this.numAdjacent = jsonObject.get("numAdjacent").getAsInt();
        scoringToken.addAll(tokenList);
    }

    /**
     Checks if a player has achieved the GroupAdjacentGoal and updates his score accordingly.
     If the player has achieved the goal, their ID is saved in the "accomplished" attribute.
     @param player The player to check for GroupAdjacentGoal achievement.
     @throws NullPlayerException if the player parameter is null.
     */
    @Override
    public void check(Player player) throws NullPlayerException {
        if (player == null || this.accomplished.contains(player.getPlayerID())) {
            throw new NullPlayerException();
        }

        Shelf shelf = player.getMyShelf();
        boolean[][] visited = new boolean[shelf.numberRows()][shelf.numberColumns()];
        int groups = 0;

        for (int i = shelf.numberRows() - 1; i >= 0; i--) {
            for (int j = 0; j < shelf.numberColumns(); j++) {
                if (!visited[i][j]) {
                    if (shelf.getTile(i, j) == null) {
                        visited[i][j] = true;
                        continue;
                    }
                    Color color = shelf.getTile(i, j).color();
                    int count = countSameAdjacent(shelf, visited, i, j, color);
                    if (count >= numAdjacent) {
                        groups ++;
                    }
                    if (groups >= numGroup) {
                        accomplished(player);
                        return;
                    }
                }
            }
        }
    }


    /**
     Counts the number of tiles with the same color that are adjacent to the tile at the given row and column position on the shelf.
     @param shelf the player's shelf
     @param visited a 2D boolean array that keeps track of which tiles have already been visited
     @param row the row index of the tile being visited
     @param column the column index of the tile being visited
     @param color the color of the tile being compared against
     @return the count of adjacent tiles with the same color as the tile at the given position
     */
    public static int countSameAdjacent(Shelf shelf, boolean[][] visited, int row, int column, Color color) {
        if (row < 0 || row >= shelf.numberRows() || column >= shelf.numberColumns() || column < 0 || visited[row][column] ||
                shelf.getTile(row, column) == null || shelf.getTile(row, column).color() != color ) {
            return 0;
        }
        visited[row][column] = true;
        int count = 1;
        count += countSameAdjacent(shelf, visited, row - 1, column, color);
        count += countSameAdjacent(shelf, visited, row + 1, column, color);
        count += countSameAdjacent(shelf, visited, row, column - 1, color);
        count += countSameAdjacent(shelf, visited, row, column + 1, color);
        return count;
    }
}