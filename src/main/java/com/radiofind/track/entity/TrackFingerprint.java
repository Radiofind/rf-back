package com.radiofind.track.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "track_fingerprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackFingerprint {

  @Id
  private Long trackId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id")
  private Track track;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "points", nullable = false, columnDefinition = "integer[]")
  private int[] points;
}
