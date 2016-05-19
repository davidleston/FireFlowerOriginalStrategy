package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.Color;
import com.davidleston.fireflower.HintEvent;
import com.davidleston.fireflower.Tile;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.stream.Stream;

final class MysteryTile {
  private final Multiset<Tile> possibleTiles = HashMultiset.create(Color.values().length * 5);
  private boolean markedForPlay = false;

  MysteryTile() {
    for (Color color : Color.values()) {
      possibleTiles.add(new Tile(color, 1), 3);
      possibleTiles.add(new Tile(color, 2), 2);
      possibleTiles.add(new Tile(color, 3), 2);
      possibleTiles.add(new Tile(color, 4), 2);
      possibleTiles.add(new Tile(color, 5));
    }
  }

  void mark(HintEvent hint) {
    possibleTiles.removeIf(hint.negate());
    markedForPlay = true;
  }

  void markNot(HintEvent hint) {
    possibleTiles.removeIf(hint);
  }

  public boolean isMarkedForPlay() {
    return markedForPlay;
  }

  public Stream<Tile> possibleTiles() {
    return possibleTiles.stream();
  }

  public void markNot(Tile tileDiscarded) {
    possibleTiles.remove(tileDiscarded);
  }
}
