package com.radiofind.track.entity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalTrackStatusException extends RuntimeException {

  public IllegalTrackStatusException(String action, TrackStatus status) {
    super("Track in status " + status + " cannot be " + action);
  }
}
