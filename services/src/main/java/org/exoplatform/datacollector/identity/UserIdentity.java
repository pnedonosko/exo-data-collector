package org.exoplatform.datacollector.identity;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

/**
 * The Class UserIdentity extends Social's {@link Identity} with required data
 * from social profile.
 */
public class UserIdentity extends Identity {

  private final String gender;

  private final String position;

  public UserIdentity(String id, String remoteId, String gender, String position) {
    super(id);
    this.setRemoteId(remoteId);
    this.setProviderId(OrganizationIdentityProvider.NAME);
    this.gender = gender;
    this.position = position;
  }

  public String getGender() {
    return gender;
  }

  public String getPosition() {
    return position;
  }
}
