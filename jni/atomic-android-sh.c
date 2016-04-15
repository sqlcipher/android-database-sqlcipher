/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cutils/atomic.h>
#ifdef HAVE_WIN32_THREADS
#include <windows.h>
#else
#include <sched.h>
#endif

/*
 * Note :
 *
 * (1) SuperH does not have CMPXCHG.  It has only TAS for atomic
 *     operations.  It does not seem a good idea to implement CMPXCHG,
 *     with TAS.  So, we choose to implemnt these operations with
 *     posix mutexes.  Please be sure that this might cause performance
 *     problem for Android-SH. Using LL/SC instructions supported in SH-X3,
 *     best performnace would be realized.
 *
 * (2) Mutex initialization problem happens, which is commented for
 *     ARM implementation, in this file above.
 *     We follow the fact that the initializer for mutex is a simple zero
 *     value.
 *
 * (3) These operations are NOT safe for SMP, as there is no currently
 *     no definition for a memory barrier operation.
 */

#include <pthread.h>

#define  SWAP_LOCK_COUNT  32U
static pthread_mutex_t  _swap_locks[SWAP_LOCK_COUNT];

#define  SWAP_LOCK(addr)   \
   &_swap_locks[((unsigned)(void*)(addr) >> 3U) % SWAP_LOCK_COUNT]


int32_t android_atomic_acquire_load(volatile const int32_t* addr)
{
    return *addr;
}

int32_t android_atomic_release_load(volatile const int32_t* addr)
{
    return *addr;
}

void android_atomic_acquire_store(int32_t value, volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, value, addr));
}

void android_atomic_release_store(int32_t value, volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, value, addr));
}

int32_t android_atomic_inc(volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, oldValue+1, addr));
    return oldValue;
}

int32_t android_atomic_dec(volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, oldValue-1, addr));
    return oldValue;
}

int32_t android_atomic_add(int32_t value, volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, oldValue+value, addr));
    return oldValue;
}

int32_t android_atomic_and(int32_t value, volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, oldValue&value, addr));
    return oldValue;
}

int32_t android_atomic_or(int32_t value, volatile int32_t* addr) {
    int32_t oldValue;
    do {
        oldValue = *addr;
    } while (android_atomic_release_cas(oldValue, oldValue|value, addr));
    return oldValue;
}

int android_atomic_acquire_cmpxchg(int32_t oldvalue, int32_t newvalue,
                           volatile int32_t* addr) {
    return android_atomic_release_cmpxchg(oldvalue, newvalue, addr);
}

int android_atomic_release_cmpxchg(int32_t oldvalue, int32_t newvalue,
                           volatile int32_t* addr) {
    int result;
    pthread_mutex_t*  lock = SWAP_LOCK(addr);

    pthread_mutex_lock(lock);

    if (*addr == oldvalue) {
        *addr  = newvalue;
        result = 0;
    } else {
        result = 1;
    }
    pthread_mutex_unlock(lock);
    return result;
}

