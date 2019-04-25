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
import static org.exoplatform.datacollector.SocialInfluencers.ACTIVITY_PARTICIPANTS_TOP;

import java.io.File;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
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
import org.exoplatform.datacollector.social.SocialStorage;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.datacollector.storage.FileStorage.ModelFile;
import org.exoplatform.datacollector.storage.FileStorage.UserDir;
import org.exoplatform.datacollector.storage.StorageException;
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
import org.exoplatform.social.common.RealtimeListAccess;
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

  public static final int        MIN_FEED_SIZE        = 1;

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

  public static final String     WORKER_TIMER_NAME    = "datacollector-worker-timer";

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

  protected static final Integer RECENT_LOGINS_COUNT  = 100;                                                                                                                                                                      // 3600000L;

  protected static final String  MAIN_BUCKET_PREFIX   = "main";

  protected static final String  AUTOSTART_MODE_PARAM = "autostart";

  protected static final String  TRAIN_PERIOD_PARAM   = "train-period";

  protected static final String  DEVELOPING_PARAM     = "developing";

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
   * processes them in mainQueue order. Collects a new dataset and sends it to
   * TrainingService, if the user's model needs training. Schedules next
   * execution in TRAIN_PERIOD ms.
   */
  public class BucketWorker extends ContainerCommand {
    private AtomicBoolean runWorker = new AtomicBoolean(true);

    public BucketWorker() {
      super();
    }

    public BucketWorker(String containerName) {
      super(containerName);
    }

    /**
     * Atomically poll a next job from queue and put to main loop.
     *
     * @return the process job
     */
    ProcessJob nextJob() {
      ProcessJob job;
      // don't modify the queue here - peek only the head!
      while ((job = mainQueue.peek()) != null) {
        ProcessJob candidateJob = job;
        // Atomically poll from the queue and put in to main loop
        if (candidateJob == mainLoop.compute(candidateJob.userName, (userName, currentJob) -> {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Getting user from queue: {}", userName);
          }
          // poll (modify) the queue only here below!
          if (currentJob == null || currentJob == candidateJob) {
            // User not already processing by main loop
            ModelEntity lastModel = lastModel(userName);
            if (modelNeedsProcessing(lastModel, candidateJob.timestamp)) {
              return mainQueue.poll();
            } else {
              // Model doesn't need to be (re)trained, return what main loop
              // already has (null or another job)
              if (LOG.isDebugEnabled()) {
                LOG.debug("User {} doesn't need processing or in failed state", userName);
              }
              mainQueue.poll(); // and skip this one
              return currentJob;
            }
          } else {
            // if another job already processing the user, we let it do
            if (LOG.isDebugEnabled()) {
              LOG.debug("User {} already processing ", userName);
            }
            mainQueue.poll(); // and skip this one
            return currentJob;
          }
        })) {
          break;
        }
      }
      return job;
    }

    @Override
    protected void execute(ExoContainer exoContainer) {
      if (runMainLoop.get() && runWorker.get()) {
        final String bucketName = nextMainBucketName();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Bucket processing time: {}", bucketName);
        }
        // Work in main loop
        ProcessJob job;
        while (runMainLoop.get() && runWorker.get() && (job = nextJob()) != null) {
          job.bucketName.set(bucketName);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Start {} model processing", job.userName);
          }
          submitTraining(job);
        }
        // Schedule next worker
        if (runMainLoop.get()) {
          // Start another worker in the same container as this one
          BucketWorker nextWorker = new BucketWorker(this.containerName);
          if (currentWorker.compareAndSet(this, nextWorker)) {
            timer.schedule(nextWorker, trainingPeriod);
          }
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Bucket worked done: {}", bucketName);
        }
      }
    }

    /**
     * Checks if the model needs (re)training.
     * 
     * @param model to be checked
     * @param checkpoint checkpoint date, a difference between this date and
     *          given model activation will be tested to decide for training if
     *          the model has new or ready status.
     * @return true, if model needs (re)training, otherwise - false
     */
    boolean modelNeedsProcessing(ModelEntity model, Date checkpoint) {
      if (model == null || model.getStatus() == Status.RETRY) {
        return true;
      }
      if (model.getStatus() == Status.FAILED_DATASET || model.getStatus() == Status.FAILED_TRAINING
          || model.getStatus() == Status.PROCESSING) {
        // If PROCESSING here it means that processing was interrupted and the
        // status not updated in the DB - it should be started manually by
        // startUser().
        // TODO we may need a logic to restart PROCESSING automatically (cleanup
        // the files and do like NEW?)
        return false;
      }
      // NEW will have null activated date, READY will have date of its training
      return model.getActivated() == null || Math.abs(checkpoint.getTime() - model.getActivated().getTime()) > trainingPeriod;
    }

    /**
     * {@inheritDoc}
     */
    public boolean cancel() {
      runWorker.set(false);
      return super.cancel();
    }
  }

  class ProcessJob {
    final String                  userName;

    final Date                    timestamp;

    final int                     hashCode;

    final AtomicReference<String> bucketName = new AtomicReference<>();

    final AtomicLong              sinceTime  = new AtomicLong();

    ProcessJob(String userName, Date timstamp) {
      super();
      this.userName = userName;
      this.timestamp = timstamp;

      int hk = 7;
      hk = hk * 31 + userName.hashCode();
      hk = hk * 31 + timestamp.hashCode();
      this.hashCode = hk;
    }

    ProcessJob(String userName, Date timstamp, String bucketName, Long sinceTime) {
      this(userName, timstamp);
      this.bucketName.set(bucketName);
      this.sinceTime.set(sinceTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (obj != null) {
        if (this == obj) {
          return true;
        }
        if (getClass().isAssignableFrom(obj.getClass())) {
          ProcessJob other = getClass().cast(obj);
          return this.userName.equals(other.userName) && this.timestamp.equals(other.timestamp);
        }
      }
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new StringBuilder(userName).append(" -- ").append(timestamp).toString();
    }
  }

  /**
   * The StartWorker is used to perform first processing based on login history.
   * Registers loginListener
   */
  public class StartWorker extends BucketWorker {

    StartWorker() {
      super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute(ExoContainer exoContainer) {
      try {
        // Last RECENT_LOGINS_COUNT logins
        List<LastLoginBean> lastLogins = loginHistory.getLastLogins(RECENT_LOGINS_COUNT, EMPTY_STRING);
        // Reverse w/o Google library
        Collections.reverse(lastLogins);
        Set<String> focusUsers = getFocusUsers();
        lastLogins.stream().forEach(login -> {
          if (focusUsers != null) {
            if (focusUsers.contains(login.getUserId())) {
              queueInMainLoop(login.getUserId(), new Date(login.getLastLogin()));
            }
          } else {
            queueInMainLoop(login.getUserId(), new Date(login.getLastLogin()));
          }
        });
      } catch (Exception e) {
        LOG.error("Cannot get last users login", e);
      }
      // process the jobs
      super.execute(exoContainer);
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
          admins.add(userId);
        }
        if (m.getGroupId().equals(EMPLOYEES_GROUPID)) {
          employees.add(userId);
        }
        if (focusGroups.keySet().contains(m.getGroupId())) {
          focusGroups.get(m.getGroupId()).add(userId);
        }
      }
    }

    public void postDelete(Membership m) throws Exception {
      String userId = getUserIdentityByName(m.getUserName()).getId();
      if (m.getGroupId().equals(userACL.getAdminGroups())) {
        admins.remove(userId);
      }
      if (m.getGroupId().equals(EMPLOYEES_GROUPID)) {
        employees.remove(userId);
      }
      if (focusGroups.keySet().contains(m.getGroupId())) {
        focusGroups.get(m.getGroupId()).remove(userId);
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
      if (runMainLoop.get()) {
        String userId = event.getData().getIdentity().getUserId();
        Set<String> focusUsers = getFocusUsers();
        if (focusUsers != null) {
          if (focusUsers.contains(userId)) {
            queueInMainLoop(userId);
          }
        } else {
          queueInMainLoop(userId);
        }
      }
    }
  }

  protected final FileStorage                               fileStorage;

  protected final IdentityManager                           identityManager;

  protected final IdentityStorage                           identityStorage;

  protected final ActivityManager                           activityManager;

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

  protected final SocialStorage                             socialStorage;

  // TODO clean this map time-from-time to do not consume the RAM
  protected final Map<String, SoftReference<UserIdentity>>  userIdentities      = new HashMap<>();

  protected final Map<String, SoftReference<SpaceIdentity>> spaceIdentities     = new HashMap<>();

  protected final ExecutorService                           workers;

  protected final AtomicLong                                currentUsersUpdated = new AtomicLong(0);

  /**
   * Jobs queued for processing (dataset collecting and training).
   */
  protected final ConcurrentLinkedQueue<ProcessJob>         mainQueue           = new ConcurrentLinkedQueue<>();

  /**
   * Jobs currently applying for processing (dataset collecting and training).
   */
  protected final ConcurrentHashMap<String, ProcessJob>     mainLoop            = new ConcurrentHashMap<>();

  /** Calculated user influencers (aka runtime cache). */
  // TODO use exo cache with eviction instead of a map
  protected final ConcurrentHashMap<String, UserSnapshot>   userSnapshots       = new ConcurrentHashMap<>();

  protected AtomicInteger                                   currentBucketIndex  = new AtomicInteger(-1);

  /** The timer for scheduled tasks execution */
  protected final Timer                                     timer               = new Timer(WORKER_TIMER_NAME);

  /** The configred run main loop. */
  protected final boolean                                   configRunMainLoop;

  /** The flag is used to stop the main loop **/
  protected AtomicBoolean                                   runMainLoop         = new AtomicBoolean(false);

  /**
   * The training period. It's the delay between processing buckets. It's 180min
   * by default.
   */
  protected Long                                            trainingPeriod      = 180 * 60000L;

  /**
   * The current worker that executes or is going to execute the main loop of
   * collecting/training.
   */
  protected AtomicReference<BucketWorker>                   currentWorker       = new AtomicReference<>();

  /** The is developing. */
  protected Boolean                                         isDeveloping;

  protected final Map<String, Set<String>>                  focusGroups         = new ConcurrentHashMap<>();

  protected Set<String>                                     admins;

  protected Set<String>                                     employees;

  /**
   * Instantiates a new data collector service.
   *
   * @param fileStorage the file storage
   * @param socialStorage the social storage
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param identityManager the identity manager
   * @param identityStorage the identity storage
   * @param activityManager the activity manager
   * @param spaceService the space service
   * @param loginHistory the login history
   * @param listenerService the listener service
   * @param userACL the user ACL
   * @param postStorage the post storage
   * @param commentStorage the comment storage
   * @param likeStorage the like storage
   * @param mentionStorage the mention storage
   * @param identityProfileStorage the identity profile storage
   * @param trainingService the training service
   * @param initParams the init params
   */
  public SocialDataCollectorService(FileStorage fileStorage,
                                    SocialStorage socialStorage,
                                    RepositoryService jcrService,
                                    SessionProviderService sessionProviders,
                                    NodeHierarchyCreator hierarchyCreator,
                                    OrganizationService organization,
                                    IdentityManager identityManager,
                                    IdentityStorage identityStorage,
                                    ActivityManager activityManager,
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
    this.socialStorage = socialStorage;
    this.identityManager = identityManager;
    this.identityStorage = identityStorage;
    this.activityManager = activityManager;
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

    ValueParam developingParam = initParams.getValueParam(DEVELOPING_PARAM);
    if (developingParam != null) {
      this.isDeveloping = Boolean.valueOf(developingParam.getValue());
    }

    ValueParam autostartParam = initParams.getValueParam(AUTOSTART_MODE_PARAM);
    ValueParam trainPeroidParam = initParams.getValueParam(TRAIN_PERIOD_PARAM);
    boolean runMainLoop;
    try {
      String pval = autostartParam.getValue();
      runMainLoop = pval != null ? Boolean.valueOf(pval) : false;
    } catch (Exception e) {
      LOG.warn("Cannot set manual mode to " + autostartParam.getValue() + ", using auto mode by default", e);
      runMainLoop = true;
    }
    this.configRunMainLoop = runMainLoop;
    try {
      String pval = trainPeroidParam.getValue();
      this.trainingPeriod = Integer.parseInt(pval) * 60000L;
    } catch (Exception e) {
      LOG.warn("Cannot set the train period to " + trainPeroidParam.getValue() + ", using " + this.trainingPeriod / 60000L
          + " min. by default", e);
    }
  }

  /**
   * Adds the focus group plugin.
   *
   * @param plugin the plugin
   */
  public void addFocusGroupPlugin(ComponentPlugin plugin) {
    if (FocusGroupPlugin.class.isAssignableFrom(plugin.getClass())) {
      FocusGroupPlugin fgplugin = FocusGroupPlugin.class.cast(plugin);
      if (fgplugin.getGroupId() != null && fgplugin.getGroupId().length() > 0) {
        this.focusGroups.put(fgplugin.getGroupId(), Collections.emptySet());
        LOG.info("Added focus group to main loop {}", fgplugin.getGroupId());
      } else {
        LOG.warn("Skipped empty focus group plugin");
      }
    } else {
      LOG.error("The focus group plugin is not an instance of {}", FocusGroupPlugin.class.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    LOG.info("Start " + this.getClass().getSimpleName() + "...");
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      // Pre-read constant things
      this.admins = getGroupMemberIds(userACL.getAdminGroups(), "manager", "member");
      // this.focusGroups.put(userACL.getAdminGroups(), admins);

      this.employees = getGroupMemberIds(EMPLOYEES_GROUPID);
      // this.focusGroups.put(EMPLOYEES_GROUPID, employees);

      // Fill focus group members
      for (Iterator<Map.Entry<String, Set<String>>> fgiter = this.focusGroups.entrySet().iterator(); fgiter.hasNext();) {
        Map.Entry<String, Set<String>> fge = fgiter.next();
        Set<String> members = getGroupMemberIds(fge.getKey());
        if (members != null) {
          fge.setValue(Collections.synchronizedSet(members));
        } else {
          fgiter.remove();
          LOG.warn("Cannot find focus group members {}, the group will be skipped", fge.getKey());
        }
      }

      try {
        this.organization.addListenerPlugin(new MembershipListener());
      } catch (Exception e) {
        LOG.error("Cannot add the MembershipListener", e);
      }

      this.listenerService.addListener(new LoginListener());

      if (this.configRunMainLoop) {
        if (startMainLoop()) {
          LOG.info("Started " + this.getClass().getSimpleName() + " in automatic mode");
        } else {
          LOG.warn("Started " + this.getClass().getSimpleName() + " with already enabled automatic mode");
        }
      } else {
        LOG.info("Started " + this.getClass().getSimpleName() + " in manual mode");
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    LOG.info("Stop " + this.getClass().getSimpleName() + "...");
    // stop mail loop
    stopMainLoop();
  }

  /**
   * Start immediate collecting of training dataset and optionally invoke the
   * user model training whit it.<br>
   * Take in account: bucket name cannot starts with
   * {@value #MAIN_BUCKET_PREFIX} if train=<code>false</code>, and if
   * train=<code>true</code> and it starts with or <code>null</code>, then a
   * next available bucket name will be used with this prefix.<br>
   * Value of sinceTime may be ignored in case if train=<code>true</code>
   *
   * @param userName userName
   * @param bucketName bucket name
   * @param sinceTime the since time to collector a dataset
   * @param train will train the model if <code>true</code>
   * @return the string with actually assigned bucket name for resulting dataset
   *         and model (if train=<code>true</code>)
   * @throws StorageException the storage exception if bucket name is not
   *           acceptable, it cannot starts with {@value #MAIN_BUCKET_PREFIX} if
   *           train=<code>false</code>
   */
  public String startUser(String userName, String bucketName, long sinceTime, boolean train) throws StorageException {
    // It should be possible to start training for an user immediately (but
    // similar like in addUser(String)).
    if (train) {
      // First check in runtime:
      if (mainQueue.removeIf(job -> userName.equals(job.userName) ? true : false)) {
        // removed from the queue - we'll start processing immediately below
        LOG.info("Removed previously queued user training from main loop {}", userName);
      }
      ProcessJob job = mainLoop.get(userName);
      if (job != null) {
        // already processing - do nothing
        // TODO for more precise check we may want to check equality of
        // sinceTime, and then we will need a lock to cancel/wait until
        // existing job complete and then start a new one (with another
        // sinceTime).
        LOG.info("User {} training already processing in {}. Do nothing.", userName, job.bucketName.get());
        return job.bucketName.get();
      }
      // Any state in persistence acceptable, a training will act accordingly.
//      ModelEntity lastModel = lastModel(userName); // TODO cleanup
//      if (lastModel != null) {
//        if (lastModel.getStatus() == Status.NEW) {
//          // This user already issued for processing (but may be with different
//          // bucket):
//          // * if stays in this state for an undefined reason (not handled
//          // failure or service stopping) - submit it immediately.
//          // * if already scheduled in the main loop - then remove from there
//          // and submit it immediately,
//        } else if (lastModel.getStatus() == Status.PROCESSING) {
//          // This user may already processing - ensure it is and if yes
//          // - do nothing, otherwise submit it immediately.
//        } // Otherwise it's READY, ARCHIVED or failed states - we submit a NEW
//          // one
//      } // otherwise, user has no model - initialize what required
      // Safely choose a bucket name to avoid disk conflicts
      if (bucketName == null || bucketName.startsWith(MAIN_BUCKET_PREFIX)) {
        bucketName = nextMainBucketName();
      }
      submitTraining(new ProcessJob(userName, new Date(), bucketName, sinceTime));
    } else {
      // Main loop bucket prefix cannot be used as it may create conflicts on
      // disk with already processing or activated models.
      if (bucketName.startsWith(MAIN_BUCKET_PREFIX)) {
        throw new StorageException("Bucket name cannot start with " + MAIN_BUCKET_PREFIX);
      }
      // TODO handle conflicts on same bucket started several times (first
      // collector/training not yet done, but another called to start) - use
      // file locks to deal with this (no model in DB here).
      submitCollector(userName, bucketName, sinceTime);
    }
    return bucketName;
  }

  /**
   * Collect user activities into prediction (inferring) dataset.
   *
   * @param id the social identity
   * @param activities the activities to collect into dataset
   * @return the dataset for prediction
   */
  public ModelFile collectActivities(Identity id, Collection<ExoSocialActivity> activities) {
    ModelEntity model = lastModel(id.getRemoteId());
    if (model != null && model.getStatus() == Status.READY) {
      String modelPath = model.getModelFile();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Collecting predicting dataset for model {}[{}] into {}", model.getName(), model.getVersion(), modelPath);
      }
      UserDir userDir = fileStorage.findUserModel(modelPath);
      if (userDir != null) {
        ModelFile predictDataset = userDir.getPredictDataset();
        try {
          // TODO care about concurrent prediction by the same user (file lock)
          // touch to have a new predict file
          if (predictDataset.exists()) {
            predictDataset.delete();
          }
          predictDataset.createNewFile();
          try (PrintWriter writer = new PrintWriter(predictDataset)) {
            UserSnapshot user = getUserSnapshot(predictDataset.getUserDir(), id);
            if (user != null) {
              writeUserActivities(user, activities.iterator(), writer, false);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Saved predicting dataset for model {}[{}] into {}",
                          model.getName(),
                          model.getVersion(),
                          predictDataset.getModelPath());
              }
              return predictDataset;
            } else {
              LOG.warn("User snapshot not found for model {}[{}]", model.getName(), model.getVersion());
            }
          }
        } catch (Exception e) {
          LOG.error("Cannot collect predicting dataset for model {}[{}]", model.getName(), model.getVersion(), e);
          predictDataset.delete();
        }
      } else {
        LOG.warn("User model storage not found for model {}[{}]", model.getName(), model.getVersion());
      }
    } else {
      LOG.warn("User model not found or not ready for {}", id.getRemoteId());
    }
    return null;
  }

  /**
   * Collect user activities into an inferring dataset (for prediction).
   *
   * @param id the social identity
   * @param activityIds the activity IDs
   * @return the dataset for prediction
   */
  public ModelFile collectActivitiesByIds(Identity id, List<String> activityIds) {
    List<ExoSocialActivity> activities = activityManager.getActivities(activityIds);
    return collectActivities(id, activities);
  }

  /**
   * Stops the main loop (collecting and training).
   *
   * @return <code>true</code>, if successfully stopped, <code>false</code> if
   *         already stopped
   */
  public boolean stopMainLoop() {
    if (runMainLoop.getAndSet(false)) {
      BucketWorker worker = currentWorker.getAndSet(null);
      if (worker != null) {
        worker.cancel();
        timer.purge();
        LOG.info("Main loop (collecting and training user models) has been signaled for stopping");
        return true;
      }
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
    BucketWorker newWorker = new StartWorker();
    BucketWorker prevWorker = currentWorker.getAndSet(newWorker);
    if (prevWorker != null) {
      LOG.warn("Main loop just marked as started but already run a worker {}, signaling this worker for stopping", prevWorker);
      // FYI this may be not enough in general, as the worker invoke
      // submitUserCollector() for users and they might continue to
      // collect/train even after stopping this worker.
      prevWorker.cancel();
    }
    workers.submit(newWorker);
    return true;
  }

  /**
   * Adds user to the training queue (main loop) for processing in the next
   * bucket processing time. This methods produces the same effect as user login
   * into the portal.
   *
   * @param userName the user to add
   */
  public void addUser(String userName) {
    if (!mainQueue.stream().anyMatch(job -> job.userName.equals(userName)) && !mainLoop.containsKey(userName)) {
//      ModelEntity lastModel = lastModel(userName);
      // If model exists, valid and is not being processed right now
      // TODO this chck and setting to RETRY has not sense after the rework
//      if (lastModel != null && lastModel.getStatus() != Status.NEW && lastModel.getStatus() != Status.PROCESSING) {
//        lastModel.setStatus(Status.RETRY);
//        trainingService.update(lastModel);
//        LOG.info("Model {}[{}] got status RETRY.", lastModel.getName(), lastModel.getVersion());
//      }
      queueInMainLoop(userName);
    } else if (LOG.isDebugEnabled()) {
      // else, user already queued or processing in the main loop
      LOG.debug("User already in training queue {}", userName);
    }
  }

  /**
   * Collect all users activities into files bucket, each file will have name of
   * an user with <code>.csv</code> extension. If such folder already exists, it
   * will overwrite the files that match found users. In case of error during
   * the work, all partial results will be deleted.
   *
   * @param bucketName the bucketRecords name, can be <code>null</code> then a
   *          timestamped name will be created
   * @return the saved bucket folder path
   * @throws Exception the exception
   */
  @Deprecated // TODO method has no practical value for production, but may be
              // used in development
  public String collectAllUsersFeeds(String bucketName) throws Exception {
    // Go through all users in the organization and swap their datasets
    // into separate data stream, then feed them to the Training Service

    long sinceTime = System.currentTimeMillis() - SocialInfluencers.FEED_MILLIS_RANGE;

    Iterator<Identity> idIter = loadListIterator(identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                                              new ProfileFilter(),
                                                                                              true));
    while (idIter.hasNext() && !Thread.currentThread().isInterrupted()) {
      final UserIdentity id = cacheUserIdentity(userIdentity(idIter.next()));
      submitCollector(id.getRemoteId(), bucketName, sinceTime);
    }
    File bucketDir = fileStorage.getBucketDir(bucketName);
    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("Saving of dataset interrupted for bucketRecords: {}", bucketName);
      // Clean the bucketRecords files
      try {
        Arrays.asList(bucketDir.listFiles()).stream().forEach(f -> f.delete());
        bucketDir.delete();
      } catch (Exception e) {
        LOG.error("Error removing canceled bucketRecords folder: {}", bucketName, e);
      }
      return null;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Saved dataset successfully into bucketRecords folder: {}", bucketDir.getAbsolutePath());
    }
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
   * Tells if collector service is working in Developing mode.
   *
   * @return <code>true</code>, if developing model enabled in the runtime
   */
  public boolean isDeveloping() {
    if (isDeveloping == null) {
      isDeveloping = Boolean.valueOf(PropertyManager.getProperty(new StringBuilder("exo.datacollector.").append(DEVELOPING_PARAM)
                                                                                                        .toString()));
    }
    return isDeveloping.booleanValue();
  }

  /**
   * Collect user feed activities into training dataset.
   *
   * @param userName the user name
   * @param bucketName the bucket name
   * @param sinceTime the since time
   * @return the dataset file
   * @throws DatasetException the dataset exception
   */
  protected ModelFile collectUserFeed(String userName, String bucketName, long sinceTime) throws DatasetException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Saving user dataset into: {}/{}", bucketName, userName);
    }
    final UserIdentity id = getUserIdentityByName(userName);
    if (id != null) {
      // XXX We cannot obtain an iterator over all the feed right here as it may
      // become disconnected from the DB by connection pool if getUserSnapshot()
      // will take too long (about a hour), it will lead to Tx rollback then SQL
      // exceptions.
      // Thus we first check if a feed contains something, then get the user
      // snapshot and only then request the feed via into an iterator.
      // TODO socialStorage
      final RealtimeListAccess<ExoSocialActivity> feed = activityManager.getActivityFeedWithListAccess(id);
      final int feedSize = feed.getNumberOfNewer(sinceTime);
      if (feedSize >= MIN_FEED_SIZE) {
        UserDir userDir = fileStorage.getBucketDir(bucketName).getUserDir(id.getRemoteId());
        ModelFile datasetFile = userDir.getTrainingDataset();
        // The feed activities (for DEBUG).
        try {
          // touch to have a new training file
          if (datasetFile.exists()) {
            datasetFile.delete();
          }
          datasetFile.createNewFile();
          try (PrintWriter writer = new PrintWriter(datasetFile)) {
            // Prepare an user state snapshot, it should not be modified until
            // another collecting - this can be looooong operation, a hour+ if
            // calculate influencers from user activities.
            // Activities iterator with its DB session may get in timeout, thus
            // it should be transactional where fetch from the storage.
            UserSnapshot user = getUserSnapshot(userDir, id);

            // TODO should this method be ExoTransactional, but with getting the
            // feed iterator inside it - in single tx?
            // socialStorage
            writeUserActivities(user, loadActivitiesListIterator(feed, sinceTime, feedSize, true), writer, true);
            // save/cache user snapshot only after successful write
            saveUserSnapshot(userDir, user);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Saved user dataset into bucket file: {}", userDir.getModelPath());
            }
            return datasetFile;
          }
        } catch (Exception e) { // this includes PersistenceException
          datasetFile.delete();
          throw new DatasetException("Cannot collect user activities for " + userName + ". Caused by: " + e.getMessage(), e);
        }
      } else if (LOG.isDebugEnabled()) {
        LOG.debug("Has no enough activities to collect for {}", userName);
      }
    } else {
      LOG.warn("User social identity not found for {}", userName);
    }
    return null;
  }

  /**
   * Collects user's all activities in separate worker and train a model using
   * it.
   *
   * @param job the internal process object describing the training
   */
  protected void submitTraining(ProcessJob job) {
    workers.submit(new ContainerCommand() {
      @Override
      protected void execute(ExoContainer exoContainer) {
        try {
          // TODO Also take in account model (scripts) updates, in this case we
          // need train from scratch, not incrementally.
          ModelEntity readyModel = null;
          ModelEntity currentModel = lastModel(job.userName/* , Status.RETRY */);
          if (currentModel != null) {
            if (currentModel.getStatus() == Status.READY) {
              readyModel = currentModel;
              currentModel = null;
            } else if (currentModel.getStatus() == Status.RETRY) {
              // If we have a model to RETRY - use it with new sinceTime
            } else if (currentModel.getStatus() == Status.NEW || currentModel.getStatus() == Status.PROCESSING) {
              // XXX Delete any of it and add a NEW one.
              // NEW can come from main loop (but it's unexpected),
              // manual startUser() also can bring it to here.
              // PROCESSING should not get to here from main loop,
              // see in modelNeedsProcessing(), but can from manual invocation
              // in startUser() if processing was interrupted previously and
              // started manually.
              LOG.warn("Already {} model in main loop {}[{}] - deleting it",
                       currentModel.getStatus(),
                       currentModel.getName(),
                       currentModel.getVersion());
              trainingService.delete(currentModel);
              currentModel = null;
            } else {
              // else FAILED* should not get to here from main
              // loop, see in modelNeedsProcessing(), but can from manual
              // invocation in startUser() - add a NEW one.
              LOG.warn("{} model in main loop {}[{}]",
                       currentModel.getStatus(),
                       currentModel.getName(),
                       currentModel.getVersion());
              currentModel = null;
            }
          }
          boolean incrementally = false;
          if (job.sinceTime.get() <= 0) {
            // Get last READY model, and count sinceTime from that last ready to
            // make incremental training over it.
            if (readyModel == null) {
              readyModel = lastModel(job.userName, Status.READY);
            }
            if (readyModel != null) {
              // TODO do we need subtract a time gap (e.g. 100ms) from the
              // created time to catch in calculation activities that happened
              // while the previous model collected and saved?
              // In fact we create a model first, then collect and save, thus it
              // may be actually desired to add a time gap (best if of actually
              // spent time by previous model).
              // In general case it's question of how critical if some
              // activities will be lost or repeatedly taken in account by the
              // model and influencers?
              incrementally = true;
              job.sinceTime.set(readyModel.getCreated().getTime());
            } else {
              job.sinceTime.set(System.currentTimeMillis() - SocialInfluencers.FEED_MILLIS_RANGE);
            }
          }
          if (currentModel == null) {
            // We add a new model first with status NEW, it will show that
            // processing already issued but not yet started.
            currentModel = trainingService.addModel(job.userName, null);
          }
          try {
            ModelFile dataset = collectUserFeed(job.userName, job.bucketName.get(), job.sinceTime.get());
            if (dataset != null) {
              // Start process of the model training
              // TODO as collecting may take time: do we need (re)new JPA
              // session/transaction or re-get the model object?
              trainingService.trainModel(currentModel, dataset, incrementally);
            }
          } catch (DatasetException e) {
            if (currentModel.getStatus() == Status.RETRY) {
              currentModel.setStatus(Status.FAILED_DATASET);
              trainingService.update(currentModel);
              LOG.error("Cannot collect dataset {} for model {}[{}].",
                        job.bucketName.get() + "/" + job.userName,
                        currentModel.getName(),
                        currentModel.getVersion(),
                        e);
            } else {
              currentModel.setStatus(Status.RETRY);
              trainingService.update(currentModel);
              queueInMainLoop(job.userName);
              if (isDeveloping) {
                LOG.warn("Dataset {} collecting failed and will be retried for model {}[{}]. Error:",
                         job.bucketName.get() + "/" + job.userName,
                         currentModel.getName(),
                         currentModel.getVersion(),
                         e);
              } else {
                LOG.warn("Dataset {} collecting failed and will be retried for model {}[{}]. Error: {}",
                         job.bucketName.get() + "/" + job.userName,
                         currentModel.getName(),
                         currentModel.getVersion(),
                         e.getMessage());
              }
            }
          }
        } finally {
          // Clean main loop from this job, but not same user processing by
          // other job (if any)
          mainLoop.remove(job.userName, job);
        }
      }
    });
  }

  /**
   * Collects user's all activities in separate worker.
   *
   * @param userName the user name
   * @param bucket the bucket
   * @param sinceTime the time since which collect the dataset
   */
  protected void submitCollector(String userName, String bucket, long sinceTime) {
    workers.submit(new ContainerCommand() {
      @Override
      protected void execute(ExoContainer exoContainer) {
        try {
          collectUserFeed(userName, bucket, sinceTime);
        } catch (DatasetException e) {
          LOG.error("User activities collector failed for {}", bucket + "/" + userName, e);
        }
      }
    });
  }

  /**
   * Write user activities to given writer.
   *
   * @param user the user state snapshot
   * @param activities the activities to process
   * @param out the writer where spool the user activities dataset
   * @param forTraining if <code>true</code> then collect dataset for training
   */
  protected void writeUserActivities(UserSnapshot user,
                                     Iterator<ExoSocialActivity> activities,
                                     PrintWriter out,
                                     boolean forTraining) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> Writing user activities for {}", user.getIdentity().getRemoteId());
    }
    out.println(activityHeader(forTraining));

    // load identity's activities and collect its data
    while (activities.hasNext() && !Thread.currentThread().isInterrupted()) {
      ExoSocialActivity activity = activities.next();
      out.println(activityLine(user, activity, forTraining));
    }

    if (Thread.currentThread().isInterrupted()) {
      LOG.warn("< Interrupted collector of user activities for {}", user.getIdentity().getRemoteId());
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("< Wrote user activities for {}", user.getIdentity().getRemoteId());
      }
    }
  }

  protected UserSnapshot getUserSnapshot(UserDir userDir, Identity id) throws Exception {
    // TODO use exo cache with eviction instead of a map
    // TODO in fact, a snapshot not connected to a bucket, and if not found
    // for some bucket, we can share a single one calculated for user at some
    // moment. But this shared may outdate - how to manage this and should we?
    return userSnapshots.computeIfAbsent(userDir.getModelPath(), sid -> {
      try {
        UserSnapshot user = readUserSnapshot(id, userDir);
        if (isDeveloping()) {
          // Saving user snapshot to the user model directory: we want see an
          // initial one used by training and of last prediction
          File userFile = userDir.childFile("training_user_snapshot.txt");
          if (userFile.exists()) {
            // If training exists, then it's prediction assumed
            // TODO care about concurrent predictions
            userFile = userDir.childFile("predict_user_snapshot.txt");
          }
          Files.write(userFile.toPath(), user.dump().getBytes());
        }
        return user;
      } catch (Exception e) {
        sneakyThrow(e);
        return null; // it will not happen
      }
    });
  }

  @ExoTransactional // for users with lot of data that can read long
  private UserSnapshot readUserSnapshot(Identity id, UserDir userDir) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> Reading user snapshot for {}", id.getRemoteId());
    }
    UserSnapshot user;
    File userFile = userDir.getUserSnapshot();
    if (userFile.exists()) {
      // Read a snapshot from user dir
      user = new UserSnapshot(id);
      try {
        user.fromJson(new String(Files.readAllBytes(userFile.toPath())));
      } catch (SnapshotException e) {
        LOG.error("Cannot load user snapshot", e);
        user = null;
      }
    } else {
      user = null;
    }
    if (user == null) {
      user = createUserSnapshot(id);
      user.initInfluencers();
      // XXX sinceTime here may not be the same as for activities until
      // we'll make UserInfluecners incrementally maintained and load them
      // from DB
      initializeUserSnapshot(user, System.currentTimeMillis() - SocialInfluencers.FEED_MILLIS_RANGE);
      // Save the snapshot in user dir
      Files.write(userFile.toPath(), user.toJson().getBytes());
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("< Read user snapshot for {}", id.getRemoteId());
    }
    return user;
  }

  /**
   * Save user snapshot.
   *
   * @param buketName the buket name
   * @param user the user
   */
  // @ExoTransactional // TODO for saving in DB
  private void saveUserSnapshot(UserDir userDir, UserSnapshot user) {
    String userId = user.getIdentity().getRemoteId();
    if (LOG.isDebugEnabled()) {
      LOG.debug("> Saving user snapshot for {}", userId);
    }
    // TODO:
    // 1) save in user storage (DB/filesystem)
    // 2) cache in runtime
    userSnapshots.put(userDir.getModelPath(), user);
    if (LOG.isDebugEnabled()) {
      LOG.debug("< Saved user snapshot for {}", userId);
    }
  }

  /**
   * Initialize user snapshot.
   *
   * @param user the user
   * @param sinceTime the since time
   */
  @ExoTransactional // we need tx for users with large history
  protected void initializeUserSnapshot(UserSnapshot user, long sinceTime) {
    String userId = user.getIdentity().getId();
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> Initializing user snapshot for {}", user.getIdentity().getRemoteId());
    }

    SocialInfluencers influencers = user.getInfluencers();

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
    if (LOG.isDebugEnabled()) {
      LOG.debug("<< Initialized user snapshot for {}", user.getIdentity().getRemoteId());
    }
  }

  protected UserSnapshot createUserSnapshot(Identity id) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Creating user snapshot for {}", id.getRemoteId());
    }
    Set<String> connsSnapshot = Collections.unmodifiableSet(socialStorage.getConnections(id));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loaded all connections of {}: {}", id.getRemoteId(), connsSnapshot.size());
    }

    Collection<Space> spaces = loadListAll(spaceService.getMemberSpaces(id.getRemoteId()));
    Map<String, SpaceSnapshot> spacesSnapshot =
                                              Collections.unmodifiableMap(spaces.stream()
                                                                                .collect(Collectors.toMap(s -> s.getId(),
                                                                                                          s -> new SpaceSnapshot(s),
                                                                                                          (s1, s2) -> s1)));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loaded all spaces of {}: {}", id.getRemoteId(), spaces.size());
    }

    UserSnapshot userSnapshot = new UserSnapshot(id, connsSnapshot, spacesSnapshot);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Created user snapshot for {}", id.getRemoteId());
    }
    return userSnapshot;
  }

  protected String activityHeader(boolean forTraining) {
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
    if (forTraining) {
      aline.append("rank");
    } else {
      aline.deleteCharAt(aline.length() - 1);
    }
    return aline.toString();
  }

  protected String activityLine(UserSnapshot user, ExoSocialActivity activity, boolean forTraining) {
    ActivityRank rank = null;
    if (forTraining) {
      rank = new ActivityRank();
    }
    StringBuilder aline = new StringBuilder();
    // Activity identification & type ID
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
    // Reactivity has different logic for training and prediction:
    // * when training, we want know actual user reaction on the post;
    // * when predicting, a reaction is unknown and reactivity is full (1).
    double reactivity = forTraining ? reactivity = user.getInfluencers().getPostReactivity(activity.getId()) : 1;
    aline.append(reactivity).append(',');

    final String myId = user.getIdentity().getId();
    final Collection<String> myConns = user.getConnections();

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
    if (forTraining) {
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
    if (forTraining) {
      rank.participatedByInfluencer(posterWeight);
      if (new HashSet<String>(Arrays.asList(activity.getLikeIdentityIds())).contains(posterId)) {
        rank.likedByInfluencer(posterWeight);
      }
    }

    // Find top 5 participants in this activity, we need not less than 5!
    int added = 0;
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
      if (forTraining) {
        if (p.isConversed > 0) {
          rank.participatedByInfluencer(pweight);
        } else if (p.isFavored > 0) {
          rank.likedByInfluencer(pweight);
        }
      }
      added++;
    }
    // if not enough influencers for the user - add dummy to reach the top
    for (; added < ACTIVITY_PARTICIPANTS_TOP; added++) {
      // participantN_id
      aline.append(DUMMY_ID).append(',');
      // participantN_conversed
      aline.append("0,");
      // participantN_favored
      aline.append("0,");
      // participantN_gender: encoded
      encGender(aline, null).append(',');
      // participantN_is_employee
      aline.append("0,");
      // participantN_is_lead
      aline.append("0,");
      // participantN_is_in_connections
      aline.append("0,");
      // participantN_focus_*: job position as team membership encoded
      encFocus(aline, null).append(',');
      // participantN_influence
      aline.append("0.0,");
    }

    if (forTraining) {
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
      return employees.contains(identity.getId());
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
      // + (SocialInfluencers.DAY_LENGTH_MILLIS *
      // SocialInfluencers.REACTIVITY_DAYS_RANGE);

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
        Iterator<String> citer = user.getConnections().iterator();
        while (citer.hasNext() && top.size() < topLength) {
          String cid = citer.next();
          // TODO if (wasUserLoggedin(cid, activityScopeTimeBegin,
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
          Iterator<String> aiter = admins.iterator();
          while (aiter.hasNext() && top.size() < topLength) {
            top.computeIfAbsent(aiter.next(), p -> new ActivityParticipant(p, false, false));
          }
        }
        // * TODO if not enough in admins - then guess random from "similar"
        // users in the organization,
        // * if no users - add "blank" user with id=0.
//        if (top.size() < topLength) {
//          int needAdd = topLength - top.size();
//          do {
//            top.computeIfAbsent(new StringBuilder(DUMMY_ID).append('-').append(needAdd).toString(),
//                                p -> new ActivityParticipant(p, false, false));
//            needAdd--;
//          } while (needAdd > 0);
//        }
      }
    }
    return Collections.unmodifiableCollection(top.values());
  }

  protected Collection<String> userIds(String... names) {
    Set<String> res = new LinkedHashSet<>();
    for (String name : names) {
      UserIdentity socId = getUserIdentityByName(name);
      if (socId == null) {
        LOG.warn("Cannot find social identity (userIds): {}", name);
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
      LOG.warn("Error reading login history for " + userId, e);
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
      LOG.warn("Error reading group members for " + groupId + " of types: " + membershipTypes, e);
      return null;
    }
  }

  protected SpaceIdentity getSpaceIdentityByName(String spaceName) {
    return getMapped(spaceIdentities, spaceName, name -> {
      Identity socId = socialIdentity(SpaceIdentityProvider.NAME, name);
      if (socId != null) {
        return spaceIdentity(socId);
      } else {
        LOG.warn("Cannot find space's social identity by name: " + name);
        return null;
      }
    }, socId -> socId.getId());
  }

  protected SpaceIdentity getSpaceIdentityById(String spaceId) {
    return getMapped(spaceIdentities, spaceId, id -> {
      Identity socId = socialIdentity(id);
      if (socId != null) {
        return spaceIdentity(socId);
      } else {
        LOG.warn("Cannot find space's social identity by ID: " + id);
        return null;
      }
    }, socId -> socId.getRemoteId());
  }

  protected UserIdentity getUserIdentityByName(String userName) {
    return getMapped(userIdentities, userName, name -> {
      // Reading from the storage, if not exists - getting from Social API
      IdentityProfileEntity persisted = identityProfileStorage.findByName(userName);
      if (persisted != null) {
        return userIdentity(persisted);
      }

      Identity socId = socialIdentity(OrganizationIdentityProvider.NAME, name);
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
        LOG.warn("Cannot find user's social identity by name: " + name);
        return null;
      }
    }, socId -> socId.getId());
  }

  protected UserIdentity getUserIdentityById(String identityId) {
    return getMapped(userIdentities, identityId, id -> {
      // Reading from the storage, if not exists - getting from Social API
      IdentityProfileEntity persisted = identityProfileStorage.findById(identityId);
      if (persisted != null) {
        return userIdentity(persisted);
      }

      Identity socId = socialIdentity(id);
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
        LOG.warn("Cannot find user's social identity by ID: " + id);
        return null;
      }
    }, socId -> socId.getId());
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
          // LOG.debug("> Get social identity: " + id);
          SoftReference<I> newRef;
          I inst = reader.apply(existingKey);
          if (inst != null) {
            newRef = new SoftReference<I>(inst);
            // also map by Name/ID
            map.put(extraKeySupplier.apply(inst), new SoftReference<I>(inst));
          } else {
            newRef = null;
          }
          // LOG.debug("< Get social identity: " + id);
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
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> Get social profile: {} ( {} ) <<", id, userName);
    }
    Profile socProfile = socialProfile(socId);
    if (socProfile != null) {
      return new UserIdentity(id, userName, socProfile.getGender(), findFocus(socProfile.getPosition()));
    } else {
      // TODO add listener to check when user will fill his profile and
      // then update the mapping
      LOG.warn("Cannot find profile of social identity: {} ( {} )", id, userName);
      return new UserIdentity(id, userName, null, null);
    }
  }

  protected UserIdentity userIdentity(IdentityProfileEntity profileEntity) {
    String gender = null;
    if (profileEntity.getContext() != null) {
      gender = Arrays.asList(profileEntity.getContext().split(";"))
                     .stream()
                     .filter(key -> key.contains("gender:"))
                     .findFirst()
                     .orElse(null);
      if (gender != null) {
        String[] genderParam = gender.split(":");
        gender = genderParam.length > 1 ? genderParam[1] : null;
      }
    }
    return new UserIdentity(profileEntity.getId(), profileEntity.getName(), gender, profileEntity.getFocus());
  }

  @ExoTransactional
  protected SpaceIdentity spaceIdentity(Identity socId) {
    return new SpaceIdentity(socId.getId(), socId.getRemoteId());
  }

  @ExoTransactional
  protected Identity socialIdentity(String porviderId, String name) {
    // We do in ExoTransactional to avoid org-service/hibernate tx problems
    return identityManager.getOrCreateIdentity(porviderId, name, false);
  }

  @ExoTransactional
  protected Identity socialIdentity(String id) {
    // We do in ExoTransactional to avoid org-service/hibernate tx problems
    return identityManager.getIdentity(id, false);
  }

  @ExoTransactional
  protected Profile socialProfile(Identity id) {
    // We do in ExoTransactional to avoid org-service/hibernate tx problems
    return identityManager.getProfile(id);
  }

  @Deprecated // TODO not used
  protected Set<String> getActiveUsers() {
//    return focusGroups.compute(ACTIVE_USERS, (gid, current) -> {
//      final long now = System.currentTimeMillis();
//      if (current != null && currentUsersUpdated.get() > now - SocialInfluencers.ACTIVITY_EXPIRATION_L0) {
//        return current;
//      }
//      currentUsersUpdated.set(now);
//      return identityStorage.getActiveUsers(new ActiveIdentityFilter(SocialInfluencers.INFLUENCE_DAYS_RANGE));
//    });
    return null;
  }

  /**
   * Gets all focus users if some were added via
   * {@link #addFocusGroupPlugin(ComponentPlugin)}. Otherwise return
   * <code>null</code>.
   *
   * @return the focus users or <code>null</code>
   */
  protected Set<String> getFocusUsers() {
    if (focusGroups.size() > 0) {
      return focusGroups.values().stream().collect(LinkedHashSet::new, Set::addAll, Set::addAll);
    }
    return null;
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
    // Minimum set of threads will be maintained online even idle, other
    // inactive will be stopped in two minutes.
    final int cpus = Runtime.getRuntime().availableProcessors();
    int poolThreads = cpus / 4;
    poolThreads = poolThreads < MIN_THREADS ? MIN_THREADS : poolThreads;
    int maxThreads = Math.round(cpus * 1f * maxFactor);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_MAX_THREADS ? MIN_MAX_THREADS : maxThreads;
    int queueSize = cpus * queueFactor;
    queueSize = queueSize < queueFactor ? queueFactor : queueSize;
    LOG.info("Creating thread executor {}* for {}..{} threads, queue size {}",
             threadNamePrefix,
             poolThreads,
             maxThreads,
             queueSize);
    return new ThreadPoolExecutor(poolThreads,
                                  maxThreads,
                                  THREAD_IDLE_TIME,
                                  TimeUnit.SECONDS,
                                  new LinkedBlockingQueue<Runnable>(queueSize),
                                  new WorkerThreadFactory(threadNamePrefix),
                                  new ThreadPoolExecutor.CallerRunsPolicy());
  }

  protected void queueInMainLoop(String userName) {
    queueInMainLoop(userName, new Date());
  }

  protected void queueInMainLoop(String userName, Date checkpoint) {
    // Remove redundant: same user but lesser date
    for (Iterator<ProcessJob> qiter = mainQueue.iterator(); qiter.hasNext();) {
      ProcessJob job = qiter.next();
      if (job.userName.equals(userName) && job.timestamp.before(checkpoint)) {
        qiter.remove();
      }
    }
    // Add new job
    mainQueue.add(new ProcessJob(userName, checkpoint));
    if (LOG.isDebugEnabled()) {
      LOG.debug("User {} has beed added to the main loop for collecting and training", userName);
    }
  }

  protected ModelEntity lastModel(String userName) {
    try {
      return trainingService.getLastModel(userName);
    } catch (PersistenceException e) {
      LOG.error("Error reading last model for {}", userName, e);
    }
    return null;
  }

  protected ModelEntity lastModel(String userName, Status status) {
    try {
      return trainingService.getLastModel(userName, status);
    } catch (PersistenceException e) {
      LOG.error("Error reading last model for {}:{}", userName, status, e);
    }
    return null;
  }

  protected String nextMainBucketName() {
    return new StringBuilder(MAIN_BUCKET_PREFIX).append(currentBucketIndex.incrementAndGet()).toString();
  }
}
