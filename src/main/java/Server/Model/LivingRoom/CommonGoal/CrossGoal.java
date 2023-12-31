package Server.Model.LivingRoom.CommonGoal;

import Exception.CommonGoal.NullPlayerException;
import Server.Model.Player.Player;
import Server.Model.Player.Shelf;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * The CrossGoal class represents a goal where players must create groups of tiles in a cross shape on their shelf.
 * It extends the CommonGoal class and contains a number of required groups.
 */
public class CrossGoal extends CommonGoal {

    /**
     * The number of required Cross.
     */
    private final int numGroup;

    /**
     Create a new CrossGoal with the provided token list and JSON object.
     @param tokenList The list of scoring tokens earnable by players, based on how many players are in the game.
     @param jsonObject The JSON object containing the properties for this goal.
     It must have "enum", "description", and "numGroup" properties.
     @throws NullPointerException if the jsonObject parameter is null.
     */
    public CrossGoal(List<Integer> tokenList, @NotNull JsonObject jsonObject) {
        super();
        this.enumeration = jsonObject.get("enum").getAsInt();
        this.description = jsonObject.get("description").getAsString();
        this.numGroup = jsonObject.get("numGroup").getAsInt();
        scoringToken.addAll(tokenList);
    }


    /**
     This method checks if a player has achieved the CrossGoal and updates his score accordingly.
     If the player has achieved the objective, their ID is saved in the "accomplished" attribute.
     @param player The player to check for CrossGoal achievement.
     @throws NullPlayerException if the player parameter is null.
     */
    @Override
    public void check(Player player) throws NullPlayerException {
        if (player == null || this.accomplished.contains(player.getPlayerID())) {
            throw new NullPlayerException();
        }
        Shelf shelf = player.getMyShelf();
        int countGroup = 0;
        for (int i = 1; i < shelf.numberRows() - 1; i++) {
            for (int j = 1; j < shelf.numberColumns() - 1 ; j++) {
                try {
                    if ((shelf.getTile(i, j).color() == shelf.getTile(i - 1, j - 1).color()) &&
                            (shelf.getTile(i, j).color() == shelf.getTile(i - 1, j + 1).color()) &&
                            (shelf.getTile(i, j).color() == shelf.getTile(i + 1, j - 1).color()) &&
                            (shelf.getTile(i, j).color() == shelf.getTile(i + 1, j + 1).color())) {
                        countGroup++;
                    }
                } catch (NullPointerException ignored) {

                }
            }
        }
        if (countGroup >= numGroup) {
            accomplished(player);
        }
    }
}
