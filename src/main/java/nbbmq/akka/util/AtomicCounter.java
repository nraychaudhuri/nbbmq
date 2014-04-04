/*
 * Copyright 2012 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nbbmq.akka.util;

import sun.misc.Unsafe;

/**
 * Pad out a cacheline to the left of a value to prevent false sharing.
 */
class LhsPadding
{
    protected volatile long p1, p2, p3, p4, p5, p6, p7;
}

/**
 * Value for the counter that is expected to be padded.
 */
class Value extends LhsPadding
{
    protected volatile long value;
}

/**
 * Pad out a cacheline to the right of a value to prevent false sharing.
 */
class RhsPadding extends Value
{
    protected volatile long p9, p10, p11, p12, p13, p14, p15;
}

/**
 * AtomicCounter to be used for concurrent communication of counters between threads.
 *
 * The value is padded to prevent false sharing in systems with a cache line less than or equal to 64 bytes.
 */
public class AtomicCounter extends RhsPadding
{
    /**
     * Value to which {@link AtomicCounter} will be initialised unless otherwise specified.
     */
    public static final long INITIAL_VALUE = 0;

    private static final Unsafe unsafe;
    private static final long valueOffset;

    static
    {
        try
        {
            unsafe = akka.util.Unsafe.instance;
            valueOffset = unsafe.objectFieldOffset(Value.class.getDeclaredField("value"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a sequence with value set to {@link AtomicCounter#INITIAL_VALUE}
     */
    public AtomicCounter()
    {
        setOrdered(INITIAL_VALUE);
    }

    /**
     * Create a AtomicCounter by providing an initial value.
     *
     * @param initialValue to be used for initialisation.
     */
    public AtomicCounter(final long initialValue)
    {
        setOrdered(initialValue);
    }

    /**
     * Get the current value of the AtomicCounter.  This will always be the latest value from memory using the same semantics
     * of a <code>volatile</code> field.
     *
     * @return the current value.
     */
    public long get()
    {
        return value;
    }

    /**
     * Set the value for the AtomicCounter without using a volatile store.
     *
     * This method can give improved performance on x86 by not waiting on the store buffer to drain.
     *
     * This method is effectively a software fence.
     *
     * @param value to which the AtomicCounter should be set.
     */
    public void setOrdered(final long value)
    {
        unsafe.putOrderedLong(this, valueOffset, value);
    }

    /**
     * Delta the AtomicCounter without using a volatile store.  To decrement the AtomicCounter pass in a negative value.
     *
     * This method can give improved performance on x86 by not waiting on the store buffer to drain.
     *
     * This method is effectively a software fence.
     *
     * @param delta by which the sequence should be incremented.
     */
    public void addOrdered(final long delta)
    {
        setOrdered(get() + delta);
    }

    /**
     * Set the value of the AtomicCounter using the same semantics as a volatile store.
     *
     * @param value to which the sequence should be set.
     */
    public void setVolatile(final long value)
    {
        this.value = value;
    }

    /**
     * Perform an atomic CAS operation on the AtomicCounter value.
     *
     * @param expectedValue for the operation to succeed.
     * @param updateValue to be set on success.
     * @return true if the operation is successful otherwise false.
     */
    public boolean compareAndSet(final long expectedValue, final long updateValue)
    {
        return unsafe.compareAndSwapLong(this, valueOffset, expectedValue, updateValue);
    }

    /**
     * Perform an atomic increment by 1 and get the new value.
     *
     * @return the value after a successful increment.
     */
    public long incrementAndGet()
    {
        return addAndGet(1L);
    }

    /**
     * Perform an atomic increment by 1 and return the value before increment.
     *
     * @return the value prior to the successful increment operation.
     */
    public long getAndIncrement()
    {
        return getAndAdd(1L);
    }

    /**
     * Perform an atomic increment by a delta and return the value before increment.
     *
     * @param delta to be applied to the current AtomicCounter.
     * @return the value prior to the successful increment operation.
     */
    public long getAndAdd(final long delta)
    {
        long currentValue;

        do
        {
            currentValue = get();
        }
        while (!compareAndSet(currentValue, currentValue + delta));

        return currentValue;
    }

    /**
     * Perform an atomic increment by a delta and return the value after increment.
     *
     * @param delta to be applied to the current AtomicCounter.
     * @return the value after to the successful increment operation.
     */
    public long addAndGet(final long delta)
    {
        long currentValue;
        long newValue;

        do
        {
            currentValue = get();
            newValue = currentValue + delta;
        }
        while (!compareAndSet(currentValue, newValue));

        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return Long.toString(get());
    }
}
