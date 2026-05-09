package io.github.chehsunliu.itx.contract.queue;

public interface MessageQueueFactory {
    MessageQueue createControlStandardQueue();

    MessageQueue createControlPremiumQueue();

    MessageQueue createComputeStandardQueue();

    MessageQueue createComputePremiumQueue();
}
