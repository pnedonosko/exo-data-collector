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

import static org.exoplatform.datacollector.ListAccessUtil.loadActivitiesListIterator;
import static org.exoplatform.datacollector.ListAccessUtil.loadListAll;
import static org.exoplatform.datacollector.ListAccessUtil.loadListIterator;
import static org.exoplatform.datacollector.UserInfluencers.ACTIVITY_PARTICIPANTS_TOP;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;

import com.google.common.collect.Lists;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.datacollector.dao.ActivityCommentedDAO;
import org.exoplatform.datacollector.dao.ActivityLikedDAO;
import org.exoplatform.datacollector.dao.ActivityMentionedDAO;
import org.exoplatform.datacollector.dao.ActivityPostedDAO;
import org.exoplatform.datacollector.dao.IdentityProfileDAO;
import org.exoplatform.datacollector.domain.IdentityProfileEntity;
import org.exoplatform.datacollector.identity.SpaceIdentity;
import org.exoplatform.datacollector.identity.UserIdentity;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.platform.gadget.services.LoginHistory.LastLoginBean;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryBean;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.prediction.TrainingService;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;
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
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
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
import org.exoplatform.social.core.storage.api.IdentityStorage;

/**
 * The Class SocialDataCollectorService.
 */
public class SocialDataCollectorService implements Startable {

  /** Logger */
  private static final Log       LOG                  = ExoLogger.getExoLogger(SocialDataCollectorService.class);

  public static final int        BATCH_SIZE           = 1000;

  public static final String     DUMMY_ID             = "0".intern();

  public static final String     EMPTY_STRING         = "".intern();

  /**
   * Base minimum number of threads for worker thread executors.
   */
  public static final int        MIN_THREADS          = 2;

  /**
   * Minimal number of threads maximum possible for worker thread executors.
   */
  public static final int        MIN_MAX_THREADS      = 4;

  /** Thread idle time for thread executors (in seconds). */
  public static final int        THREAD_IDLE_TIME     = 120;

  /**
   * Maximum threads per CPU for worker thread executors.
   */
  public static final int        WORKER_MAX_FACTOR    = 2;

  /**
   * Queue size per CPU for worker thread executors.
   */
  public static final int        WORKER_QUEUE_FACTOR  = WORKER_MAX_FACTOR * 20;

  /**
   * Thread name used for worker thread.
   */
  public static final String     WORKER_THREAD_PREFIX = "datacollector-worker-thread-";

  public static final String     ENGINEERING_FOCUS    = "engineering".intern();

  public static final String     SALES_FOCUS          = "sales".intern();

  public static final String     MARKETING_FOCUS      = "marketing".intern();

  public static final String     MANAGEMENT_FOCUS     = "management".intern();

  public static final String     FINANCIAL_FOCUS      = "financial".intern();

  public static final String     OTHER_FOCUS          = "other".intern();

  protected static final Pattern ENGINEERING_PATTERN  =
                                                     Pattern.compile("^.*developer|architect|r&d|mobile|qa|fqa|tqa|test|quality|qualité|expert|integrator|designer|cwi|technical advisor|services delivery|software engineer.*$");

  protected static final Pattern SALES_PATTERN        =
                                               Pattern.compile("^.*consultant|sales|client|support|sales engineer|demand generator.*$");

  protected static final Pattern MARKETING_PATTERN    =
                                                   Pattern.compile("^.*brand|communication|marketing|customer success|user experience|.*$");

  protected static final Pattern MANAGEMENT_PATTERN   =
                                                    Pattern.compile("^.*officer|chief|founder|coo|cto|cio|evp|advisor|product manager|director|general manager.*$");

  protected static final Pattern FINANCIAL_PATTERN    = Pattern.compile("^.*accountant|financial|investment|account manager.*$");

  /**
   * The Constant EMPLOYEES_GROUPID - TODO better make it configurable. For
   * /Groups/spaces/exo_employees.
   */
  protected static final String  EMPLOYEES_GROUPID    = "/spaces/exo_employees";

  /** The Constant for ACTIVE USERS virtual group. */
  protected static final String  ACTIVE_USERS         = "$active_users";

  protected static final Integer RECENT_LOGINS_COUNT  = 20;                                                                                                                                                                       // 3600000L;

  protected static final String  MAIN_BUCKET_PREFIX   = "prod";

  protected static final String  AUTOSTART_MODE_PARAM = "autostart";

  protected static final String  TRAIN_PERIOD_PARAM   = "train-period";

  /**
   * Sneaky throw given exception. An idea grabbed from <a href=
   * "https://www.baeldung.com/java-sneaky-throws">https://www.baeldung.com/java-sneaky-throws</a><br>
   *
   * @param <E> the element type
   * @param e the e
   * @throws E the e
   */
  @SuppressWarnings("unchecked")
  static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  /**
   * The BucketWorker gets target users from login history or bucketRecords and
   * processes them in loginsQueue order. Collects a new dataset and sends it to
   * TrainingService, if the user's model needs training. Schedules next
   * execution in TRAIN_PERIOD ms.
   */
  public class BucketWorker extends ContainerCommand {
    private AtomicBoolean runWorker = new AtomicBoolean(true);

    public BucketWorker(String containerName) {
      super(containerName);
    }

    @Override
    void execute(ExoContainer exoContainer) {
      if (!runWorker.get()) {
        return;
      }
      if (runMainLoop.get()) {
        currentBucketIndex++;
        LOG.info("Bucket processing time! Current bucket: {}", MAIN_BUCKET_PREFIX + currentBucketIndex);
      }

      Map<String, Date> targetUsers = getTargetUsers();

      while (!loginsQueue.isEmpty() && runMainLoop.get()) {
        String userName = loginsQueue.poll();
        Date loginDate = targetUsers.get(userName);
        LOG.info("Getting user from bucket: {}", userName);
        ModelEntity existingModel = trainingService.getLastModel(userName);

        if (modelNeedsTraining(existingModel, loginDate)) {
          submitUserCollector(userName, MAIN_BUCKET_PREFIX + currentBucketIndex, true);
          LOG.info("Started processing {}", userName);
        } else {
          LOG.info("User's {} model doesn't need training ", userName);
        }
        targetUsers.remove(userName);
      }

      if (runMainLoop.get()) {
        currentWorker = new BucketWorker(containerName);
        timer.schedule(currentWorker, trainPeriod);
      }
    }

    @Override
    void onContainerError(String error) {
      LOG.error("Container error has occured: {}", error);
    }

    /**
     * Returns bucketRecords
     * 
     * @return bucketRecords
     */
    Map<String, Date> getTargetUsers() {
      return bucketRecords;
    }

    /**
     * Checks if the model needs (re)training
     * 
     * @param model to be checked
     * @param loginDate login date
     * @return true, if model needs (re)training, otherwise - false
     */
    boolean modelNeedsTraining(ModelEntity model, Date loginDate) {
      // TODO: refactor statements
      if (model == null) {
        return true;
      }
      if (Status.RETRY.equals(model.getStatus())) {
        return true;
      }
      if (Status.FAILED_DATASET.equals(model.getStatus()) || Status.FAILED_TRAINING.equals(model.getStatus())
          || Status.PROCESSING.equals(model.getStatus())) {
        return false;
      }

      return model.getActivated() == null || Math.abs(model.getActivated().getTime() - loginDate.getTime()) > trainPeriod;
    }

    void stop() {
      runWorker.set(false);
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
      if (!runMainLoop.get()) {
        return;
      }

      String userId = event.getData().getIdentity().getUserId();
      if (!bucketRecords.containsKey(userId)) {
        loginsQueue.add(userId);
        bucketRecords.put(userId, new Date());
        LOG.info("User {} has logged in and added to bucketRecords", event.getData().getIdentity().getUserId());
      }
    }
  }

  /**
   * Internal class to be used within lambda-calculations in the collector.
   */
  @Deprecated // TODO not used
  class DeferredProcessingException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3708618505744089276L;

    DeferredProcessingException() {
      super();
    }

    DeferredProcessingException(String message, Throwable cause) {
      super(message, cause);
    }

    DeferredProcessingException(Throwable cause) {
      super(cause);
    }
  }

  protected final FileStorage                               fileStorage;

  protected final IdentityManager                           identityManager;

  protected final IdentityStorage                           identityStorage;

  protected final ActivityManager                           activityManager;

  protected final RelationshipManager                       relationshipManager;

  protected final SpaceService                              spaceService;

  protected final OrganizationService                       organization;

  protected final LoginHistoryService                       loginHistory;

  protected final UserACL                                   userACL;

  protected final ActivityCommentedDAO                      commentStorage;

  protected final ActivityPostedDAO                         postStorage;

  protected final ActivityLikedDAO                          likeStorage;

  protected final ActivityMentionedDAO                      mentionStorage;

  protected final IdentityProfileDAO                        identityProfileStorage;

  protected final ListenerService                           listenerService;

  protected final TrainingService                           trainingService;

  protected final Map<String, Set<String>>                  focusGroups         = new HashMap<>();

  // TODO clean this map time-from-time to do not consume the RAM
  protected final Map<String, SoftReference<UserIdentity>>  userIdentities      = new HashMap<>();

  protected final Map<String, SoftReference<SpaceIdentity>> spaceIdentities     = new HashMap<>();

  protected final ExecutorService                           workers;

  protected final AtomicLong                                currentUsersUpdated = new AtomicLong(0);

  /**
   * Contains pairs K - user id, V - date of login to be processed by
   * BucketWorker
   */
  protected final ConcurrentHashMap<String, Date>           bucketRecords       = new ConcurrentHashMap<>();

  /** Contains users id ordered by the login time */
  protected final ConcurrentLinkedQueue<String>             loginsQueue         = new ConcurrentLinkedQueue<>();

  /** Calculated user influencers (aka runtime cache). */
  // TODO use exo cache with eviction instead of a map
  protected final ConcurrentHashMap<String, UserSnapshot>   userSnapshots       = new ConcurrentHashMap<>();

  protected Integer                                         currentBucketIndex  = -1;

  /** The timer for scheduled tasks execution */
  protected final Timer                                     timer               = new Timer();

  /** The flag is used to stop the main loop **/
  protected AtomicBoolean                                   runMainLoop         = new AtomicBoolean(false);

  /**
   * The train period. It's the delay between processing buckets. 180 min. by
   * default
   */
  protected Long                                            trainPeriod         = 180 * 60000L;

  /**
   * The current worker that executes or is going to execute the main loop of
   * collecting/training.
   */
  protected BucketWorker                                    currentWorker;

  /**
   * Instantiates a new data collector service.
   *
   * @param fileStorage the file storage
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param identityManager the identity manager
   * @param identityStorage the identity storage
   * @param activityManager the activity manager
   * @param relationshipManager the relationship manager
   * @param spaceService the space service
   * @param loginHistory the login history
   * @param listenerService the listener service
   * @param userACL the user ACL
   * @param postStorage the post storage
   * @param commentStorage the comment storage
   * @param likeStorage the like storage
   * @param mentionStorage the mention storage
   * @param trainingService the training service
   * @param initParams the init params
   */
  public SocialDataCollectorService(FileStorage fileStorage,
                                    RepositoryService jcrService,
                                    SessionProviderService sessionProviders,
                                    NodeHierarchyCreator hierarchyCreator,
                                    OrganizationService organization,
                                    IdentityManager identityManager,
                                    IdentityStorage identityStorage,
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
                                    IdentityProfileDAO identityProfileStorage,
                                    TrainingService trainingService,
                                    InitParams initParams) {
    this.fileStorage = fileStorage;
    this.identityManager = identityManager;
    this.identityStorage = identityStorage;
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
    this.identityProfileStorage = identityProfileStorage;
    this.trainingService = trainingService;
    this.workers = createThreadExecutor(WORKER_THREAD_PREFIX, WORKER_MAX_FACTOR, WORKER_QUEUE_FACTOR);

    ValueParam autostartParam = initParams.getValueParam(AUTOSTART_MODE_PARAM);
    ValueParam trainPeroidParam = initParams.getValueParam(TRAIN_PERIOD_PARAM);
    try {
      String pval = autostartParam.getValue();
      this.runMainLoop.set(pval != null ? Boolean.valueOf(pval) : false);
    } catch (Exception e) {
      LOG.warn("Cannot set manual mode to {}, using auto mode by default", autostartParam.getValue());
    }
    try {
      String pval = trainPeroidParam.getValue();
      this.trainPeriod = Integer.parseInt(pval) * 60000L;
    } catch (Exception e) {
      LOG.warn("Cannot set the train period to {}, using {} min. by default",
               trainPeroidParam.getValue(),
               this.trainPeriod / 60000L);
    }
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

    listenerService.addListener(new LoginListener());

    if (runMainLoop.get()) {
      // TODO use(reuse) startMainLoop()
      final String containerName = ExoContainerContext.getCurrentContainer().getContext().getName();
      currentWorker = new StartWorker(containerName);
      workers.submit(currentWorker);
      LOG.info("Data Collector started (in automatic mode)");
    } else {
      LOG.info("Data Collector started (in manual mode)");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing
  }

  /**
   * Starts manual collecting and optionally training a user's model.
   * 
   * @param userName userName
   * @param bucketName bucket name
   * @param train trains the model if true
   * @throws Exception is thrown if the bucket is incorrect or user is not found
   *           or user's model is being processed
   */
  public void startUserCollector(String userName, String bucketName, boolean train) throws Exception {
    if (bucketName == null || bucketName.startsWith(MAIN_BUCKET_PREFIX)) {
      throw new Exception("Bucket name cannot be null or start with " + MAIN_BUCKET_PREFIX);
    }
    ModelEntity model = trainingService.getLastModel(userName);
    if (model == null || !Status.NEW.equals(model.getStatus()) && !Status.PROCESSING.equals(model.getStatus())) {
      submitUserCollector(userName, bucketName, train);
    } else {
      throw new Exception("The user is not found or being processed right now");
    }
  }

  /**
   * Collect user activities into an inferring dataset (for prediction).
   *
   * @param id the social identity
   * @param activities the activities
   * @return the string with a path to inferring dataset
   */
  public String collectActivities(Identity id, Collection<ExoSocialActivity> activities) {
    if (id != null) {
      ModelEntity model = trainingService.getLastModel(id.getRemoteId());
      if (model != null && Status.READY.equals(model.getStatus())) {
        // TODO Fix bucket name extraction after rework to relative paths
        // Ready model should have a model file - extract bucket name from it
        // TODO Should we store paths in the DB at all, may be bucket name will
        // be enough? And then we use predefined sub-tree
        // ($bucketName/$modelName/$dataFile).
        String modelFile = model.getModelFile();
        // TODO compile path in FileStorage?
        // File bucketDir = fileStorage.getBucketDir(bucketName);
        String bucketName = modelFile.substring(0, modelFile.lastIndexOf("/" + id.getRemoteId() + "/model"));
        bucketName = bucketName.substring(bucketName.lastIndexOf("/"));

        final File userFile = new File(modelFile.replace("/model", "/predict.csv"));
        try (PrintWriter writer = new PrintWriter(userFile)) {
          UserSnapshot user = getUserSnapshot(bucketName, id);
          if (user != null) {
            writeUserActivities(user, activities.iterator(), writer, false);
            LOG.info("Saved user inferring dataset into bucket file: {}", userFile.getAbsolutePath());
            return userFile.getAbsolutePath();
          } else {
            LOG.warn("User snapshot not found for {}", id.getRemoteId());
          }
        } catch (Exception e) {
          LOG.error("Cannot collect user inferring dataset for {} : {}", id.getRemoteId(), e);
          userFile.delete();
        }
      } else {
        LOG.warn("User model not found or not ready for {}", id.getRemoteId());
      }
    }
    return null;
  }

  /**
   * Collect user activities into an inferring dataset (for prediction).
   *
   * @param id the social identity
   * @param activityIds the activity IDs
   * @return the string with a path to inferring dataset
   */
  public String collectActivitiesByIds(Identity id, List<String> activityIds) {
    if (id != null) {
      List<ExoSocialActivity> activities = activityManager.getActivities(activityIds);
      return collectActivities(id, activities);
    }
    return null;
  }

  /**
   * Stops the main loop (collecting and training).
   *
   * @return <code>true</code>, if successfully stopped, <code>false</code> if
   *         already stopped
   */
  public boolean stopMainLoop() {
    if (runMainLoop.getAndSet(false)) {
      if (currentWorker != null) {
        currentWorker.stop();
        currentWorker = null;
      }
      LOG.info("Main loop (collecting and training user models) has been signaled for stopping");
      return true;
    }
    LOG.warn("Main loop (collecting and training user models) already stopped");
    return false;
  }

  /**
   * Start the main loop (collecting and training).
   *
   * @return <code>true</code>, if successfully started, <code>false</code> if
   *         already started
   */
  public boolean startMainLoop() {
    if (runMainLoop.getAndSet(true)) {
      LOG.warn("Main loop (collecting and training user models) already started");
      return false;
    }
    LOG.info("Starting main loop (collecting and training user models)");
    final String containerName = ExoContainerContext.getCurrentContainer().getContext().getName();
    currentWorker = new StartWorker(containerName);
    workers.submit(currentWorker);
    return true;
  }

  /**
   * Adds user to the loginsQueue and bucketRecords for processing in the next
   * bucket processing time.
   * 
   * @param userName to be added
   */
  public void addUser(String userName) {
    // TODO need better name: it's actually about TrainingService but lies in
    // Data Collector.
    ModelEntity currentModel = trainingService.getLastModel(userName);
    // If model exists and is not being processed right now
    if (currentModel != null && !Status.NEW.equals(currentModel.getStatus())
        && !Status.PROCESSING.equals(currentModel.getStatus())) {
      currentModel.setStatus(Status.RETRY);
      trainingService.update(currentModel);
      LOG.info("Model {} version {} got status RETRY.", currentModel.getName(), currentModel.getVersion());
    }
    if (!bucketRecords.containsKey(userName)) {
      bucketRecords.put(userName, new Date());
      loginsQueue.add(userName);
      LOG.info("User {} has beed added to the main queue for collecting and training", userName);
    } else {
      LOG.info("User {} is already in the main queue for collecting and training", userName);
    }
  }

  /**
   * Collect users activities into files bucketRecords in Platform data folder,
   * each file will have name of an user with <code>.csv</code> extension. If
   * such folder already exists, it will overwrite the files that match found
   * users. In case of error during the work, all partial results will be
   * deleted.
   *
   * @param bucketName the bucketRecords name, can be <code>null</code> then a
   *          timestamped name will be created
   * @return the saved bucketRecords folder or <code>null</code> if error
   *         occurred
   * @throws Exception the exception
   */
  public String collectUsersActivities(String bucketName) throws Exception {
    // Go through all users in the organization and swap their datasets
    // into separate data stream, then feed them to the Training Service

    File bucketDir = fileStorage.getBucketDir(bucketName);
    Iterator<Identity> idIter = loadListIterator(identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                                              new ProfileFilter(),
                                                                                              true));
    while (idIter.hasNext() && !Thread.currentThread().isInterrupted()) {
      final UserIdentity id = cacheUserIdentity(userIdentity(idIter.next()));
      submitUserCollector(id.getRemoteId(), bucketName, false);
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("Saving of dataset interrupted for bucketRecords: {}", bucketName);
      // Clean the bucketRecords files
      try {
        Arrays.asList(bucketDir.listFiles()).stream().forEach(f -> f.delete());
        bucketDir.delete();
      } catch (Exception e) {
        LOG.error("Error removing canceled bucketRecords folder: {}", bucketName);
      }
      return null;
    }
    LOG.info("Saved dataset successfully into bucketRecords folder: {}", bucketDir.getAbsolutePath());
    return bucketDir.getAbsolutePath();
  }

  /**
   * Gets the user by username on Organization service.
   *
   * @param userName the user name
   * @return the user by name
   */
  public Identity getUserByName(String userName) {
    return getUserIdentityByName(userName);
  }

  /**
   * Gets the user by Social ID.
   *
   * @param socialId the social id
   * @return the user by id
   */
  public Identity getUserById(String socialId) {
    return getUserIdentityById(socialId);
  }

  /**
   * Collect user activities into given files bucketRecords in Platform data
   * folder. Moves old model directory to a new one. Adds new model to DB
   *
   * @param bucketName the bucketRecords name
   * @param userName the user name
   * @param sinceTime the since time
   * @param withRank collect with rank if <code>true</code>
   * @return the dataset file
   */
  protected String collectUserActivities(String bucketName, String userName, long sinceTime, boolean withRank) {
    File bucketDir = fileStorage.getBucketDir(bucketName);
    LOG.info("Saving user dataset into bucket folder: {}", bucketDir.getAbsolutePath());
    final UserIdentity id = getUserIdentityByName(userName);
    if (id != null) {
      // TODO compile path in FileStorage?
      final File userFile = new File(bucketDir.getPath() + "/" + id.getRemoteId() + "/training.csv");
      userFile.getParentFile().mkdirs();

      try (PrintWriter writer = new PrintWriter(userFile)) {
        // Prepare an user state snapshot, it should not be modified until
        // another collecting
        UserSnapshot user = getUserSnapshot(bucketName, id);
        // XXX sinceTime here may not be the same as for activities until we'll
        // make UserInfluecners incrementally maintained and load them from DB
        initializeUserSnapshot(user, System.currentTimeMillis() - UserInfluencers.FEED_MILLIS_RANGE);
        
        Iterator<ExoSocialActivity> activities = loadActivitiesListIterator(activityManager.getActivityFeedWithListAccess(id),
                                                                            sinceTime);
        writeUserActivities(user, activities, writer, withRank);

        // Set the dataset path to the latest model in DB if exists
        // TODO Don't use absolute path but use relative to
        // data-collector/datasets path, so it will be possible to relocate file
        // storage w/o modifying DB. Relative path should start from a bucket
        // name.
        trainingService.setDatasetToLatestModel(userName, userFile.getAbsolutePath());

        // save/cache user snapshot only after successful write
        saveUserSnapshot(bucketName, user);
      } catch (Exception e) {
        LOG.error("Cannot collect user activities for {} : {}", userName, e);
        userFile.delete();
        return null;
      }

      LOG.info("Saved user dataset into bucket file: {}", userFile.getAbsolutePath());
      return userFile.getAbsolutePath();
    }
    LOG.warn("User social identity not found for {}", userName);
    return null;
  }

  /**
   * Collects user's all activities in separate worker. Trains model if
   * necessary.
   *
   * @param userName the user name
   * @param bucket the bucket
   * @param train if true - trains model
   */
  protected void submitUserCollector(String userName, String bucket, boolean train) {
    final String containerName = ExoContainerContext.getCurrentContainer().getContext().getName();
    workers.submit(new ContainerCommand(containerName) {
      @Override
      void execute(ExoContainer exoContainer) {
        long sinceTime;
        ModelEntity currentModel = trainingService.getLastModel(userName);
        if (currentModel != null && Status.READY.equals(currentModel.getStatus())) {
          // TODO do we need subtract a time gap (e.g. 100ms) from the created
          // time to catch in calculation activities that happened while the
          // previous model collected and saved?
          // In fact we create a model first, then collect and save, thus it may
          // be actually desired to add a time gap (best if of actually spent
          // time by previous model).
          // In general case it's question of how critical if some activities
          // will be lost or repeatedly taken in account by the model and
          // influencers?
          sinceTime = currentModel.getCreated().getTime();
        } else {
          sinceTime = System.currentTimeMillis() - UserInfluencers.FEED_MILLIS_RANGE;
        }
        if (train) {
          currentModel = trainingService.addModel(userName, null);
        }
        String dataset = collectUserActivities(bucket, userName, sinceTime, true);
        if (dataset != null && train) {
          copyModelFile(trainingService.getPreviousModel(userName), new File(dataset).getParentFile());
          trainingService.submitTrainModel(dataset, userName);
        }
        if (dataset == null) {
          // TODO need re-get it?
          // currentModel = trainingService.getLastModel(userName);
          if (Status.RETRY.equals(currentModel.getStatus())) {
            currentModel.setStatus(Status.FAILED_DATASET);
            LOG.warn("Model {} got status FAILED_DATASET. Cannot collect dataset. ", userName);
          } else {
            currentModel.setStatus(Status.RETRY);
            bucketRecords.put(userName, new Date());
            loginsQueue.add(userName);
            LOG.warn("Model {} got status RETRY. Cannot collect dataset. Added to next bucket ", userName);
          }
          trainingService.update(currentModel);
        }
      }

      @Override
      void onContainerError(String error) {
        LOG.error("Container error has occured: {}", error);
      }
    });
  }

  /**
   * Copies model file to new destination. Only if model has status READY
   * 
   * @param model contains model file to be copied
   * @param dest new folder
   */
  protected void copyModelFile(ModelEntity model, File dest) {
    if (model != null && model.getModelFile() != null && model.getStatus().equals(Status.READY)) {
      try {
        FileUtils.copyDirectoryToDirectory(new File(model.getModelFile()), dest);
        LOG.info("Old model file copied for {}", model.getName());
      } catch (IOException e) {
        LOG.info("Failed to copy old model file for {}, {}", model.getName(), e.getMessage());
      }
    }
  }

  /**
   * Write user activities to given writer.
   *
   * @param user the user state snapshot
   * @param activities the activities to process
   * @param out the writer where spool the user activities dataset
   * @param withRank if <code>true</code> then collect dataset with rank
   * @throws Exception the exception
   */
  protected void writeUserActivities(UserSnapshot user,
                                     Iterator<ExoSocialActivity> activities,
                                     PrintWriter out,
                                     boolean withRank) throws Exception {
    LOG.info("> Writing user activities for {}", user.getIdentity().getRemoteId());
    out.println(activityHeader(withRank));

    // load identity's activities and collect its data
    while (activities.hasNext() && !Thread.currentThread().isInterrupted()) {
      ExoSocialActivity activity = activities.next();
      out.println(activityLine(user, activity, withRank));
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("< Interrupted collector of user activities for {}", user.getIdentity().getRemoteId());
    } else {
      LOG.info("< Wrote user activities for {}", user.getIdentity().getRemoteId());
    }
  }

  /**
   * Collect user activities.
   *
   * @param id the user identity in Social
   * @param out the writer where spool the user activities dataset
   * @param withRank if <code>true</code> then collect dataset with rank
   * @throws Exception the exception
   */
  @Deprecated
  protected void collectUserActivities_OLD(UserIdentity id, PrintWriter out, boolean withRank) throws Exception {
    // TODO split this method on parts:
    // 1) consume parameter with UserSnapshot and Iterator<ExoSocialActivity>
    // 2) go over the iterator and write the dataset
    // 3) UserSnapshot should obtained in separate method (and used where
    // required)
    // 4) activities iterator - it's all we want put in the dataset

    LOG.info("> Collecting user activities for {}", id.getRemoteId());
    out.println(activityHeader(withRank));

    // Find this user favorite participants (influencers) and streams
    LOG.info(">> Buidling user influencers for {}", id.getRemoteId());
    Collection<Identity> conns = loadListAll(relationshipManager.getConnections(id));
    Collection<Space> spaces = loadListAll(spaceService.getMemberSpaces(id.getRemoteId()));
    final long sinceTime = System.currentTimeMillis() - UserInfluencers.FEED_MILLIS_RANGE;

    UserSnapshot user = userSnapshots.computeIfAbsent(id.getRemoteId(), userName -> {
      // Prepare an user state snapshot, it should not be modified until another
      // collecting
      // XXX Collector with merge function to solve duplicates in source
      // collections (may have place for connections)
      Map<String, Identity> connsSnapshot = Collections.unmodifiableMap(conns.stream()
                                                                             .collect(Collectors.toMap(c -> c.getId(),
                                                                                                       c -> c,
                                                                                                       (c1, c2) -> c1)));
      Map<String, SpaceSnapshot> spacesSnapshot =
                                                Collections.unmodifiableMap(spaces.stream()
                                                                                  .collect(Collectors.toMap(s -> s.getId(),
                                                                                                            s -> new SpaceSnapshot(s),
                                                                                                            (s1, s2) -> s1)));
      UserInfluencers influencers = new UserInfluencers(id, connsSnapshot, spacesSnapshot);

      influencers.addUserPosts(postStorage.findUserPosts(id.getId(), sinceTime));

      influencers.addCommentedPoster(commentStorage.findPartIsCommentedPoster(id.getId(), sinceTime));
      influencers.addCommentedCommenter(commentStorage.findPartIsCommentedCommenter(id.getId(), sinceTime));
      influencers.addCommentedConvoPoster(commentStorage.findPartIsCommentedConvoPoster(id.getId(), sinceTime));

      influencers.addPostCommenter(commentStorage.findPartIsPostCommenter(id.getId(), sinceTime));
      influencers.addCommentCommenter(commentStorage.findPartIsCommentCommenter(id.getId(), sinceTime));
      influencers.addConvoCommenter(commentStorage.findPartIsConvoCommenter(id.getId(), sinceTime));

      influencers.addMentioner(mentionStorage.findPartIsMentioner(id.getId(), sinceTime));
      influencers.addMentioned(mentionStorage.findPartIsMentioned(id.getId(), sinceTime));

      influencers.addLikedPoster(likeStorage.findPartIsLikedPoster(id.getId(), sinceTime));
      influencers.addLikedCommenter(likeStorage.findPartIsLikedCommenter(id.getId(), sinceTime));
      influencers.addLikedConvoPoster(likeStorage.findPartIsLikedConvoPoster(id.getId(), sinceTime));

      influencers.addPostLiker(likeStorage.findPartIsPostLiker(id.getId(), sinceTime));
      influencers.addCommentLiker(likeStorage.findPartIsCommentLiker(id.getId(), sinceTime));
      influencers.addConvoLiker(likeStorage.findPartIsConvoLiker(id.getId(), sinceTime));

      influencers.addSamePostLiker(likeStorage.findPartIsSamePostLiker(id.getId(), sinceTime));
      influencers.addSameCommentLiker(likeStorage.findPartIsSameCommentLiker(id.getId(), sinceTime));
      influencers.addSameConvoLiker(likeStorage.findPartIsSameConvoLiker(id.getId(), sinceTime));

      // Here the influencers object knows favorite streams of the user
      Set<String> favoriteStreams = influencers.getFavoriteStreamsTop(10);
      if (favoriteStreams.size() < 10) {
        // TODO add required (to 10) streams where user has most of its
        // connections
      }
      if (favoriteStreams.size() > 0) {
        influencers.addStreamPoster(postStorage.findPartIsFavoriteStreamPoster(id.getId(), sinceTime, favoriteStreams));
        influencers.addStreamCommenter(commentStorage.findPartIsFavoriteStreamCommenter(id.getId(), sinceTime, favoriteStreams));
        influencers.addStreamPostLiker(likeStorage.findPartIsFavoriteStreamPostLiker(id.getId(), sinceTime, favoriteStreams));
        influencers.addStreamCommentLiker(likeStorage.findPartIsFavoriteStreamCommentLiker(id.getId(),
                                                                                           sinceTime,
                                                                                           favoriteStreams));
      }
      LOG.info("<< Built user influencers for {}", id.getRemoteId());
      UserSnapshot userSnapshot = new UserSnapshot(id, connsSnapshot, spacesSnapshot);
      // Favorite streams calculated before addStream* methods call
      userSnapshot.setFavoriteStreams(Collections.unmodifiableSet(favoriteStreams));
      return userSnapshot;
    });

    // load identity's activities and collect its data
    Iterator<ExoSocialActivity> feedIter =
                                         loadActivitiesListIterator(activityManager.getActivityFeedWithListAccess(id), sinceTime);
    while (feedIter.hasNext() && !Thread.currentThread().isInterrupted()) {
      ExoSocialActivity activity = feedIter.next();
      out.println(activityLine(user, activity, withRank));
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("< Interrupted collector of user activities for {}", id.getRemoteId());
    } else {
      LOG.info("< Collected user activities for {}", id.getRemoteId());
    }
  }

  protected String snapshotId(String bucket, String id) {
    return new StringBuilder(bucket).append('-').append(id).toString();
  }

  protected UserSnapshot getUserSnapshot(String bucket, Identity id) throws Exception {
    // TODO use exo cache with eviction instead of a map
    return userSnapshots.computeIfAbsent(snapshotId(bucket, id.getRemoteId()), sid -> {
      try {
        return readUserSnapshot(id);
      } catch (Exception e) {
        sneakyThrow(e);
        return null; // it will not happen
      }
    });
  }

  private UserSnapshot readUserSnapshot(Identity id) throws Exception {
    // TODO read user snapshot from DB:
    // 1) connections and spaces from Social
    // 2) influencers from collector tables
    LOG.info("> Reading user snapshot for {}", id.getRemoteId());
    // TODO if not found in DB then return null
    UserSnapshot user = createUserSnapshot(id);
    // otherwise init from tables
    // user.initInfluencers(entity); 
    user.initInfluencers(); // XXX temporal until will load from DB
    LOG.info("< Read user snapshot for {}", id.getRemoteId());
    return user;
  }

  private void saveUserSnapshot(String buketName, UserSnapshot user) throws Exception {
    String userId = user.getIdentity().getRemoteId();
    LOG.info("> Saving user snapshot for {}", userId);
    // TODO read user snapshot from DB:
    // 1) save in the DB
    // 2) cache in runtime
    userSnapshots.put(snapshotId(buketName, userId), user);
    LOG.info("< Saved user snapshot for {}", userId);
  }

  protected void initializeUserSnapshot(UserSnapshot user, long sinceTime) throws Exception {
    String userId = user.getIdentity().getRemoteId();
    LOG.info(">> Initializing user snapshot for {}", userId);

    // TODO read user influencers from DB and if found set conns/spaces into it
    UserInfluencers influencers = user.initInfluencers();

    influencers.addUserPosts(postStorage.findUserPosts(userId, sinceTime));

    influencers.addCommentedPoster(commentStorage.findPartIsCommentedPoster(userId, sinceTime));
    influencers.addCommentedCommenter(commentStorage.findPartIsCommentedCommenter(userId, sinceTime));
    influencers.addCommentedConvoPoster(commentStorage.findPartIsCommentedConvoPoster(userId, sinceTime));

    influencers.addPostCommenter(commentStorage.findPartIsPostCommenter(userId, sinceTime));
    influencers.addCommentCommenter(commentStorage.findPartIsCommentCommenter(userId, sinceTime));
    influencers.addConvoCommenter(commentStorage.findPartIsConvoCommenter(userId, sinceTime));

    influencers.addMentioner(mentionStorage.findPartIsMentioner(userId, sinceTime));
    influencers.addMentioned(mentionStorage.findPartIsMentioned(userId, sinceTime));

    influencers.addLikedPoster(likeStorage.findPartIsLikedPoster(userId, sinceTime));
    influencers.addLikedCommenter(likeStorage.findPartIsLikedCommenter(userId, sinceTime));
    influencers.addLikedConvoPoster(likeStorage.findPartIsLikedConvoPoster(userId, sinceTime));

    influencers.addPostLiker(likeStorage.findPartIsPostLiker(userId, sinceTime));
    influencers.addCommentLiker(likeStorage.findPartIsCommentLiker(userId, sinceTime));
    influencers.addConvoLiker(likeStorage.findPartIsConvoLiker(userId, sinceTime));

    influencers.addSamePostLiker(likeStorage.findPartIsSamePostLiker(userId, sinceTime));
    influencers.addSameCommentLiker(likeStorage.findPartIsSameCommentLiker(userId, sinceTime));
    influencers.addSameConvoLiker(likeStorage.findPartIsSameConvoLiker(userId, sinceTime));

    // Here the influencers object knows favorite streams of the user
    Set<String> favoriteStreams = influencers.getFavoriteStreamsTop(10);
    if (favoriteStreams.size() < 10) {
      // TODO add required (to 10) streams where user has most of its
      // connections
    }
    if (favoriteStreams.size() > 0) {
      influencers.addStreamPoster(postStorage.findPartIsFavoriteStreamPoster(userId, sinceTime, favoriteStreams));
      influencers.addStreamCommenter(commentStorage.findPartIsFavoriteStreamCommenter(userId, sinceTime, favoriteStreams));
      influencers.addStreamPostLiker(likeStorage.findPartIsFavoriteStreamPostLiker(userId, sinceTime, favoriteStreams));
      influencers.addStreamCommentLiker(likeStorage.findPartIsFavoriteStreamCommentLiker(userId, sinceTime, favoriteStreams));
    }

    // Favorite streams calculated before addStream* methods call
    user.setFavoriteStreams(Collections.unmodifiableSet(favoriteStreams));
    LOG.info("<< Initialized user snapshot for {}", userId);
  }

  protected UserSnapshot createUserSnapshot(Identity id) throws Exception {
    Collection<Identity> conns = loadListAll(relationshipManager.getConnections(id));
    Collection<Space> spaces = loadListAll(spaceService.getMemberSpaces(id.getRemoteId()));

    // XXX Collector with merge function to solve duplicates in source
    // collections (may have place for connections)
    Map<String, Identity> connsSnapshot = Collections.unmodifiableMap(conns.stream()
                                                                           .collect(Collectors.toMap(c -> c.getId(),
                                                                                                     c -> c,
                                                                                                     (c1, c2) -> c1)));
    Map<String, SpaceSnapshot> spacesSnapshot =
                                              Collections.unmodifiableMap(spaces.stream()
                                                                                .collect(Collectors.toMap(s -> s.getId(),
                                                                                                          s -> new SpaceSnapshot(s),
                                                                                                          (s1, s2) -> s1)));
    UserSnapshot userSnapshot = new UserSnapshot(id, connsSnapshot, spacesSnapshot);
    return userSnapshot;
  }

  protected String activityHeader(boolean withRank) {
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
    if (withRank) {
      aline.append("rank");
    } else {
      aline.deleteCharAt(aline.length() - 1);
    }
    return aline.toString();
  }

  protected String activityLine(UserSnapshot user, ExoSocialActivity activity, boolean withRank) {
    ActivityRank rank = null;
    if (withRank) {
      rank = new ActivityRank();
    }
    StringBuilder aline = new StringBuilder();
    // Activity identification & type
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
    aline.append(user.getInfluencers().getStreamWeight(ownerId)).append(',');
    // number_of_likes
    aline.append(activity.getNumberOfLikes()).append(',');
    // number_of_comments
    aline.append(activity.getCommentedIds().length).append(',');
    // reactivity: difference in days between a day of posted and a day when
    // user commented/liked: 0..1, where 1 is same day, 0 is 30+ days old
    // TODO Should we take in account user login history: time between nearest
    // login and the reaction? Indeed this may be not accurate.
    // TODO reactivity has different logic for training and prediction:
    // when trained, we want know actual user reaction on the post
    // when predicting, a reaction is unknown and reactivity is full (1)
    double reactivity = user.getInfluencers().getPostReactivity(activity.getId());
    aline.append(reactivity).append(',');

    final String myId = user.getIdentity().getId();
    final Collection<String> myConns = user.getConnections().keySet();

    boolean participatedByMe = false;
    boolean participatedByConns = false;
    // is_mentions_me
    // is_mentions_connections
    encContainsMeOthers(aline, myId, myConns, activity.getMentionedIds()).append(',');
    participatedByMe = aline.charAt(aline.length() - 4) == '1';
    participatedByConns = aline.charAt(aline.length() - 2) == '1';

    // is_commented_by_me
    // is_commented_by_connetions
    encContainsMeOthers(aline, myId, myConns, activity.getCommentedIds()).append(',');
    participatedByMe = aline.charAt(aline.length() - 4) == '1';
    participatedByConns = aline.charAt(aline.length() - 2) == '1';

    boolean likedByMe = false;
    boolean likedByConns = false;
    // is_liked_by_me
    // is_liked_by_connections
    encContainsMeOthers(aline, myId, myConns, activity.getLikeIdentityIds()).append(',');
    likedByMe = aline.charAt(aline.length() - 4) == '1';
    likedByConns = aline.charAt(aline.length() - 2) == '1';

    // Contribute into target weight
    if (withRank) {
      rank.participatedByMe(participatedByMe);
      rank.participatedByConnections(participatedByConns);
      rank.likedByMe(likedByMe);
      rank.likedByConnections(likedByConns);
      rank.postedInFavoriteStream(user.getFavoriteStreams().contains(ownerId));
      // TODO app not yet used in features
      // target.postedInFavoriteApp(isPostedInFavoriteApp);
      rank.widelyLiked(user.getInfluencers().isWidelyLiked(activity.getNumberOfLikes()));
      rank.reactivity(reactivity);
    }

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
      encFocus(aline, null).append(',');
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
      encFocus(aline, poster).append(',');
    }
    // poster_influence
    double posterWeight = user.getInfluencers().getParticipantWeight(posterId, ownerId);
    aline.append(posterWeight).append(',');
    if (withRank) {
      rank.participatedByInfluencer(posterWeight);
      if (new HashSet<String>(Arrays.asList(activity.getLikeIdentityIds())).contains(posterId)) {
        rank.likedByInfluencer(posterWeight);
      }
    }

    // Find top 5 participants in this activity, we need not less than 5!
    for (ActivityParticipant p : findTopParticipants(activity, user, isSpace, ACTIVITY_PARTICIPANTS_TOP)) {
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
      encFocus(aline, part).append(',');
      // participantN_influence
      double pweight = user.getInfluencers().getParticipantWeight(p.id, ownerId);
      aline.append(pweight).append(',');
      // LOG.info("<<< Added activity participant: " + p.id + "@" +
      // activity.getId() + " <<<<");
      if (withRank) {
        if (p.isConversed > 0) {
          rank.participatedByInfluencer(pweight);
        } else if (p.isFavored > 0) {
          rank.likedByInfluencer(pweight);
        }
      }
    }

    if (withRank) {
      // rank column
      aline.append(rank.build());
    } else {
      // remove last comma
      aline.deleteCharAt(aline.length() - 1);
    }

    // LOG.info("<< Added activity: " +
    // influencers.getUserIdentity().getRemoteId() + "@" + activity.getId());
    return aline.toString();
  }

  protected StringBuilder encFocus(StringBuilder aline, UserIdentity identity) {
    // Columns order: engineering, sales&support, marketing, management,
    // financial, other
    if (identity != null) {
      if (ENGINEERING_FOCUS.equals(identity.getFocus())) {
        aline.append("1,0,0,0,0,0");
      } else if (SALES_FOCUS.equals(identity.getFocus())) {
        aline.append("0,1,0,0,0,0");
      } else if (MARKETING_FOCUS.equals(identity.getFocus())) {
        aline.append("0,0,1,0,0,0");
      } else if (MANAGEMENT_FOCUS.equals(identity.getFocus())) {
        aline.append("0,0,0,1,0,0");
      } else if (FINANCIAL_FOCUS.equals(identity.getFocus())) {
        aline.append("0,0,0,0,1,0");
      } else {
        aline.append("0,0,0,0,0,1"); // OTHER_FOCUS
      }
    } else {
      aline.append("0,0,0,0,0,1");
    }
    return aline;
  }

  protected String findFocus(String title) {
    // Columns order: engineering, sales&support, marketing, management,
    // financial, other
    if (title != null) {
      String position = title.toUpperCase().toLowerCase();
      if (ENGINEERING_PATTERN.matcher(position).matches()) {
        return ENGINEERING_FOCUS;
      } else if (SALES_PATTERN.matcher(position).matches()) {
        return SALES_FOCUS;
      } else if (MARKETING_PATTERN.matcher(position).matches()) {
        return MARKETING_FOCUS;
      } else if (MANAGEMENT_PATTERN.matcher(position).matches()) {
        return MARKETING_FOCUS;
      } else if (FINANCIAL_PATTERN.matcher(position).matches()) {
        return FINANCIAL_FOCUS;
      }
    }
    return OTHER_FOCUS;
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

  protected boolean contains(Collection<String> c1, Collection<String> c2) {
    if (c1.size() > c2.size()) {
      // We want loop over smaller collection to leverage c.contains() of a
      // bigger (in case of hash sets it will be faster)
      Collection<String> ctmp = c2;
      c2 = c1;
      c1 = ctmp;
    }
    for (String t : c1) {
      if (c2.contains(t)) {
        return true;
      }
    }
    return false;
  }

  protected Collection<ActivityParticipant> findTopParticipants(ExoSocialActivity activity,
                                                                UserSnapshot user,
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

      // TODO existing login history is short living and cleaned frequently
      // thus we may need maintain an own one.
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
        Iterator<Identity> citer = user.getConnections().values().iterator();
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
        // TODO first use Employees group (or similar)
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
    return getMapped_OLD(spaceIdentities,
                         spaceName,
                         name -> identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, name, false),
                         socId -> spaceIdentity(socId),
                         socId -> socId.getId());
  }

  protected SpaceIdentity getSpaceIdentityById(String spaceId) {
    return getMapped_OLD(spaceIdentities,
                         spaceId,
                         id -> identityManager.getIdentity(id, false),
                         socId -> spaceIdentity(socId),
                         socId -> socId.getRemoteId());
  }

  @Deprecated
  protected UserIdentity getUserIdentityByName_OLD(String userName) {
    return getMapped_OLD(userIdentities,
                         userName,
                         name -> identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, false),
                         socId -> userIdentity(socId),
                         socId -> socId.getId());
  }

  protected UserIdentity getUserIdentityByName(String userName) {
    return getMapped(userIdentities, userName, name -> {
      // Reading from the storage, if not exists - getting from Social API
      IdentityProfileEntity persisted = identityProfileStorage.findByName(userName);
      if (persisted != null) {
        String gender = null;
        if (persisted.getContext() != null) {
          gender = Arrays.asList(persisted.getContext().split(";"))
                         .stream()
                         .filter(key -> key.contains("gender:"))
                         .map(genderStr -> genderStr.split(":")[1])
                         .findFirst()
                         .orElse(null);

        }

        return new UserIdentity(persisted.getId(), persisted.getName(), gender, persisted.getFocus());
      }

      Identity socId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, false);
      if (socId != null) {
        UserIdentity uid = userIdentity(socId);
        // Save IdentityProfileEntry to the storage
        String context = "gender:" + uid.getGender();
        IdentityProfileEntity identityProfile = new IdentityProfileEntity(uid.getId(),
                                                                          uid.getRemoteId(),
                                                                          uid.getRemoteId(),
                                                                          uid.getFocus(),
                                                                          context);
        identityProfileStorage.create(identityProfile);
        return uid;
      } else {
        LOG.warn("Cannot find social identity by name: " + name);
        return null;
      }
    }, socId -> socId.getId());
  }

  @Deprecated
  protected UserIdentity getUserIdentityById_OLD(String identityId) {
    return getMapped_OLD(userIdentities,
                         identityId,
                         id -> identityManager.getIdentity(id, false),
                         socId -> userIdentity(socId),
                         socId -> socId.getRemoteId());
  }

  protected UserIdentity getUserIdentityById(String identityId) {
    return getMapped(userIdentities, identityId, id -> {
      // Reading from the storage, if not exists - getting from Social API
      IdentityProfileEntity persisted = identityProfileStorage.findById(identityId);
      if (persisted != null) {
        String gender = null;
        if (persisted.getContext() != null) {
          gender = Arrays.asList(persisted.getContext().split(";"))
                         .stream()
                         .filter(key -> key.contains("gender:"))
                         .map(genderStr -> genderStr.split(":")[1])
                         .findFirst()
                         .orElse(null);

        }

        return new UserIdentity(persisted.getId(), persisted.getName(), gender, persisted.getFocus());
      }

      Identity socId = identityManager.getIdentity(id, false);
      if (socId != null) {
        UserIdentity uid = userIdentity(socId);
        // Save IdentityProfileEntry to the storage
        String context = "gender:" + uid.getGender();
        IdentityProfileEntity identityProfile = new IdentityProfileEntity(uid.getId(),
                                                                          uid.getRemoteId(),
                                                                          uid.getRemoteId(),
                                                                          uid.getFocus(),
                                                                          context);
        identityProfileStorage.create(identityProfile);
        return uid;
      } else {
        LOG.warn("Cannot find social identity by ID: " + id);
        return null;
      }
    }, socId -> socId.getId());
  }

  @Deprecated
  private <I extends Identity> I getMapped_OLD(Map<String, SoftReference<I>> map,
                                               String key,
                                               Function<String, Identity> getter,
                                               Function<Identity, I> factory,
                                               Function<Identity, String> extraKeySupplier) {
    if (!DUMMY_ID.equals(key)) {
      do {
        SoftReference<I> ref = map.compute(key, (existingKey, existingRef) -> {
          if (existingRef != null && existingRef.get() != null) {
            return existingRef;
          }
          // LOG.info("> Get social identity: " + id);
          SoftReference<I> newRef;
          Identity socId = getter.apply(existingKey);
          if (socId != null) {
            newRef = new SoftReference<I>(factory.apply(socId));
            map.put(extraKeySupplier.apply(socId), newRef); // map by Name/ID
          } else {
            newRef = null;
            LOG.warn("Cannot find social identity: " + existingKey);
          }
          // LOG.info("< Get social identity: " + id);
          return newRef;
        });
        if (ref != null) {
          // It may be null still here: after checking existingRef.get() !=
          // null in compute() above the GC may decide to clear the value.
          I id = ref.get();
          if (id != null) {
            return id;
          } // otherwise repeat the loop to read from identityManager
        } else {
          break; // it will return null
        }
      } while (true);
    }
    return null;
  }

  private <I extends Identity> I getMapped(Map<String, SoftReference<I>> map,
                                           String key,
                                           Function<String, I> reader,
                                           Function<I, String> extraKeySupplier) {
    if (!DUMMY_ID.equals(key)) {
      do {
        SoftReference<I> ref = map.compute(key, (existingKey, existingRef) -> {
          if (existingRef != null && existingRef.get() != null) {
            return existingRef;
          }
          // LOG.info("> Get social identity: " + id);
          SoftReference<I> newRef;
          I inst = reader.apply(existingKey);
          if (inst != null) {
            newRef = new SoftReference<I>(inst);
            // also map by Name/ID
            map.put(extraKeySupplier.apply(inst), new SoftReference<I>(inst));
          } else {
            newRef = null;
          }
          // LOG.info("< Get social identity: " + id);
          return newRef;
        });
        if (ref != null) {
          // It may be null still here: after checking existingRef.get() !=
          // null in compute() above the GC may decide to clear the value.
          I id = ref.get();
          if (id != null) {
            return id;
          } // otherwise repeat the loop to read from identityManager
        } else {
          break; // it will return null
        }
      } while (true);
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
    SoftReference<SpaceIdentity> ref = new SoftReference<SpaceIdentity>(id);
    spaceIdentities.put(id.getId(), ref);
    spaceIdentities.put(id.getRemoteId(), ref);
    return id;
  }

  protected UserIdentity userIdentity(Identity socId) {
    String id = socId.getId();
    String userName = socId.getRemoteId();
    LOG.info(">> Get social profile: {} ( {} ) <<", id, userName);
    Profile socProfile = identityManager.getProfile(socId);
    if (socProfile != null) {
      return new UserIdentity(id, userName, socProfile.getGender(), findFocus(socProfile.getPosition()));
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

  @Deprecated // TODO not used
  protected Set<String> getActiveUsers() {
    return focusGroups.compute(ACTIVE_USERS, (gid, current) -> {
      final long now = System.currentTimeMillis();
      if (current != null && currentUsersUpdated.get() > now - UserInfluencers.ACTIVITY_EXPIRATION_L0) {
        return current;
      }
      currentUsersUpdated.set(now);
      return identityStorage.getActiveUsers(new ActiveIdentityFilter(UserInfluencers.INFLUENCE_DAYS_RANGE));
    });
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

}
