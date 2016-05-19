package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.*;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;

final class OtherPlayer {
  final int playerNumber;
  final Event.Operation eventVisitor;
  private static final ImmutableList<Integer> distribution = ImmutableList.of(3, 2, 2, 2, 5);
  private final MysteryHand handFromTheirPerspective = new MysteryHand();
  private final List<Tile> hand = new ArrayList<>();
  private final Board board;

  OtherPlayer(Board board, int playerNumber) {
    this.board = board;
    this.playerNumber = playerNumber;
    this.eventVisitor = Event.Operation.create(
        this::tileRevealed,
        drawEvent -> {
          if (drawEvent.sourcePlayer == playerNumber) {
            hand.add(0, drawEvent.tile);
            handFromTheirPerspective.drawTile();
          } else {
            handFromTheirPerspective.not(drawEvent.tile);
          }
        },
        hintEvent -> {
          if (hintEvent.playerReceivingHint == playerNumber) {
            handFromTheirPerspective.handleHintEvent(hintEvent);
          }
        },
        this::tileRevealed,
        reorderEvent -> {
          if (reorderEvent.sourcePlayer == playerNumber) {
            List<Tile> oldHand = new ArrayList<>(hand);
            hand.clear();
            for (int newPosition : reorderEvent.newPositions) {
              hand.add(oldHand.get(newPosition));
            }
            handFromTheirPerspective.reorder(reorderEvent.newPositions);
          }
        }
    );
  }

  private void tileRevealed(TileRevealedEvent event) {
    if (event.sourcePlayer == playerNumber) {
      hand.remove(event.position);
      handFromTheirPerspective.remove(event.position);
      handFromTheirPerspective.not(event.tile);
    }
  }

  public boolean hasPlay() {
    return handFromTheirPerspective.playPosition().isMarkedForPlay();
  }

  public Optional<Tile> playableTileInPlayPositionNotAlreadyMarkedForPlay() {
    if (board.isPlayable(hand.get(0))) {
      return Optional.of(hand.get(0));
    }
    return Optional.empty();
  }

  // todo: implement reverse finesse?
  public Optional<PotentialHint> hintPlayableChainWithFirstTileAs(Tile tile) {
    if (hand.stream().filter(isEqual(tile)).count() != 1) {
      return Optional.empty();
    }

    // prefer color clues because color more clearly indicates play
    return Stream.of(
        hand.stream()
            .filter(t -> t.color == tile.color)
            .findFirst()
            .filter(isEqual(tile))
            .map(t -> PotentialHint.color(quantityOf(tile.color), tile.color)),
        hand.stream()
            .filter(t -> t.number == tile.number)
            .findFirst()
            .filter(isEqual(tile))
            .map(t -> PotentialHint.number(quantityOf(tile.number), tile.number)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(PotentialHint.comparator);
  }

  public boolean hasUnsafeDiscardOnChop() {
    // todo: implement next two players have same tile on chop
    int quantityDiscarded = board.quantityDiscarded(chop());
    int maxSafeDiscards = distribution.get(chop().number - 1) - 1;
    return quantityDiscarded == maxSafeDiscards;
  }

  /**
   * Uses convention that color means play
   */
  public Optional<Action> hintChopToIndicatePlay() {
    Tile chop = chop();
    if (board.hintAllowed() && board.isPlayable(chop)) {
      // todo: optimize that one can hint color when hinting multiple tiles if other tiles are understood to not be playable
      if (quantityOf(chop.color) == 1) {
        // todo: figure out if we get events from our own actions
        return Optional.of(Action.hint(playerNumber, chop.color));
      }
      if (quantityOf(chop.number) == 1 && board.allPlayable(chop.number)) {
        return Optional.of(Action.hint(playerNumber, chop.number));
      }
    }
    return Optional.empty();
  }

  private int quantityOf(Color color) {
    return quantityOf(tile -> tile.color == color);
  }

  private int quantityOf(int number) {
    return quantityOf(tile -> tile.number == number);
  }

  private int quantityOf(Predicate<Tile> predicate) {
    return (int) hand.stream().filter(predicate).count();
  }

  private Tile chop() {
    return hand.get(4);
  }

  public Optional<Action> hintSafeDiscard() {
    if (!board.hintAllowed()) {
      return Optional.empty();
    }
    int highestFullyPlayedNumber = board.highestFullyPlayedNumber();

    return concat(
        rangeClosed(1, highestFullyPlayedNumber)
            .mapToObj(number -> PotentialHint.number(quantityOf(number), number)),
        board.colorsFullyPlayed()
            .map(color -> PotentialHint.color(quantityOf(color), color)))
        .max(PotentialHint.comparator)
        .flatMap(hint -> hint.action(playerNumber));
  }

  public Optional<Action> clueToSaveChop() {
    if (!board.hintAllowed()) {
      return Optional.empty();
    }
    return null;
  }

  public Optional<Action> hintAnyPlayableIfNoTileMarkedForPlay() {
    if (hasPlay()) {
      return Optional.empty();
    }
    return hand.stream()
        .filter(board::isPlayable)
        .map(this::hintPlayableChainWithFirstTileAs)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(PotentialHint.comparator)
        .flatMap(potentialHint -> potentialHint.action(playerNumber));
  }
}
