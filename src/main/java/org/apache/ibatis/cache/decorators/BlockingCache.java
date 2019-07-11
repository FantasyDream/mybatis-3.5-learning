/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Simple blocking decorator
 *
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  /**
   * 阻塞超时时长
   */
  private long timeout;
  /**
   * 被装饰的cache对象
   */
  private final Cache delegate;
  /**
   * 每个key都有对应的ReentrantLock对象
   */
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void putObject(Object key, Object value) {
    try {
      // 缓存
      delegate.putObject(key, value);
    } finally {
      // 因为该方法会在getObject方法后调用,当getObject获得的值为null时,需要调用该方法缓存,所以锁一直保存到了现在,避免发生读写不一致的情况
      releaseLock(key);
    }
  }

  @Override
  public Object getObject(Object key) {
    // 获得该key对应的锁
    acquireLock(key);
    // 查询key
    Object value = delegate.getObject(key);
    // 若缓存有key对应的缓存项,释放锁,否则继续持有
    if (value != null) {
      releaseLock(key);
    }
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    // 尽管这个方法的名称是removeObject,但他仅仅只是释放锁而已
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  private ReentrantLock getLockForKey(Object key) {
    // 又是computeIfAbsent这个方法...看来这个方法确实省了不少行数
    // 同样,含义是key有对应的锁的话,则取出,否则生成一个新的再取出
    return locks.computeIfAbsent(key, k -> new ReentrantLock());
  }

  private void acquireLock(Object key) {
    // 获取key对应的锁对象
    Lock lock = getLockForKey(key);
    if (timeout > 0) {
      try {
        // 在timeout毫秒内一直尝试获取该锁
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        // 若未获取到,则抛出异常
        if (!acquired) {
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 获取锁,不带超时时长
      lock.lock();
    }
  }

  private void releaseLock(Object key) {
    ReentrantLock lock = locks.get(key);
    // 判断锁是否被当前线程持有
    if (lock.isHeldByCurrentThread()) {
      // 释放锁
      lock.unlock();
    }
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
