/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.datacollector;

import static org.exoplatform.datacollector.UserInfluencers.ACTIVITY_PARTICIPANTS_TOP;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.picocontainer.Startable;

import com.google.common.collect.Lists;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.datacollector.dao.ActivityCommentedDAO;
import org.exoplatform.datacollector.dao.ActivityLikedDAO;
import org.exoplatform.datacollector.dao.ActivityMentionedDAO;
import org.exoplatform.datacollector.dao.ActivityPostedDAO;
import org.exoplatform.datacollector.identity.SpaceIdentity;
import org.exoplatform.datacollector.identity.UserIdentity;
import org.exoplatform.platform.gadget.services.LoginHistory.LastLoginBean;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryBean;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.prediction.TrainingService;
import org.exoplatform.prediction.user.domain.ModelEntity;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * The Class SocialDataCollectorService.
 */
public class SocialDataCollectorService implements Startable {

  /** Logger */
  private static final Log       LOG                   = ExoLogger.getExoLogger(SocialDataCollectorService.class);

  public static final String     DUMMY_ID              = "0".intern();

  public static final String     EMPTY_STRING          = "".intern();

  /**
   * Base minimum number of threads for worker thread executors.
   */
  public static final int        MIN_THREADS           = 2;

  /**
   * Minimal number of threads maximum possible for worker thread executors.
   */
  public static final int        MIN_MAX_THREADS       = 4;

  /** Thread idle time for thread executors (in seconds). */
  public static final int        THREAD_IDLE_TIME      = 120;

  /**
   * Maximum threads per CPU for worker thread executors.
   */
  public static final int        WORKER_MAX_FACTOR     = 2;

  /**
   * Queue size per CPU for worker thread executors.
   */
  public static final int        WORKER_QUEUE_FACTOR   = WORKER_MAX_FACTOR * 20;

  /**
   * Thread name used for worker thread.
   */
  public static final String     WORKER_THREAD_PREFIX  = "datacollector-worker-thread-";

  protected static final Pattern ENGINEERING_PATTERN   =
                                                     Pattern.compile("^.*developer|architect|r&d|mobile|qa|fqa|tqa|test|quality|qualité|expert|integrator|designer|cwi|technical advisor|services delivery|software engineer.*$");

  protected static final Pattern SALES_PATTERN         =
                                               Pattern.compile("^.*consultant|sales|client|support|sales engineer|demand generator.*$");

  protected static final Pattern MARKETING_PATTERN     =
                                                   Pattern.compile("^.*brand|communication|marketing|customer success|user experience|.*$");

  protected static final Pattern MANAGEMENT_PATTERN    =
                                                    Pattern.compile("^.*officer|chief|founder|coo|cto|cio|evp|advisor|product manager|director|general manager.*$");

  protected static final Pattern FINANCIAL_PATTERN     = Pattern.compile("^.*accountant|financial|investment|account manager.*$");

  /**
   * The Constant EMPLOYEES_GROUPID - TODO better make it configurable. For
   * /Groups/spaces/exo_employees.
   */
  protected static final String  EMPLOYEES_GROUPID     = "/spaces/exo_employees";

  protected static final String  FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd_HHmmss.SSSS";

  protected static final Integer RECENT_LOGINS_COUNT   = 20;                                                                                                                                                                      // 3600000L;

  // 3 hours - 3 * 3600000L (3 min for testing purposes)
  protected static final Long    TRAIN_PERIOD          = 3 * 60000L;                                                                                                                                                              // 3600000L;

  protected static final String  BUCKET_PREFIX         = "prod-";

  /**
   * The BucketWorker gets target users from login history or bucketRecords
   * and processes them in loginsQueue order. Collects a new dataset and sends it to TrainingService, if the user's model needs training.
   * Schedules next execution in TRAIN_PERIOD ms.
   */
  public class BucketWorker extends ContainerCommand {

    public BucketWorker(String containerName) {
      super(containerName);
    }

    @Override
    void execute(ExoContainer exoContainer) {
      LOG.info("Bucket processing time! Current bucket: prod-{}", currentBucketIndex);
      Map<String, Date> targetUsers = getTargetUsers();

      while (!loginsQueue.isEmpty()) {
        String userName = loginsQueue.poll();
        Date loginDate = targetUsers.get(userName);
        LOG.info("Processing user from bucket: {} loginTime: {}", userName, loginDate.getTime());

        ModelEntity existingModel = trainingService.getLastModel(userName);

        if (modelNeedsTraining(existingModel, loginDate)) {
          String datasetFile = collectUserActivities(BUCKET_PREFIX + currentBucketIndex, userName);
          trainingService.addModel(userName, datasetFile);
          LOG.info("Collected a new dataset and added model for user {}", userName);
        }
        else {
          LOG.info("User's {} model doesn't need training ", userName);
        }

        targetUsers.remove(userName);
      }
      currentBucketIndex++;
      timer.schedule(new BucketWorker(containerName), TRAIN_PERIOD);
    }

    @Override
    void onContainerError(String error) {
      LOG.error("Container error has occured: {}", error);
    }

    /**
     * Returns bucketRecords
     * @return bucketRecords
     */
    Map<String, Date> getTargetUsers() {
      return bucketRecords;
    }

    /**
     * Checks if the model needs (re)training
     * @param model to be checked
     * @param loginDate login date
     * @return true, if model needs (re)training, otherwise - false
     */
    boolean modelNeedsTraining(ModelEntity model, Date loginDate) {
      return model == null
          || model.getActivated() == null || Math.abs(model.getActivated().getTime() - loginDate.getTime()) > TRAIN_PERIOD;
    }

  }

  /**
   * The StartWorker is used to perform first processing based on login history.
   * Registers loginListener
   */
  public class StartWorker extends BucketWorker {
    public StartWorker(String containerName) {
      super(containerName);
    }

    @Override
    void execute(ExoContainer exoContainer) {
      super.execute(exoContainer);
      listenerService.addListener(new LoginListener());
    }

    /**
     * Returns last logins from login history
     */
    @Override
    Map<String, Date> getTargetUsers() {
      Map<String, Date> recentLogins = new HashMap<>();
      try {
        // Last RECENT_LOGINS_COUNT logins
        List<LastLoginBean> lastLogins = loginHistory.getLastLogins(RECENT_LOGINS_COUNT, EMPTY_STRING);
        lastLogins.forEach(entity -> recentLogins.put(entity.getUserId(), new Date(entity.getLastLogin())));
        // From older to newer logins
        Lists.reverse(lastLogins).forEach(login -> loginsQueue.add(login.getUserId()));

        LOG.info("Users from login history to be processed: ");
        loginsQueue.forEach(LOG::info);

      } catch (Exception e) {
        LOG.error("Cannot get last users login: {}", e.getMessage());
      }
      return recentLogins;
    }

  }

  /**
   * The MembershipListener is used to keep the focusGroups up-to-date
   */
  public class MembershipListener extends MembershipEventListener {

    @Override
    public void postSave(Membership m, boolean isNew) throws Exception {
      if (isNew) {
        String userId = getUserIdentityByName(m.getUserName()).getId();
        if (m.getGroupId().equals(userACL.getAdminGroups())) {
          focusGroups.get(userACL.getAdminGroups()).add(userId);
        }
        if (m.getGroupId().equals(EMPLOYEES_GROUPID)) {
          focusGroups.get(EMPLOYEES_GROUPID).add(userId);

        }
      }
    }

    public void postDelete(Membership m) throws Exception {
      String userId = getUserIdentityByName(m.getUserName()).getId();
      if (m.getGroupId().equals(userACL.getAdminGroups())) {
        focusGroups.get(userACL.getAdminGroups()).remove(userId);
      }
      if (m.getGroupId().equals(EMPLOYEES_GROUPID)) {
        focusGroups.get(EMPLOYEES_GROUPID).remove(userId);
      }
    }

  }

  /**
   * The LoginListener add new logins to the bucketRecords
   */
  public class LoginListener extends Listener<ConversationRegistry, ConversationState> {

    public LoginListener() {
      this.name = "exo.core.security.ConversationRegistry.register";
    }

    @Override
    public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
      String userId = event.getData().getIdentity().getUserId();

      if (!bucketRecords.containsKey(userId)) {
        loginsQueue.add(userId);
        bucketRecords.put(userId, new Date());
        LOG.info("User {} has logged in and added to bucketRecords", event.getData().getIdentity().getUserId());
      }

    }
  }

  protected final IdentityManager                          identityManager;

  protected final ActivityManager                          activityManager;

  protected final RelationshipManager                      relationshipManager;

  protected final SpaceService                             spaceService;

  protected final OrganizationService                      organization;

  protected final LoginHistoryService                      loginHistory;

  protected final UserACL                                  userACL;

  protected final ActivityCommentedDAO                     commentStorage;

  protected final ActivityPostedDAO                        postStorage;

  protected final ActivityLikedDAO                         likeStorage;

  protected final ActivityMentionedDAO                     mentionStorage;

  protected final ListenerService                          listenerService;

  protected final TrainingService                          trainingService;

  protected final Map<String, Set<String>>                 focusGroups        = new HashMap<>();

  // TODO clean this map time-from-time to do not consume the RAM
  protected final Map<String, SoftReference<UserIdentity>> userIdentities     = new HashMap<>();

  protected final Map<String, SpaceIdentity>               spaceIdentities    = new HashMap<>();

  protected final ExecutorService                          workers;

  /** Contains pairs K - user id, V - date of login to be processed by BucketWorker */
  protected final ConcurrentHashMap<String, Date>          bucketRecords      = new ConcurrentHashMap<>();

  /** Contains users id ordered by the login time */
  protected final ConcurrentLinkedQueue<String>            loginsQueue        = new ConcurrentLinkedQueue<>();

  protected Integer                                        currentBucketIndex = 0;

  /** The timer for scheduled tasks execution */
  protected final Timer                                    timer              = new Timer();

  /**
   * Instantiates a new data collector service.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param identityManager the identity manager
   * @param activityManager the activity manager
   * @param relationshipManager the relationship manager
   * @param spaceService the space service
   * @param loginHistory the login history
   * @param userACL the user ACL
   * @param postStorage the post storage
   * @param commentStorage the comment storage
   * @param likeStorage the like storage
   * @param mentionStorage the mention storage
   */
  public SocialDataCollectorService(RepositoryService jcrService,
                                    SessionProviderService sessionProviders,
                                    NodeHierarchyCreator hierarchyCreator,
                                    OrganizationService organization,
                                    IdentityManager identityManager,
                                    ActivityManager activityManager,
                                    RelationshipManager relationshipManager,
                                    SpaceService spaceService,
                                    LoginHistoryService loginHistory,
                                    ListenerService listenerService,
                                    UserACL userACL,
                                    ActivityPostedDAO postStorage,
                                    ActivityCommentedDAO commentStorage,
                                    ActivityLikedDAO likeStorage,
                                    ActivityMentionedDAO mentionStorage,
                                    TrainingService trainingService) {
    super();
    this.identityManager = identityManager;
    this.activityManager = activityManager;
    this.relationshipManager = relationshipManager;
    this.organization = organization;
    this.spaceService = spaceService;
    this.loginHistory = loginHistory;
    this.userACL = userACL;
    this.listenerService = listenerService;
    this.postStorage = postStorage;
    this.commentStorage = commentStorage;
    this.likeStorage = likeStorage;
    this.mentionStorage = mentionStorage;
    this.trainingService = trainingService;

    this.workers = createThreadExecutor(WORKER_THREAD_PREFIX, WORKER_MAX_FACTOR, WORKER_QUEUE_FACTOR);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // Pre-read constant things
    Set<String> admins = getGroupMemberIds(userACL.getAdminGroups(), "manager", "member");
    this.focusGroups.put(userACL.getAdminGroups(), admins);
    Set<String> employees = getGroupMemberIds(EMPLOYEES_GROUPID);
    this.focusGroups.put(EMPLOYEES_GROUPID, employees);

    try {
      organization.addListenerPlugin(new MembershipListener());
    } catch (Exception e) {
      LOG.error("Cannot add the MembershipListener: {}", e.getMessage());
    }

    final String containerName = ExoContainerContext.getCurrentContainer().getContext().getName();
    workers.submit(new StartWorker(containerName));

    // TODO Brief description of dataset/model flow:
    // 1) load currently active users from login history
    // and collect datasets for those who outdated into a new bucketRecords and
    // train a
    // model for it in TrainingService.
    // 2) Then register login listener to for a new logins and collect/train
    // models for them into a new buckter. Collect users for the bucketRecords
    // continously. Train new model only if active one is already outdated.
    // Train collected models in a single bucketRecords.
    // 3) Model become outdated in 3hrs (will be configurable)
    // 4) Collector maintains datasets and models in buckets: first
    // bucketRecords is of
    // a first start (all active users in single bucketRecords), second and
    // following
    // for listened logins. Runtime bucketRecords name is based on "prod" prefix
    // with
    // incremented index.
    // 5) After model was trained, we don't need a dataset file anymore and it
    // should be removed
    // 6) It's important to avoid creation of a garbage in buckets: all files
    // not required should be removed. Buckets that not used - also removed.
    // 7) We maintain a short history of archived models: an active one and last
    // 20 (for about 3-4 days for daily active users), older models should be
    // removed in DB and files.

    // Tips:
    // Login history: see LoginHistoryService dependency and how it was used in
    // wasUserLoggedin().
    // Login listener see in LoginHistoryListener of Platform's project gadget
    // pack, we need register an own listener via configuration.

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing
  }

  /**
   * Collect users activities into files bucketRecords in Platform data folder, each
   * file will have name of an user with <code>.csv</code> extension. If such
   * folder already exists, it will overwrite the files that match found users.
   * In case of error during the work, all partial results will be deleted.
   *
   * @param bucketName the bucketRecords name, can be <code>null</code> then a
   *          timestamped name will be created
   * @return the string with a path to saved bucketRecords folder or <code>null</code>
   *         if error occured
   * @throws Exception the exception
   */
  public String collectUsersActivities(String bucketName) throws Exception {
    // TODO go through all users in the organization and swap their datasets
    // into separate data stream, then feed them to the Training Service

    File bucketDir = openBuckerDir(bucketName);
    LOG.info("Saving dataset into bucketRecords folder: {}", bucketDir.getAbsolutePath());

    // ProfileFilter filter = new ProfileFilter();
    // long idsCount =
    // identityStorage.getIdentitiesByProfileFilterCount(OrganizationIdentityProvider.NAME,
    // filter);
    Iterator<Identity> idIter =
                              ListAccessUtil.loadListIterator(identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                                                           new ProfileFilter(),
                                                                                                           true));
    while (idIter.hasNext() && !Thread.currentThread().isInterrupted()) {
      final UserIdentity id = cacheUserIdentity(userIdentity(idIter.next()));
      final File userFile = new File(bucketDir, id.getRemoteId() + ".csv");
      submitCollectUserActivities(id, userFile);
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("Saving of dataset interrupted for bucketRecords: {}", bucketName);
      workers.shutdownNow();
      // Clean the bucketRecords files
      try {
        Arrays.asList(bucketDir.listFiles()).stream().forEach(f -> f.delete());
        bucketDir.delete();
      } catch (Exception e) {
        LOG.error("Error removing canceled bucketRecords folder: {}", bucketName);
      }
      return null;
    } else {
      LOG.info("Saved dataset successfully into bucketRecords folder: {}", bucketDir.getAbsolutePath());
      return bucketDir.getAbsolutePath();
    }
  }

  /**
   * Collect user activities into given files bucketRecords in Platform data folder.
   *
   * @param bucketName the bucketRecords name
   * @param userName the user name
   * @return the path of created dataset file
   */
  public String collectUserActivities(String bucketName, String userName) {
    File bucketDir = openBuckerDir(bucketName);
    LOG.info("Saving user dataset into bucketRecords folder: {}", bucketDir.getAbsolutePath());
    final UserIdentity id = getUserIdentityByName(userName);
    if (id != null) {
      final File userFile = new File(bucketDir, id.getRemoteId() + ".csv");
      submitCollectUserActivities(id, userFile);
      LOG.info("Saved user dataset into bucketRecords file: {}", userFile.getAbsolutePath());
      return userFile.getAbsolutePath();
    } else {
      LOG.warn("User social identity not found for {}", userName);
      return null;
    }
  }

  // **** internals

  protected void submitCollectUserActivities(UserIdentity id, File file) {
    final String containerName = ExoContainerContext.getCurrentContainer().getContext().getName();
    workers.submit(new ContainerCommand(containerName) {
      /**
       * {@inheritDoc}
       */
      @Override
      void execute(ExoContainer exoContainer) {
        try {
          PrintWriter writer = new PrintWriter(file);
          try {
            collectUserActivities(id, writer);
          } catch (Exception e) {
            LOG.error("User activities collector error for worker of {} : {}", id.getRemoteId(), e);
          } finally {
            writer.close();
          }
        } catch (IOException e) {
          LOG.error("Error opening activities collector file for {} : {}", id.getRemoteId(), e);
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      void onContainerError(String error) {
        LOG.error("Container error: ( {} ) for worker of {} : {}", containerName, id.getRemoteId(), error);
      }
    });
  }

  /**
   * Collect user activities.
   *
   * @param id the user identity in Social
   * @param out the writer where spool the user activities dataset
   * @throws Exception the exception
   */
  protected void collectUserActivities(UserIdentity id, PrintWriter out) throws Exception {
    LOG.info("> Collecting user activities for {}", id.getRemoteId());
    out.println(activityHeader());

    // Find this user favorite participants (influencers) and streams
    LOG.info(">> Buidling user influencers for {}", id.getRemoteId());
    Collection<Identity> idConnections = ListAccessUtil.loadListAll(relationshipManager.getConnections(id));
    Collection<Space> userSpaces = ListAccessUtil.loadListAll(spaceService.getMemberSpaces(id.getRemoteId()));

    UserInfluencers influencers = new UserInfluencers(id, idConnections, userSpaces);

    influencers.addCommentedPoster(commentStorage.findPartIsCommentedPoster(id.getId()));
    influencers.addCommentedCommenter(commentStorage.findPartIsCommentedCommenter(id.getId()));
    influencers.addCommentedConvoPoster(commentStorage.findPartIsCommentedConvoPoster(id.getId()));

    influencers.addPostCommenter(commentStorage.findPartIsPostCommenter(id.getId()));
    influencers.addCommentCommenter(commentStorage.findPartIsCommentCommenter(id.getId()));
    influencers.addConvoCommenter(commentStorage.findPartIsConvoCommenter(id.getId()));

    influencers.addMentioner(mentionStorage.findPartIsMentioner(id.getId()));
    influencers.addMentioned(mentionStorage.findPartIsMentioned(id.getId()));

    influencers.addLikedPoster(likeStorage.findPartIsLikedPoster(id.getId()));
    influencers.addLikedCommenter(likeStorage.findPartIsLikedCommenter(id.getId()));
    influencers.addLikedConvoPoster(likeStorage.findPartIsLikedConvoPoster(id.getId()));

    influencers.addPostLiker(likeStorage.findPartIsPostLiker(id.getId()));
    influencers.addCommentLiker(likeStorage.findPartIsCommentLiker(id.getId()));
    influencers.addConvoLiker(likeStorage.findPartIsConvoLiker(id.getId()));

    influencers.addSamePostLiker(likeStorage.findPartIsSamePostLiker(id.getId()));
    influencers.addSameCommentLiker(likeStorage.findPartIsSameCommentLiker(id.getId()));
    influencers.addSameConvoLiker(likeStorage.findPartIsSameConvoLiker(id.getId()));

    // Here the influencers object knows favorite streams of the user
    Collection<String> userStreams = influencers.getFavoriteStreamsTop(10);
    if (userStreams.size() < 10) {
      // TODO add required (to 10) streams where user has most of its
      // connections
    }
    if (userStreams.size() > 0) {
      influencers.addStreamPoster(postStorage.findPartIsFavoriteStreamPoster(id.getId(), userStreams));
      influencers.addStreamCommenter(commentStorage.findPartIsFavoriteStreamCommenter(id.getId(), userStreams));
      influencers.addStreamPostLiker(likeStorage.findPartIsFavoriteStreamPostLiker(id.getId(), userStreams));
      influencers.addStreamCommentLiker(likeStorage.findPartIsFavoriteStreamCommentLiker(id.getId(), userStreams));
    }
    LOG.info("<< Built user influencers for {}", id.getRemoteId());

    // load identity's activities and collect its data
    Iterator<ExoSocialActivity> feedIter = ListAccessUtil.loadListIterator(activityManager.getActivityFeedWithListAccess(id));
    while (feedIter.hasNext() && !Thread.currentThread().isInterrupted()) {
      ExoSocialActivity activity = feedIter.next();
      out.println(activityLine(influencers, activity));
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("< Interrupted collector of user activities for {}", id.getRemoteId());
    } else {
      LOG.info("< Collected user activities for {}", id.getRemoteId());
    }
  }

  protected String activityHeader() {
    StringBuilder aline = new StringBuilder();
    aline.append("id,")
         // .append("title,")
         .append("type_content,")
         .append("type_social,")
         .append("type_calendar,")
         .append("type_forum,")
         .append("type_wiki,")
         .append("type_poll,")
         .append("type_other,")
         .append("owner_id,")
         .append("owner_title,")
         .append("owner_type_organization,")
         .append("owner_type_space,")
         .append("owner_influence,")
         .append("number_of_likes,")
         .append("number_of_comments,")
         .append("reactivity,")
         .append("is_mentions_me,")
         .append("is_mentions_connections,")
         .append("is_commented_by_me,")
         .append("is_commented_by_connetions,")
         .append("is_liked_by_me,")
         .append("is_liked_by_connections,")
         // Poster features
         .append("poster_id,")
         .append("poster_gender_male,")
         .append("poster_gender_female,")
         .append("poster_is_employee,")
         .append("poster_is_lead,")
         .append("poster_is_in_connections,")
         .append("poster_focus_engineering,")
         .append("poster_focus_sales,")
         .append("poster_focus_marketing,")
         .append("poster_focus_management,")
         .append("poster_focus_financial,")
         .append("poster_focus_other,")
         .append("poster_influence,");
    for (int i = 1; i <= ACTIVITY_PARTICIPANTS_TOP; i++) {
      // Participant #N features
      StringBuilder prefix = new StringBuilder("participant").append(i).append('_');
      aline.append(prefix)
           .append("id,")
           .append(prefix)
           .append("conversed,")
           .append(prefix)
           .append("favored,")
           .append(prefix)
           .append("gender_male,")
           .append(prefix)
           .append("gender_female,")
           .append(prefix)
           .append("is_employee,")
           .append(prefix)
           .append("is_lead,")
           .append(prefix)
           .append("is_in_connections,")
           .append(prefix)
           .append("focus_engineering,")
           .append(prefix)
           .append("focus_sales,")
           .append(prefix)
           .append("focus_marketing,")
           .append(prefix)
           .append("focus_management,")
           .append(prefix)
           .append("focus_financial,")
           .append(prefix)
           .append("focus_other,")
           .append(prefix)
           .append("influence,");
    }
    aline.deleteCharAt(aline.length() - 1); // remove last comma
    return aline.toString();
  }

  protected String activityLine(UserInfluencers influencers, ExoSocialActivity activity) {
    // Activity identification & type
    StringBuilder aline = new StringBuilder();
    // ID
    aline.append(activity.getId()).append(',');
    // title: escape comma in the text to avoid problems with separator
    // aline.append(activity.getTitle().replace(',', '_')).append(',');
    // type: encoded
    encActivityType(aline, activity.getType()).append(',');
    // app ID: TODO need it?
    // aline.append(activity.getAppId()).append(',');
    String ownerName = activity.getStreamOwner();
    Identity owner;
    String ownerId;
    ActivityStream stream = activity.getActivityStream();
    boolean isSpace = Type.SPACE.equals(stream.getType());
    if (isSpace) {
      owner = getSpaceIdentityByName(ownerName);
    } else {
      owner = getUserIdentityByName(ownerName);
    }
    // owner_id
    if (owner != null) {
      ownerId = owner.getId();
    } else {
      ownerId = DUMMY_ID;
      LOG.warn("Cannot find social identity of stream owner: {} Activity: {}", ownerName, activity.getId());
    }
    aline.append(ownerId).append(',');
    // owner_title
    aline.append(ownerName).append(',');
    // owner_type
    if (isSpace) {
      aline.append("0,1,");
    } else {
      aline.append("1,0,");
    }
    // TODO do we need owner/stream focus?
    // aline.append(findStreamFocus(stream.getPrettyId())).append(',');
    // owner influence
    // TODO format double/float with dot-delimiter for decimal part
    aline.append(influencers.getStreamWeight(ownerId)).append(',');
    // number_of_likes
    aline.append(activity.getNumberOfLikes()).append(',');
    // number_of_comments
    aline.append(activity.getCommentedIds().length).append(',');
    // reactivity: difference in days between a day of posted and a day when
    // user commented/liked: 0..1, where 1 is same day, 0 is 30+ days old
    // TODO Should we take in account user login history: time between nearest
    // login and the reaction? Indeed this may be not accurate.
    aline.append(influencers.getPostReactivity(activity.getId())).append(',');

    final String myId = influencers.getUserIdentity().getId();
    final Collection<String> myConns = influencers.getUserConnections().keySet();

    // is_mentions_me
    // is_mentions_connections
    encContainsMeOthers(aline, myId, myConns, activity.getMentionedIds()).append(',');

    // is_commented_by_me
    // is_commented_by_connetions
    encContainsMeOthers(aline, myId, myConns, activity.getCommentedIds()).append(',');

    // is_liked_by_me
    // is_liked_by_connections
    encContainsMeOthers(aline, myId, myConns, activity.getLikeIdentityIds()).append(',');

    // Poster (creator)
    String posterId = activity.getPosterId();
    aline.append(posterId).append(','); // poster ID
    if (SpaceActivityPublisher.SPACE_PROFILE_ACTIVITY.equals(activity.getType()) && posterId.equals(ownerId)) {
      // It's Space itself posted an update (e.g. member joined)
      // Identity poster = getSpaceIdentityById(posterId);
      encGender(aline, null).append(',');
      // poster_is_employee
      aline.append('0').append(',');
      // TODO poster_is_lead
      aline.append('0').append(',');
      // poster_is_in_connections: 1 - as this user already a member
      aline.append('1').append(',');
      // poster_focus_*: poster job position as team membership encoded
      // TODO may be we would provide a focus of this space regarding the user?
      encPosition(aline, null).append(',');
    } else {
      UserIdentity poster = getUserIdentityById(posterId);
      // TODO poster full name?
      // aline.append(posterProfile.getFullName()).append(',');
      // poster gender: encoded
      encGender(aline, poster).append(',');
      // poster_is_employee
      aline.append(isEmployee(poster) ? '1' : '0').append(',');
      // TODO poster_is_lead
      aline.append('0').append(',');
      // poster_is_in_connections
      aline.append(myConns.contains(posterId) ? '1' : '0').append(',');
      // poster_focus_*: poster job position as team membership encoded
      encPosition(aline, poster).append(',');
    }
    // poster_influence
    aline.append(influencers.getParticipantWeight(posterId, ownerId)).append(',');

    // Find top 5 participants in this activity, we need not less than 5!
    for (ActivityParticipant p : findTopParticipants(activity, influencers, isSpace, ACTIVITY_PARTICIPANTS_TOP)) {
      // participantN_id
      aline.append(p.id).append(',');
      // participantN_conversed
      aline.append(p.isConversed).append(',');
      // participantN_favored
      aline.append(p.isFavored).append(',');
      //
      UserIdentity part = getUserIdentityById(p.id);
      // participantN_gender: encoded
      encGender(aline, part).append(',');
      // participantN_is_employee
      aline.append(isEmployee(part) ? '1' : '0').append(',');
      // aline.append('0').append(','); // TODO
      // TODO participantN_is_lead
      aline.append('0').append(',');
      // participantN_is_in_connections
      aline.append(myConns.contains(p.id) ? '1' : '0').append(',');
      // participantN_focus_*: job position as team membership encoded
      encPosition(aline, part).append(',');
      // participantN_influence
      aline.append(influencers.getParticipantWeight(p.id, ownerId)).append(',');
      // LOG.info("<<< Added activity participant: " + p.id + "@" +
      // activity.getId() + " <<<<");
    }

    // remove ending comma
    aline.deleteCharAt(aline.length() - 1);

    // LOG.info("<< Added activity: " +
    // influencers.getUserIdentity().getRemoteId() + "@" + activity.getId());
    return aline.toString();
  }

  protected StringBuilder encPosition(StringBuilder aline, UserIdentity identity) {
    // Columns order: engineering, sales&support, marketing, management,
    // financial, other
    if (identity != null && identity.getPosition() != null) {
      String position = identity.getPosition().toUpperCase().toLowerCase();
      if (ENGINEERING_PATTERN.matcher(position).matches()) {
        aline.append("1,0,0,0,0,0");
      } else if (SALES_PATTERN.matcher(position).matches()) {
        aline.append("0,1,0,0,0,0");
      } else if (MARKETING_PATTERN.matcher(position).matches()) {
        aline.append("0,0,1,0,0,0");
      } else if (MANAGEMENT_PATTERN.matcher(position).matches()) {
        aline.append("0,0,0,1,0,0");
      } else if (FINANCIAL_PATTERN.matcher(position).matches()) {
        aline.append("0,0,0,0,1,0");
      } else {
        aline.append("0,0,0,0,0,1");
      }
    } else {
      aline.append("0,0,0,0,0,1");
    }
    return aline;
  }

  protected StringBuilder encGender(StringBuilder aline, UserIdentity identity) {
    // Columns order: male, female
    if (identity != null && identity.getGender() != null) {
      if (identity.getGender().equals(Profile.FEMALE)) {
        aline.append("0,1");
      } else {
        aline.append("1,0");
      }
    } else {
      aline.append("1,0");
    }
    return aline;
  }

  protected StringBuilder encActivityType(StringBuilder aline, String activityType) {
    // Write several columns, fill the one with 1 for given type, others with 0
    //
    // Possible types: exosocial:people, exosocial:spaces,
    // cs-calendar:spaces,
    // ks-forum:spaces, ks-wiki:spaces, ks-poll:spaces, poll:spaces,
    // contents:spaces, files:spaces,
    // sharefiles:spaces, sharecontents:spaces
    // outlook:attachment etc.
    //
    // Columns order: content, social, calendar, forum, wiki, poll, other
    if (activityType != null) {
      if (activityType.indexOf("content:") > 0) {
        aline.append("1,0,0,0,0,0,0");
      } else if (activityType.indexOf("social:") > 0) {
        aline.append("0,1,0,0,0,0,0");
      } else if (activityType.indexOf("calendar:") > 0) {
        aline.append("0,0,1,0,0,0,0");
      } else if (activityType.indexOf("forum:") > 0) {
        aline.append("0,0,0,1,0,0,0");
      } else if (activityType.indexOf("wiki:") > 0) {
        aline.append("0,0,0,0,1,0,0");
      } else if (activityType.indexOf("poll:") > 0) {
        aline.append("0,0,0,0,0,1,0");
      } else {
        aline.append("0,0,0,0,0,0,1");
      }
    } else {
      aline.append("0,0,0,0,0,0,1");
    }
    return aline;
  }

  protected boolean isEmployee(UserIdentity identity) {
    if (identity != null && identity.getId() != null) {
      Set<String> employees = focusGroups.get(EMPLOYEES_GROUPID);
      return employees != null ? employees.contains(identity.getId()) : false;
    } else {
      return false;
    }
  }

  protected StringBuilder encContainsMeOthers(StringBuilder aline, String me, Collection<String> others, String[] target) {
    int isMe = 0;
    int isOthers = 0;
    for (String o : target) {
      if (me.equals(o)) {
        isMe = 1;
      }
      if (others.contains(o)) {
        isOthers = 1;
      }
      if (isMe == 1 && isOthers == 1) {
        break; // Make loop quicker if already know everything
      }
    }
    aline.append(isMe).append(',');
    aline.append(isOthers);
    return aline;
  }

  protected Collection<ActivityParticipant> findTopParticipants(ExoSocialActivity activity,
                                                                UserInfluencers influencers,
                                                                boolean isInSpace,
                                                                int topLength) {
    Map<String, ActivityParticipant> top = new LinkedHashMap<>();
    // 1) Commenters are first line candidates:
    Iterator<String> piter = Arrays.asList(activity.getCommentedIds()).iterator();
    while (piter.hasNext() && top.size() < topLength) {
      top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, true, false));
    }
    if (top.size() < topLength) {
      // 2) Mentioned are second line (count them as passive commenters)
      piter = Arrays.asList(activity.getMentionedIds()).iterator();
      while (piter.hasNext() && top.size() < topLength) {
        top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, true, false));
      }
    }
    if (top.size() < topLength) {
      // 3) Liked are third line
      piter = Arrays.asList(activity.getLikeIdentityIds()).iterator();
      while (piter.hasNext() && top.size() < topLength) {
        top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, false, true));
      }
    }
    if (top.size() < topLength) {
      // 4) Viewers are fourth line: find connection/space viewers for given
      // user:
      // 4.1) First try over user connections/space-members who can
      // access the activity stream and last active around the activity date -
      // this seems a heavy op.

      // TODO attempt to select most relevant connections/members (use another
      // ML to figure out 'leaders' in the intranet/spaces)

      // Count on user history for 30 days after the activity
      // long beforeTime = 15 * 60 * 1000; // 15min in ms
      // long activityDate = activity.getPostedTime();
      // long activityScopeTimeBegin = activityDate - beforeTime;
      // long activityScopeTimeEnd = activityScopeTimeBegin
      // + (UserInfluencers.DAY_LENGTH_MILLIS *
      // UserInfluencers.REACTIVITY_DAYS_RANGE);

      if (isInSpace) {
        // Get space managers and members who were logged-in
        Space space = spaceService.getSpaceByGroupId(activity.getStreamOwner());
        if (space != null) {
          Collection<String> smanagers = userIds(space.getManagers());
          for (Iterator<String> miter = smanagers.iterator(); miter.hasNext() && top.size() < topLength;) {
            String mid = miter.next();
            // if (wasUserLoggedin(mid, activityScopeTimeBegin,
            // activityScopeTimeEnd)) {
            top.computeIfAbsent(mid, p -> new ActivityParticipant(p, false, false));
            // }
          }
          Collection<String> smembers = userIds(space.getMembers());
          for (Iterator<String> miter = smembers.iterator(); miter.hasNext() && top.size() < topLength;) {
            String mid = miter.next();
            // if (wasUserLoggedin(mid, activityScopeTimeBegin,
            // activityScopeTimeEnd)) {
            top.computeIfAbsent(mid, p -> new ActivityParticipant(p, false, false));
            // }
          }
          // if top still not full, we add managers w/o login check
          for (Iterator<String> miter = smanagers.iterator(); miter.hasNext() && top.size() < topLength;) {
            top.computeIfAbsent(miter.next(), p -> new ActivityParticipant(p, false, false));
          }
        }
      }

      if (top.size() < topLength) {
        // Add user connections who were logged-in
        Iterator<Identity> citer = influencers.getUserConnections().values().iterator();
        while (citer.hasNext() && top.size() < topLength) {
          String cid = citer.next().getId();
          // if (wasUserLoggedin(cid, activityScopeTimeBegin,
          // activityScopeTimeEnd)) {
          top.computeIfAbsent(cid, p -> new ActivityParticipant(p, false, false));
          // }
        }
      }

      if (top.size() < topLength) {
        // 4.2) Second and finally, if user has no spaces/connections,
        // // * then we choose user's group managers
        // // TODO first use Employees group (or similar)
        // Collection<Group> userGroups = influencers.getUserGroups();
        // if (userGroups != null) {
        // for (Group ug : userGroups) {
        // String gid = ug.getId();
        // if (!gid.startsWith(SpaceUtils.SPACE_GROUP) &&
        // !gid.startsWith(SpaceUtils.PLATFORM_USERS_GROUP)) {
        // // Use non space groups
        // Iterator<String> miter = getGroupMembers(gid, "manager").iterator();
        // while (miter.hasNext() && top.size() < topLength) {
        // // String userId = gusers.next().getUserName();
        // String uname = miter.next();
        // Identity mid =
        // identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
        // uname, false);
        // if (mid != null) {
        // top.computeIfAbsent(mid.getId(), p -> new ActivityParticipant(p,
        // false, false));
        // } else {
        // LOG.warn("Group '" + gid + "' manager identity cannot be found in
        // Social: " + uname);
        // }
        // }
        // }
        // }
        // }
        // * choose Root user and members of Admins group as participants
        if (top.size() < topLength) {
          UserIdentity sid = getUserIdentityByName(userACL.getSuperUser());
          if (sid != null) {
            top.computeIfAbsent(sid.getId(), p -> new ActivityParticipant(p, false, false));
          } else {
            LOG.warn("Root user identity cannot be found in Social");
          }
          Iterator<String> aiter = focusGroups.get(userACL.getAdminGroups()).iterator();
          while (aiter.hasNext() && top.size() < topLength) {
            top.computeIfAbsent(aiter.next(), p -> new ActivityParticipant(p, false, false));
            // String aname = aiter.next();
            // UserIdentity aid = getUserIdentityByName(aname);
            // if (aid != null) {
            // top.computeIfAbsent(aid.getId(), p -> new ActivityParticipant(p,
            // false, false));
            // } else {
            // LOG.warn("Admin group member identity cannot be found in Social:
            // " + aname);
            // }
          }
        }
        // * TODO if not enough in admins - then guess random from "similar"
        // users in the organization,
        // * if no users - add "blank" user with id=0.
        if (top.size() < topLength) {
          int needAdd = topLength - top.size();
          do {
            top.computeIfAbsent(DUMMY_ID, p -> new ActivityParticipant(p, false, false));
            needAdd--;
          } while (needAdd > 0);
        }
      }
    }
    return Collections.unmodifiableCollection(top.values());
  }

  protected Collection<String> userIds(String... names) {
    Set<String> res = new LinkedHashSet<>();
    for (String name : names) {
      UserIdentity socId = getUserIdentityByName(name);
      if (socId == null) {
        LOG.error("Cannot find social identity (userIds): {}", name);
      } else {
        res.add(socId.getId());
      }
    }
    return res;
  }

  protected boolean wasUserLoggedin(String userId, long fromTime, long toTime) {
    try {
      List<LoginHistoryBean> phistory = loginHistory.getLoginHistory(userId, fromTime, toTime);
      // List is descending order, thus we see from the last (earlier login)
      // ListIterator<LoginHistoryBean> liter =
      // phistory.listIterator(phistory.size());
      // while (liter.hasPrevious()) {
      // LoginHistoryBean hbean = liter.previous();
      // if (hbean.getLoginTime() >= activityScopeTimeBegin &&
      // hbean.getLoginTime() < activityScopeTimeEnd) {
      // return true;
      // }
      // }
      // return false;
      return phistory.size() > 0;
    } catch (Exception e) {
      LOG.warn("Error reading login history for {}", userId);
      return false;
    }
  }

  /**
   * Gets the group members Social IDs filtered optionally by given type(s). If
   * no types given then all members will be returned. If types are given, then
   * returned set will contain members sorted in order of given memberships.
   * E.g. if types given ["manager", "member"], then the result will contain
   * managers first, then members.
   *
   * @param groupId the group ID
   * @param membershipTypes the membership types or <code>null</code> if need
   *          return all members
   * @return the group members IDs in a set
   */
  protected Set<String> getGroupMemberIds(String groupId, String... membershipTypes) {
    // FYI: be careful with large groups, like /platform/users, this may work
    // very slow as will read all the users.
    try {
      Iterator<User> uiter = ListAccessUtil.loadListIterator(organization.getUserHandler().findUsersByGroupId(groupId));
      if (membershipTypes != null && membershipTypes.length > 0) {
        Map<String, Set<String>> members = new LinkedHashMap<>();
        while (uiter.hasNext()) {
          String userName = uiter.next().getUserName();
          UserIdentity mid = getUserIdentityByName(userName);
          if (mid != null) {
            for (Membership um : organization.getMembershipHandler().findMembershipsByUserAndGroup(userName, groupId)) {
              for (String mt : membershipTypes) {
                if (um.getMembershipType().equals(mt)) {
                  members.computeIfAbsent(mt, m -> new LinkedHashSet<>()).add(mid.getId());
                }
              }
            }
          } else {
            LOG.warn("Group member identity cannot be found in Social: {}", userName);
          }
        }
        return members.values().stream().collect(LinkedHashSet::new, Set::addAll, Set::addAll);
      } else {
        Set<String> members = new LinkedHashSet<>();
        while (uiter.hasNext()) {
          String userName = uiter.next().getUserName();
          UserIdentity mid = getUserIdentityByName(userName);
          if (mid != null) {
            members.add(mid.getId());
          } else {
            LOG.warn("Group member identity cannot be found in Social: {}", userName);
          }
        }
        return members;
      }
    } catch (Exception e) {
      LOG.warn("Error reading group members for {} of types {} : {}", groupId, membershipTypes, e);
      return null;
    }
  }

  protected SpaceIdentity getSpaceIdentityByName(String spaceName) {
    if (spaceName != null) {// userIdentities.remove("peter")
      return spaceIdentities.computeIfAbsent(spaceName, name -> {
        // LOG.info("> Get space identity by Name: " + name);
        SpaceIdentity theIdentity;
        Identity socId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, name, false);
        if (socId != null) {
          theIdentity = spaceIdentity(socId);
          spaceIdentities.put(theIdentity.getId(), theIdentity); // map by ID
        } else {
          theIdentity = null;
          LOG.warn("Cannot find space identity by Name: {}", name);
        }
        // LOG.info("< Get space identity by Name: " + name);
        return theIdentity;
      });
    }
    return null;
  }

  protected SpaceIdentity getSpaceIdentityById(String spaceId) {
    if (!DUMMY_ID.equals(spaceId)) {
      return spaceIdentities.computeIfAbsent(spaceId, id -> {
        // LOG.info("> Get space identity by ID: " + id);
        SpaceIdentity theIdentity;
        Identity socId = identityManager.getIdentity(id, false);
        if (socId != null) {
          theIdentity = spaceIdentity(socId);
          spaceIdentities.put(socId.getRemoteId(), theIdentity); // map by Name
        } else {
          theIdentity = null;
          LOG.warn("Cannot find space identity by ID: {}", id);
        }
        // LOG.info("< Get space identity by ID: " + id);
        return theIdentity;
      });
    }
    return null;
  }

  protected UserIdentity getUserIdentityByName(String userName) {
    if (userName != null) {// userIdentities.remove("peter")
      SoftReference<UserIdentity> ref = userIdentities.computeIfAbsent(userName, name -> {
        // LOG.info("> Get user identity by Name: " + name);
        SoftReference<UserIdentity> newRef;
        Identity socId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, false);
        if (socId != null) {
          newRef = new SoftReference<UserIdentity>(userIdentity(socId));
          userIdentities.put(socId.getId(), newRef); // map by ID
        } else {
          newRef = null;
          LOG.warn("Cannot find user identity by Name: {}", name);
        }
        // LOG.info("< Get user identity by Name: " + name);
        return newRef;
      });
      if (ref != null) {
        return ref.get();
      }
    }
    return null;
  }

  protected UserIdentity getUserIdentityById(String identityId) {
    if (!DUMMY_ID.equals(identityId)) {
      SoftReference<UserIdentity> ref = userIdentities.computeIfAbsent(identityId, id -> {
        // LOG.info("> Get user identity by ID: " + id);
        SoftReference<UserIdentity> newRef;
        Identity socId = identityManager.getIdentity(id, false);
        if (socId != null) {
          newRef = new SoftReference<UserIdentity>(userIdentity(socId));
          userIdentities.put(socId.getRemoteId(), newRef); // map by Name
        } else {
          newRef = null;
          LOG.warn("Cannot find user identity by ID: {}", id);
        }
        // LOG.info("< Get user identity by ID: " + id);
        return newRef;
      });
      if (ref != null) {
        return ref.get();
      }
    }
    return null;
  }

  protected UserIdentity cacheUserIdentity(UserIdentity id) {
    SoftReference<UserIdentity> ref = new SoftReference<UserIdentity>(id);
    userIdentities.put(id.getId(), ref);
    userIdentities.put(id.getRemoteId(), ref);
    return id;
  }

  protected SpaceIdentity cacheSpaceIdentity(SpaceIdentity id) {
    spaceIdentities.put(id.getId(), id);
    spaceIdentities.put(id.getRemoteId(), id);
    return id;
  }

  protected UserIdentity userIdentity(Identity socId) {
    String id = socId.getId();
    String userName = socId.getRemoteId();
    LOG.info(">> Get social profile: {} ( {} ) <<", id, userName);
    Profile socProfile = identityManager.getProfile(socId);
    if (socProfile != null) {
      return new UserIdentity(id, userName, socProfile.getGender(), socProfile.getPosition());
    } else {
      // TODO add listener to check when user will fill his profile and
      // then update the mapping
      LOG.warn("Cannot find profile of social identity: {} ( {} )", id, userName);
      return new UserIdentity(id, userName, null, null);
    }
  }

  protected SpaceIdentity spaceIdentity(Identity socId) {
    return new SpaceIdentity(socId.getId(), socId.getRemoteId());
  }

  /**
   * Create a new thread executor service.
   *
   * @param threadNamePrefix the thread name prefix
   * @param maxFactor - max processes per CPU core
   * @param queueFactor - queue size per CPU core
   * @return the executor service
   */
  protected ExecutorService createThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
    // Executor will queue all commands and run them in maximum set of threads.
    // Minimum set of threads will be
    // maintained online even idle, other inactive will be stopped in two
    // minutes.
    final int cpus = Runtime.getRuntime().availableProcessors();
    int poolThreads = cpus / 4;
    poolThreads = poolThreads < MIN_THREADS ? MIN_THREADS : poolThreads;
    int maxThreads = Math.round(cpus * 1f * maxFactor);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_MAX_THREADS ? MIN_MAX_THREADS : maxThreads;
    int queueSize = cpus * queueFactor;
    queueSize = queueSize < queueFactor ? queueFactor : queueSize;
    // if (LOG.isDebugEnabled()) {
    LOG.info("Creating thread executor {}* for {}..{} threads, queue size {}",
             threadNamePrefix,
             poolThreads,
             maxThreads,
             queueSize);
    // }
    return new ThreadPoolExecutor(poolThreads,
                                  maxThreads,
                                  THREAD_IDLE_TIME,
                                  TimeUnit.SECONDS,
                                  new LinkedBlockingQueue<Runnable>(queueSize),
                                  new WorkerThreadFactory(threadNamePrefix),
                                  new ThreadPoolExecutor.CallerRunsPolicy());
  }

  protected File openBuckerDir(String bucketName) {
    // TODO Temporally spool all users datasets into a dedicated folder into
    // ${gatein.data.dir}/data-collector/${bucketName}
    // String dataDirPath = System.getProperty("gatein.data.dir");

    String dataDirPath = System.getProperty("java.io.tmpdir");
    if (dataDirPath == null || dataDirPath.trim().length() == 0) {
      dataDirPath = System.getProperty("exo.data.dir");
      if (dataDirPath == null || dataDirPath.trim().length() == 0) {
        dataDirPath = System.getProperty("java.io.tmpdir");
        LOG.warn("Platoform data dir not defined. Will use: {}", dataDirPath);
      }
    }
    if (bucketName == null || bucketName.trim().length() == 0) {
      bucketName = "bucketRecords-" + new SimpleDateFormat(FILE_TIMESTAMP_FORMAT).format(new Date());
    }

    File bucketDir = new File(dataDirPath + "/data-collector/" + bucketName);
    bucketDir.mkdirs();

    return bucketDir;
  }

}
