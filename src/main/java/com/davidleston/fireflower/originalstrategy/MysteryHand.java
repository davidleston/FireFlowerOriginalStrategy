package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.HintEvent;
import com.davidleston.fireflower.Tile;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

final class MysteryHand {

  private final List<MysteryTile> tiles = new ArrayList<>();

  public void handleHintEvent(HintEvent hintEvent) {
    IntStream.range(0, tiles.size())
        .filter(hintEvent.hintedPositions::contains)
        .mapToObj(tiles::get)
        .forEach(tile -> tile.mark(hintEvent));

    IntStream.range(0, tiles.size())
        .filter(position -> !hintEvent.hintedPositions.contains(position))
        .mapToObj(tiles::get)
        .forEach(tile -> tile.markNot(hintEvent));
  }

  public MysteryTile playPosition() {
    return tiles.get(0);
  }

  public MysteryTile chop() {
    return tiles.get(4);
  }

  public void remove(int positionDiscarded) {
    tiles.remove(positionDiscarded);
  }

  public void not(Tile tileDiscarded) {
    tiles.forEach(tile -> tile.markNot(tileDiscarded));
  }

  public void drawTile() {
    tiles.add(0, new MysteryTile());
  }

  public void reorder(ImmutableSet<Integer> newPositions) {
    List<MysteryTile> oldHand = new ArrayList<>(tiles);
    tiles.clear();
    for (int newPosition : newPositions) {
      tiles.add(oldHand.get(newPosition));
    }
  }
}
