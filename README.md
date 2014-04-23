nbbmq
=====

Non-blocking bounded queue implementation for Akka mailbox where it is ok to drop old messages when the queue is full. Internally the queue is backed by an array based ring buffer where if queue is full old messages are overridden by new messages and head points to the next available oldest message in the queue.

When to use it?
===============
Typically queues are mostly empty or mostly full. This queue will work well on the later situations. Since the implementation is backed by an array it will always take up
and fix amount of memory. You really don't want to use it as a default mailbox for actor. It makes more sense to have a handful top level router/event processor actors to have this mailbox so that you can control the event flow for rest of application. 

Take a look at the NonBlockingBoundedMessageQueueSpec and Main class for its usage. 

License
========

This software is licensed under the Apache 2 license.





