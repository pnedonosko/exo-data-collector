package org.exoplatform.datacollector.domain;

public class ActivityCommentedEntityMock extends ActivityCommentedEntity {
  
  public ActivityCommentedEntityMock(Long id, Long posted, Long commentPosted) {
    this.postId = id;
    this.posted = posted;
    this.commentPosted = commentPosted;
  }

}
