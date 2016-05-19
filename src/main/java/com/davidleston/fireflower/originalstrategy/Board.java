package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.*;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Board {
  private static final int maxHintsAvailable = 8;
  private final Multiset<Color> playedTiles = EnumMultiset.create(Color.class);
  private final Multiset<Tile> discardedTiles = HashMultiset.create();
  private int hintsAvailable = maxHintsAvailable;
  private int bombsRemaining = 2;

  final Event.Operation eventVisitor = Event.Operation.create(
      discardEvent -> {
        hintsAvailable++;
        discardedTiles.add(discardEvent.tile);
      },
      drawEvent -> {},
      hintEvent -> hintsAvailable--,
      playEvent -> {
        if (playEvent.wasSuccessful && playEvent.tile.number == 5 && hintsAvailable < maxHintsAvailable) {
          hintsAvailable++;
        }
        if (!playEvent.wasSuccessful) {
          bombsRemaining--;
        }
      },
      reorderEvent -> {}
  );

  boolean discardAllowed() {
    return hintsAvailable < maxHintsAvailable;
  }

  public boolean hintAllowed() {
    return hintsAvailable > 0;
  }

//  public boolean isPlayable(MysteryTile tile) {
//    return tile.possibleTiles().allMatch(this::isPlayable);
//  }

  public boolean isPlayable(Tile tile) {
    return playedTiles.count(tile.color) + 1 == tile.number;
  }

  /**
   * leans on bombs
   */
  public boolean allPlayable(int number) {
    IntStream lastPlayeds = Stream.of(Color.values()).mapToInt(playedTiles::count);
    int lastPlayedToMatch = number - 1;

    if (bombAvailable()) {
      return lastPlayeds.allMatch(lastPlayed -> lastPlayed >= lastPlayedToMatch);
    }
    return lastPlayeds.allMatch(lastPlayed -> lastPlayed == lastPlayedToMatch);
  }

  public boolean bombAvailable() {
    return bombsRemaining > 0;
  }

  public int quantityDiscarded(Tile tile) {
    return discardedTiles.count(tile);
  }

  public int highestFullyPlayedNumber() {
    return Stream.of(Color.values()).mapToInt(playedTiles::count).min().getAsInt();
  }

  public Stream<Color> colorsFullyPlayed() {
    return playedTiles.entrySet().stream()
        .filter(entry -> entry.getCount() == 5)
        .map(Multiset.Entry::getElement);
  }
}
