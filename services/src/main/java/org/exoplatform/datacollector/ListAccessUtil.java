/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.enterprise.context.Dependent;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ListAccessUtil {

  /** Logger */
  private static final Log LOG              = ExoLogger.getExoLogger(ListAccessUtil.class);

  public static final int  BATCH_SIZE       = 100;

  /**
   * Load all given access list to a {@link List} instance. Use carefully for
   * larger data or prefer using {@link #loadListIterator(ListAccess)} instead.
   *
   * @param <E> the element type
   * @param list the {@link ListAccess} instance
   * @return the {@link List} instance
   * @throws Exception the exception
   */
  public static <E> List<E> loadListAll(ListAccess<E> list) throws Exception {
    List<E> res = new ArrayList<>();
    int size = list.getSize();
    if (size == 0) {
      // here we assume that load() below will raise an error on end-of-data
      size = 1;
    }
    int batches = size / BATCH_SIZE;
    if (batches == 0) {
      batches = 1;
    }
    int batchIndex = 0;
    for (int fi = 0; fi < batches; fi++) {
      try {
        int batchSize = size - batchIndex;
        if (batchSize > 0) {
          if (batchSize >= BATCH_SIZE) {
            batchSize = BATCH_SIZE;
          }
          for (E e : list.load(batchIndex, batchSize)) {
            res.add(e);
          }
          batchIndex += batchSize;
        } else {
          // reached actual end-of-data
          break;
        }
      } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        // faced with actual end-of-data
        LOG.warn("Unexpected index/size error in the batch: {}", e);
        break;
      }
    }
    return res;
  }

  /**
   * Wrap given access list into an {@link Iterator} instance.
   *
   * @param <E> the element type
   * @param list the list
   * @return the iterator
   * @throws Exception the exception
   */
  public static <E> Iterator<E> loadListIterator(ListAccess<E> list) throws Exception {
    final int size;
    int listSize = list.getSize();
    if (listSize == 0) {
      // here we assume that load() below will raise an error on end-of-data
      size = 1;
    } else {
      size = listSize;
    }

    Iterator<E> res = new Iterator<E>() {

      int batchIndex = 0;

      int index      = 0;

      E[] nextBatch;

      E   next;

      private void loadNextBatch() {
        try {
          int batchSize = size - batchIndex;
          if (batchSize > 0) {
            if (batchSize >= BATCH_SIZE) {
              batchSize = BATCH_SIZE;
            }
            nextBatch = list.load(batchIndex, batchSize);
            batchIndex += batchSize;
          } else {
            // reached actual end-of-data
            nextBatch = null;
          }
        } catch (IndexOutOfBoundsException e) {
          // faced with actual end-of-data, or already empty
          nextBatch = null;
        } catch (IllegalArgumentException e) {
          LOG.warn("Unexpected index/size error during loading access list", e);
          nextBatch = null;
        } catch (Exception e) {
          // Here can be DB or network error
          LOG.error("Unexpected error during loading access list", e);
          nextBatch = null;
        }
      }

      private void loadNext() {
        if (nextBatch == null) {
          loadNextBatch();
          index = 0;
        }
        if (nextBatch != null && nextBatch.length > 0) {
          next = nextBatch[index++];
          if (nextBatch.length == index) {
            nextBatch = null;
          }
        } else {
          next = null;
          nextBatch = null;
          index = 0;
        }
      }

      @Override
      public boolean hasNext() {
        if (next == null) {
          loadNext();
        }
        return next != null;
      }

      @Override
      public E next() {
        if (hasNext()) {
          E theNext = next;
          next = null;
          return theNext;
        }
        throw new NoSuchElementException("No more elements");
      }
    };
    return res;
  }

  /**
   * Wrap given access real-time list of activities into an {@link Iterator}
   * instance.<br>
   * WARN: this iterator may return same activity several times (as it may
   * appear in the original list's loadNewer() method). This iterator fetching
   * also may run infinite or long if activities batch will repeat already
   * fetched "newer" signature (see in CachedActivityStorage how keys
   * implemented).
   *
   * @param list the real-time list of activities
   * @param sinceTime the since time
   * @param skipSame if <code>true</code> the iterator will skip same ID
   *          activities, thus each returned activity will appear once
   * @return the iterator
   * @throws Exception the exception
   */
  @Deprecated
  public static Iterator<ExoSocialActivity> loadActivitiesListIterator(RealtimeListAccess<ExoSocialActivity> list,
                                                                       long sinceTime,
                                                                       boolean skipSame) throws Exception {
    Iterator<ExoSocialActivity> res = new Iterator<ExoSocialActivity>() {

      Set<String>                 fetchedIds = skipSame ? new HashSet<>() : null;

      Iterator<ExoSocialActivity> nextBatch;

      ExoSocialActivity           next;

      ExoSocialActivity           prev;

      @ExoTransactional // for large feed long fetching
      private void loadNextBatch() {
        List<ExoSocialActivity> nextList;
        try {
          if (prev != null) {
            nextList = list.loadNewer(prev, BATCH_SIZE);
          } else {
            nextList = list.loadNewer(sinceTime, BATCH_SIZE);
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("> Load next batch: {} items", nextList.size());
          }
        } catch (IllegalArgumentException e) {
          LOG.warn("Unexpected time/length error during real-time loading access list:", e);
          nextList = Collections.emptyList();
        } catch (Exception e) {
          // Here can be DB or network error
          LOG.error("Unexpected error during loading real-time access list:", e);
          nextList = Collections.emptyList();
        }
        if (nextList.isEmpty()) {
          // reached actual end-of-data
          nextBatch = null;
        } else {
          nextBatch = nextList.iterator();
        }
      }

      private void loadNext() {
        if (nextBatch == null) {
          loadNextBatch();
        }
        if (nextBatch != null && nextBatch.hasNext()) {
          do {
            next = nextBatch.next();
            if (next != null) {
              if (LOG.isDebugEnabled()) {
                LOG.debug(">> Load next activity from the iterator: {}, updated: {} ", next.getId(), next.getUpdated());
                if (fetchedIds != null && fetchedIds.contains(next.getId())) {
                  LOG.debug(">>> Activity repeats in the iterator: {} and it will be skipped", next.getId());
                }
              }
            }
          } while (fetchedIds != null && next != null && !fetchedIds.add(next.getId()) && nextBatch.hasNext());
          if (!nextBatch.hasNext()) {
            nextBatch = null;
          }
        } else {
          next = null;
          nextBatch = null;
        }
      }

      @Override
      public boolean hasNext() {
        if (next == null) {
          loadNext();
        }
        return next != null;
      }

      @Override
      public ExoSocialActivity next() {
        if (hasNext()) {
          prev = next;
          next = null;
          return prev;
        }
        throw new NoSuchElementException("No more elements");
      }
    };
    return res;
  }

  /**
   * Wrap given access real-time list of activities into an {@link Iterator}
   * instance. List will be fetched in batches of maximum {@value #BATCH_SIZE}
   * items.
   *
   * @param list the real-time list of activities
   * @param limit the limit how many items to fetch from the given list
   * @param skipSame if <code>true</code> the iterator will skip same ID
   *          activities, thus each returned activity will appear once
   * @return the iterator
   * @throws Exception the exception
   */
  @Dependent // TODO not used
  public static Iterator<ExoSocialActivity> loadActivitiesListIterator(RealtimeListAccess<ExoSocialActivity> list,
                                                                       int limit,
                                                                       boolean skipSame) throws Exception {
    Iterator<ExoSocialActivity> res = new Iterator<ExoSocialActivity>() {

      Set<String>                 fetchedIds = skipSame ? new HashSet<>() : null;

      Iterator<ExoSocialActivity> nextBatch;

      ExoSocialActivity           next;

      int                         batchIndex = 0;

      @ExoTransactional // for large feed long fetching
      private void loadNextBatch() {
        try {
          int batchSize = limit - batchIndex;
          if (batchSize > 0) {
            if (batchSize >= BATCH_SIZE) {
              batchSize = BATCH_SIZE;
            }
            List<ExoSocialActivity> nextList = list.loadAsList(batchIndex, batchSize);
            nextBatch = nextList.iterator();
            if (LOG.isDebugEnabled()) {
              LOG.debug("> Load next batch: {} items, loaded {}", nextList.size(), batchIndex);
            }
            batchIndex += nextList.size();
          } else {
            // reached actual end-of-data
            nextBatch = null;
          }
        } catch (IllegalArgumentException e) {
          LOG.warn("Unexpected item index/limit during real-time loading access list:", e);
          nextBatch = null;
        } catch (Exception e) {
          // Here can be DB or network error
          LOG.error("Unexpected error during loading real-time access list:", e);
          nextBatch = null;
        }
      }

      private void loadNext() {
        if (nextBatch == null) {
          loadNextBatch();
        }
        if (nextBatch != null && nextBatch.hasNext()) {
          do {
            next = nextBatch.next();
            if (LOG.isDebugEnabled()) {
              if (next != null) {
                LOG.debug(">> Load next activity from the iterator: {}, updated: {} ", next.getId(), next.getUpdated());
                if (fetchedIds != null && fetchedIds.contains(next.getId())) {
                  LOG.debug(">>> Activity repeats in the iterator: {} and it will be skipped", next.getId());
                }
              } else {
                LOG.debug("<< Load NULL activity from the iterator");
              }
            }
          } while (fetchedIds != null && next != null && !fetchedIds.add(next.getId()) && nextBatch.hasNext());
          if (!nextBatch.hasNext()) {
            nextBatch = null;
          }
        } else {
          next = null;
          nextBatch = null;
        }
      }

      @Override
      public boolean hasNext() {
        if (next == null) {
          loadNext();
        }
        return next != null;
      }

      @Override
      public ExoSocialActivity next() {
        if (hasNext()) {
          ExoSocialActivity res = next;
          next = null;
          return res;
        }
        throw new NoSuchElementException("No more elements");
      }
    };
    return res;
  }

  /**
   * Wrap given access real-time list of activities into an {@link Iterator}
   * instance. Iterator will load only newer items starting from the given since
   * time and up to a given limit.
   *
   * @param list the real-time list of activities
   * @param sinceTime the since time from which to load the activities
   * @param limit the limit how many items to fetch from the given list
   * @param skipSame if <code>true</code> the iterator will skip same ID
   *          activities, thus each returned activity will appear once
   * @return the iterator
   * @throws Exception the exception
   */
  public static Iterator<ExoSocialActivity> loadActivitiesListIterator(RealtimeListAccess<ExoSocialActivity> list,
                                                                       long sinceTime,
                                                                       int limit,
                                                                       boolean skipSame) throws Exception {
    Iterator<ExoSocialActivity> res = new Iterator<ExoSocialActivity>() {

      Set<String>                 fetchedIds = skipSame ? new HashSet<>() : null;

      Iterator<ExoSocialActivity> iter;

      ExoSocialActivity           next;

      //@ExoTransactional // for large feed long fetching
      private void loadList() {
        List<ExoSocialActivity> theList;
        try {
          theList = list.loadNewer(sinceTime, limit);
          if (LOG.isDebugEnabled()) {
            LOG.debug("> Load iterator with {} items", theList.size());
          }
        } catch (IllegalArgumentException e) {
          LOG.warn("Unexpected sinceTime/limit during real-time loading access list:", e);
          theList = Collections.emptyList();
        } catch (Exception e) {
          // Here can be DB or network error
          LOG.error("Unexpected error during loading real-time access list:", e);
          theList = Collections.emptyList();
        }
        iter = theList.iterator();
      }

      private void loadNext() {
        if (iter == null) {
          loadList();
        }
        if (iter.hasNext()) {
          do {
            next = iter.next();
            if (LOG.isDebugEnabled()) {
              if (next != null) {
                //LOG.debug(">> Load next activity from the iterator: {}, updated: {} ", next.getId(), next.getUpdated());
                if (fetchedIds != null && fetchedIds.contains(next.getId())) {
                  LOG.debug(">>> Activity repeats in the iterator: {} and it will be skipped", next.getId());
                }
              } else {
                LOG.debug("<< Load NULL activity from the iterator");
              }
            }
          } while (fetchedIds != null && next != null && !fetchedIds.add(next.getId()) && iter.hasNext());
        } else {
          next = null;
        }
      }

      @Override
      public boolean hasNext() {
        if (next == null) {
          loadNext();
        }
        return next != null;
      }

      @Override
      public ExoSocialActivity next() {
        if (hasNext()) {
          ExoSocialActivity res = next;
          next = null;
          return res;
        }
        throw new NoSuchElementException("No more elements");
      }
    };
    return res;
  }

}
