package com.davidleston.fireflower.originalstrategy;

import com.davidleston.fireflower.*;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ForThreePlayers implements Player {
  // todo: implement reorder
  private final int playerNumber = 0;
  private final OtherPlayer nextPlayer;
  private final OtherPlayer nextNextPlayer;
  private final Board board;
  private final MysteryHand hand;
  private final Consumer<Event> eventVisitors;
  private final ReorderAction doNotReorder;

  public ForThreePlayers(int playerNumber) {
    board = new Board();
    nextPlayer = new OtherPlayer(board, (playerNumber + 1) % 3);
    nextNextPlayer = new OtherPlayer(board, (playerNumber + 2 % 3));
    hand = new MysteryHand();
    eventVisitors = board.eventVisitor
        .andThen(nextPlayer.eventVisitor)
        .andThen(nextNextPlayer.eventVisitor);
    doNotReorder = event -> {
      eventVisitors.accept(event);
      return null;
    };
  }

  @SafeVarargs
  private final Optional<Action> firstActionOf(Supplier<Optional<Action>>... actionSuppliers) {
    return Stream.of(actionSuppliers)
        .map(Supplier::get)
        .filter(Optional::isPresent)
        .findFirst()
        .orElseGet(Optional::empty);
  }

  @Override
  public Action takeTurn(Stream<Event> events) {
    events.forEachOrdered(eventVisitors);
    return firstActionOf(
        this::finesseOrBluff,
        this::saveTheChop,
        this::playTileMarkedForPlay,
        nextNextPlayer::hintAnyPlayableIfNoTileMarkedForPlay)
        .orElseGet(this::discard);
  }

  private Optional<Action> playTileMarkedForPlay() {
    if (iHavePlay()) {
      return Optional.of(play());
    }
    return Optional.empty();
  }

  // aggressively save the chop
  private Optional<Action> saveTheChop() {
    if (nextPlayer.hasUnsafeDiscardOnChop()) {
      return firstActionOf(
          this::playIfNoDiscards,
          nextPlayer::hintChopToIndicatePlay,
          nextPlayer::hintSafeDiscard,
          nextPlayer::clueToSaveChop,
          this::discardDespitePlay);
    }
    return Optional.empty();
  }

  private Optional<Action> playIfNoDiscards() {
    if (!board.discardAllowed() && iHavePlay()) {
      return Optional.of(play());
    }
    return Optional.empty();
  }

  private Optional<Action> discardDespitePlay() {
    if (iHavePlay()) {
      return Optional.of(discard());
    }
    return Optional.empty();
  }

  private Action discard() {
    return Action.discard(4, doNotReorder);
  }

  private Action play() {
    return Action.play(0, doNotReorder);
  }

  private boolean iHavePlay() {
    return hand.playPosition().isMarkedForPlay();
  }

  private Optional<Action> finesseOrBluff() {
    // todo: implement finessing and bluffing past a play
    if (nextPlayer.hasPlay()) {
      return Optional.empty();
    }
    return nextPlayer.playableTileInPlayPositionNotAlreadyMarkedForPlay()
        .filter(nextTile -> nextTile.number < 5)
        .flatMap(nextTile -> firstActionOf(
            () -> finesse(nextTile),
            () -> bluff(nextTile)
        ));
  }

  private Optional<Action> finesse(Tile nextTile) {
    return nextNextPlayer
        .hintPlayableChainWithFirstTileAs(nextTile)
        .flatMap(potentialHint -> potentialHint.action(nextPlayer.playerNumber));
  }

  private Optional<Action> bluff(Tile nextTile) {
    // todo: implement bluffing
    return Optional.empty();
  }

  /**
   * @param events the last one is our hint
   * @return null means no reorder
   */
  @Override
  public ImmutableSet<Integer> receiveHint(Stream<Event> events) {
    AtomicReference<ImmutableSet<Integer>> reorder = new AtomicReference<>();
    events
        .forEachOrdered(eventVisitors.andThen(event -> Event.Operation.create(
            discardEvent -> {
            },
            drawEvent -> {
            },
            hintEvent -> {
              if (hintEvent.playerReceivingHint == playerNumber) {
                hand.handleHintEvent(hintEvent);
                handle(hintEvent.hintedPositions);
              }
            },
            playEvent -> {
            },
            reorderEvent -> {
            }
        )));
    return reorder.get();
  }

  private void handle(ImmutableSet<Integer> hintedPositions) {
    if (hintedPositions.contains(5)) {
      // if chop is playable, play all for playable
      // else play all for sure playable, save rest
    } else {
      // mark all for play in order
    }
  }

}
