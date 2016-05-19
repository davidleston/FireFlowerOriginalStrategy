package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.Action;
import com.davidleston.fireflower.Color;

import java.util.Comparator;
import java.util.Optional;

public abstract class PotentialHint {
  public static final Comparator<PotentialHint> comparator
      = Comparator.comparingInt(potentialHint -> potentialHint.quantityOfTilesHinted);
  private final int quantityOfTilesHinted;

  private PotentialHint(int quantityOfTilesHinted) {
    this.quantityOfTilesHinted = quantityOfTilesHinted;
  }

  public final Optional<Action> action(int playerNumber) {
    if (quantityOfTilesHinted == 0) {
      return Optional.empty();
    }
    return Optional.of(actionInternal(playerNumber));
  }

  protected abstract Action actionInternal(int playerNumber);


  public static PotentialHint number(int quantityOfTilesHinted, int number) {
    return new PotentialHint(quantityOfTilesHinted) {
      @Override
      protected Action actionInternal(int playerNumber) {
        return Action.hint(playerNumber, number);
      }
    };
  }

  public static PotentialHint color(int quantityOfTilesHinted, Color color) {
    return new PotentialHint(quantityOfTilesHinted) {
      @Override
      protected Action actionInternal(int playerNumber) {
        return Action.hint(playerNumber, color);
      }
    };
  }

}
