package org.exoplatform.datacollector;

/**
 * The Class ActivityParticipant.
 */
public class ActivityParticipant {
  final String  id;

  final Integer isConversed;

  final Integer isFavored;
  
  final String action;

  ActivityParticipant(String id, Boolean isConversed, Boolean isFavored) {
    super();
    if (id == null) {
      throw new NullPointerException("id should be not null");
    }
    this.id = id;
    if (isConversed == null) {
      throw new NullPointerException("isConversed should be not null");
    }
    this.isConversed = isConversed ? 1 : 0;
    if (isFavored == null) {
      throw new NullPointerException("isFavored should be not null");
    }
    this.isFavored = isFavored ? 1 : 0;
    // Sep 2, 2019: categorical feature 'action':
    if (this.isConversed == 1) {
      this.action = "commented";
    } else if (this.isFavored == 1) {
      this.action = "liked";
    } else {
      this.action = "viewed";
    }
  }

  @Override
  public int hashCode() {
    int hc = 7 + id.hashCode() * 31;
    hc = hc * 31 + isConversed.hashCode();
    hc = hc * 31 + isFavored.hashCode();
    return hc;
  }

  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (this.getClass().isAssignableFrom(o.getClass())) {
        ActivityParticipant other = this.getClass().cast(o);
        return id.equals(other.id) && isConversed.equals(other.isConversed) && isFavored.equals(other.isFavored);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [id=" + id + ", isConversed=" + isConversed.intValue() + ", isFavored="
        + isFavored.intValue() + "]";
  }
}