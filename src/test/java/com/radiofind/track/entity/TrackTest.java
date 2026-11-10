package com.radiofind.track.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackTest {

  @Test
  void trackInReviewCanBeWithdrawnToDraft() {
    Track track = Track.builder().status(TrackStatus.PENDING_REVIEW).build();

    track.cancelSubmission();

    assertEquals(TrackStatus.DRAFT, track.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = TrackStatus.class, names = { "DRAFT", "APPROVED", "REJECTED" })
  void onlyTrackInReviewCanBeWithdrawn(TrackStatus status) {
    Track track = Track.builder().status(status).build();

    assertThrows(IllegalTrackStatusException.class, track::cancelSubmission);
  }

  @ParameterizedTest
  @EnumSource(value = TrackStatus.class, names = { "DRAFT", "REJECTED" })
  void draftOrRejectedTrackGoesBackToReview(TrackStatus status) {
    Track track = Track.builder().status(status).build();

    track.submitForReview();

    assertEquals(TrackStatus.PENDING_REVIEW, track.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = TrackStatus.class, names = { "PENDING_REVIEW", "APPROVED" })
  void reviewedOrWaitingTrackCannotBeSubmittedAgain(TrackStatus status) {
    Track track = Track.builder().status(status).build();

    assertThrows(IllegalTrackStatusException.class, track::submitForReview);
  }

  @Test
  void publishedTrackCannotBeEdited() {
    Track track = Track.builder().status(TrackStatus.APPROVED).build();

    assertThrows(IllegalTrackStatusException.class, track::requireEditable);
  }

  @ParameterizedTest
  @EnumSource(value = TrackStatus.class, names = { "DRAFT", "PENDING_REVIEW", "REJECTED" })
  void unpublishedTrackCanBeEdited(TrackStatus status) {
    Track track = Track.builder().status(status).build();

    assertDoesNotThrow(track::requireEditable);
  }

  @ParameterizedTest
  @CsvSource({ "DRAFT, false", "PENDING_REVIEW, false", "APPROVED, true", "REJECTED, true" })
  void onlyReviewedTracksAcceptLikes(TrackStatus status, boolean acceptsLikes) {
    Track track = Track.builder().status(status).build();

    assertEquals(acceptsLikes, track.acceptsLikes());
  }
}
