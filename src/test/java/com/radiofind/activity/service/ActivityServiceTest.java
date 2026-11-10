package com.radiofind.activity.service;

import com.radiofind.activity.dto.ActivityResponse;
import com.radiofind.activity.entity.Activity;
import com.radiofind.activity.entity.ActivityType;
import com.radiofind.activity.repository.ActivityRepository;
import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.event.TrackResubmittedEvent;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.service.TrackMapper;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

  @Mock
  private ActivityRepository activityRepository;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @InjectMocks
  private ActivityService activityService;

  @Test
  void uploadedTrackAppearsInTheOwnersActivity() {
    User owner = User.builder().id(7L).build();
    Track track = Track.builder().id(42L).owner(owner).build();
    ArgumentCaptor<Activity> activity = ArgumentCaptor.forClass(Activity.class);

    activityService.recordTrackUploaded(new TrackUploadedEvent(track));

    verify(activityRepository).save(activity.capture());
    assertSame(owner, activity.getValue().getUser());
    assertSame(track, activity.getValue().getTrack());
    assertEquals(ActivityType.TRACK_UPLOADED, activity.getValue().getType());
  }

  @Test
  void resubmittedTrackAppearsInTheOwnersActivity() {
    Track track = Track.builder().id(42L).owner(User.builder().id(7L).build()).build();
    ArgumentCaptor<Activity> activity = ArgumentCaptor.forClass(Activity.class);

    activityService.recordTrackResubmitted(new TrackResubmittedEvent(track));

    verify(activityRepository).save(activity.capture());
    assertEquals(ActivityType.TRACK_RESUBMITTED, activity.getValue().getType());
  }

  @Test
  void activityOfADeletedTrackIsRemoved() {
    activityService.forgetDeletedTrack(new TrackDeletedEvent(42L));

    verify(activityRepository).deleteByTrackId(42L);
  }

  @Test
  void activityShowsCoverTitleAndArtistOfTheTrack() {
    Track track = Track.builder()
        .id(42L)
        .title("Midnight Dreams")
        .artistName("The Engines")
        .coverFileName("c.png")
        .build();
    Activity activity = Activity.builder().id(3L).type(ActivityType.TRACK_UPLOADED).track(track).build();
    when(activityRepository.findByUserIdOrderByCreatedAtDescIdDesc(7L, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(activity), PageRequest.of(0, 10), 1));

    PageResponse<ActivityResponse> page = activityService.getActivities(
        new CustomUserDetails(User.builder().id(7L).build()), 0, 10);

    ActivityResponse item = page.items().getFirst();
    assertEquals("Midnight Dreams", item.getTrack().getTitle());
    assertEquals("The Engines", item.getTrack().getArtistName());
    assertEquals("/api/tracks/42/cover", item.getTrack().getCoverUrl());
  }
}
