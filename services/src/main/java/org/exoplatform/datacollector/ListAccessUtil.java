package org.exoplatform.datacollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ListAccessUtil {

  /** Logger */
  private static final Log LOG        = ExoLogger.getExoLogger(ListAccessUtil.class);

  public static final int  BATCH_SIZE = 100;

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
   * instance.
   *
   * @param list the real-time list of activities
   * @param sinceTime the since time
   * @return the iterator
   * @throws Exception the exception
   */
  public static Iterator<ExoSocialActivity> loadActivitiesListIterator(RealtimeListAccess<ExoSocialActivity> list,
                                                                       long sinceTime) throws Exception {
    Iterator<ExoSocialActivity> res = new Iterator<ExoSocialActivity>() {

      Iterator<ExoSocialActivity> nextBatch;

      ExoSocialActivity           next, prev;

      @ExoTransactional // for large feed long fetching
      private void loadNextBatch() {
        List<ExoSocialActivity> nextList;
        try {
          if (prev != null) {
            nextList = list.loadNewer(prev, BATCH_SIZE);
          } else {
            nextList = list.loadNewer(sinceTime, BATCH_SIZE);
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
          next = nextBatch.next();
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

}
