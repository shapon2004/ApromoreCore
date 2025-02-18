/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.apromore.dao.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.springframework.beans.factory.annotation.Configurable;

import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_day", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"day_of_week", "calendar_id", "start_time"})})
@Configurable("work_day")
@NoArgsConstructor
public class WorkDay {

  private Long id;
  private DayOfWeek dayOfWeek;

  private String startTime;


  private String endTime;

  private boolean isWorkingDay = true;

  private String createdBy;
  private String updatedBy;

  private CustomCalendar customCalendar;

  
  private Duration duration;

  public WorkDay(DayOfWeek dayOfWeek, OffsetTime startTime, OffsetTime endTime,
      boolean isWorkingDay) {
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime.toString();
    this.endTime = endTime.toString();
    this.isWorkingDay = isWorkingDay;
    this.duration = Duration.between(getOffsetStartTime(), getOffsetEndTime());
  }
  
  public WorkDay(Long id,DayOfWeek dayOfWeek, OffsetTime startTime, OffsetTime endTime,
	      boolean isWorkingDay) {
	  	this.id=id;
	    this.dayOfWeek = dayOfWeek;
	    this.startTime = startTime.toString();
	    this.endTime = endTime.toString();
	    this.isWorkingDay = isWorkingDay;
	  }

  @PostLoad
  @PostPersist
  @PostUpdate
  public void calculateDuration() {
    duration = Duration.between(getOffsetStartTime(), getOffsetEndTime());

  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false)
  public Long getId() {
    return id;
  }

  @Column(name = "day_of_week")
  @Enumerated(EnumType.STRING)
  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  @Column(name = "start_time")
  public String getStartTime() {
    return startTime;
  }

  @Column(name = "end_time")
  public String getEndTime() {

    return endTime;
  }

  @Column(name = "is_working_day")
  public boolean isWorkingDay() {
    return isWorkingDay;
  }

  @ManyToOne
  @JoinColumn(name = "calendar_id")
  public CustomCalendar getCustomCalendar() {
    return customCalendar;
  }

  @Column(name = "created_by")
  public String getCreatedBy() {
    return createdBy;
  }

  @Transient
  public OffsetTime getOffsetStartTime() {
    return OffsetTime.parse(startTime);
  }

  @Column(name = "updated_by")
  public String getUpdatedBy() {
    return updatedBy;
  }

  @Transient
  public OffsetTime getOffsetEndTime() {
    return OffsetTime.parse(endTime);
  }

  public void setCustomCalendar(CustomCalendar customCalendar) {
    this.customCalendar = customCalendar;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setDayOfWeek(DayOfWeek dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }


  public void setWorkingDay(boolean isWorkingDay) {
    this.isWorkingDay = isWorkingDay;
  }

  @Transient
  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }
  
  

  
}
