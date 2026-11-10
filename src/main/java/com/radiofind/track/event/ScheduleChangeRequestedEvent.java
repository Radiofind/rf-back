package com.radiofind.track.event;

import com.radiofind.track.entity.Track;

public record ScheduleChangeRequestedEvent(Track track) {
}
