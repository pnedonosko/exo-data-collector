package org.exoplatform.datacollector.identity;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;

/**
 * The Class SpaceIdentity extends Social's {@link Identity} with required
 * data from social space.
 */
public class SpaceIdentity extends Identity {
  
  public SpaceIdentity(String id, String remoteId) {
    super(id);
    this.setRemoteId(remoteId);
    this.setProviderId(SpaceIdentityProvider.NAME);
  }
  
}
