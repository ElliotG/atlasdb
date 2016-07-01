/**
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.paxos;

import static com.google.common.collect.ImmutableList.copyOf;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of a paxos proposer than can be a designated proposer (leader) and designated
 * learner (informer).
 *
 * @author rullman
 */
public class PaxosProposerImpl implements PaxosProposer {
    private static final Logger log = LoggerFactory.getLogger(PaxosProposerImpl.class);

    public static PaxosProposer newProposer(PaxosLearner localLearner,
                                            List<PaxosAcceptor> allAcceptors,
                                            List<PaxosLearner> allLearners,
                                            int quorumSize,
                                            ExecutorService executor) {
        return new PaxosProposerImpl(
                localLearner,
                allAcceptors,
                allLearners,
                quorumSize,
                UUID.randomUUID().toString(),
                executor);
    }

    final ImmutableList<PaxosAcceptor> allAcceptors;
    final ImmutableList<PaxosLearner> allLearners;
    final PaxosLearner localLearner;
    final int quorumSize;
    final String uuid;
    final AtomicLong proposalNum;

    private final ExecutorService executor;

    private PaxosProposerImpl(PaxosLearner localLearner,
                              List<PaxosAcceptor> acceptors,
                              List<PaxosLearner> learners,
                              int quorumSize,
                              String uuid,
                              ExecutorService executor) {
        Preconditions.checkState(
                quorumSize > acceptors.size() / 2,
                "quorum size needs to be at least the majority of acceptors");
        this.localLearner = localLearner;
        this.allAcceptors = copyOf(acceptors);
        this.allLearners = copyOf(learners);
        this.quorumSize = quorumSize;
        this.uuid = uuid;
        this.proposalNum = new AtomicLong();
        this.executor = executor;
    }

    @Override
    public byte[] propose(final PaxosKey key, @Nullable byte[] bytes) throws PaxosRoundFailureException {
        long seq = key.seq();
        final PaxosProposalId proposalID = new PaxosProposalId(proposalNum.incrementAndGet(), uuid);
        PaxosValue toPropose = new PaxosValue(key, bytes);

        final PaxosValue finalValue = prepareAndPromise(seq, proposalID, toPropose);
        collectAcceptancesForProposal(seq, proposalID, finalValue);
        broadcastLearnedValue(seq, finalValue);

        return finalValue.getData();
    }

    /**
     * Executes phase one of paxos (see
     * http://en.wikipedia.org/wiki/Paxos_(computer_science)#Basic_Paxos)
     *
     * @param seq the number identifying this instance of paxos
     * @param pid the id of the proposal currently being considered
     * @param value the default proposal value if no member of the quorum has already
     *        accepted an offer
     * @return the value accepted by the quorum
     * @throws PaxosRoundFailureException if quorum cannot be reached in this phase
     */
    private PaxosValue prepareAndPromise(final long seq, final PaxosProposalId pid, PaxosValue value)
            throws PaxosRoundFailureException {
        List<PaxosPromise> receivedPromises = PaxosQuorumChecker.<PaxosAcceptor, PaxosPromise> collectQuorumResponses(
                allAcceptors,
                new Function<PaxosAcceptor, PaxosPromise>() {
                    @Override
                    @Nullable
                    public PaxosPromise apply(@Nullable PaxosAcceptor acceptor) {
                        return acceptor.prepare(seq, pid);
                    }
                },
                quorumSize,
                executor,
                PaxosQuorumChecker.DEFAULT_REMOTE_REQUESTS_TIMEOUT_IN_SECONDS);

        if (!PaxosQuorumChecker.hasQuorum(receivedPromises, quorumSize)) {
            // update proposal number on failure
            for (PaxosPromise promise : receivedPromises) {
                while (true) {
                    long curNum = proposalNum.get();
                    if (promise.promisedId.number <= curNum) {
                        break;
                    }
                    if (proposalNum.compareAndSet(curNum, promise.promisedId.number)) {
                        break;
                    }
                }
            }
            throw new PaxosRoundFailureException("failed to acquire quorum in paxos phase one");
        }

        PaxosPromise greatestPromise = Collections.max(receivedPromises);
        if (greatestPromise.lastAcceptedValue != null) {
            return greatestPromise.lastAcceptedValue;
        }

        return value;
    }

    /**
     * Executes phase two of paxos (see
     * http://en.wikipedia.org/wiki/Paxos_(computer_science)#Basic_Paxos)
     *
     * @param seq the number identifying this instance of paxos
     * @param pid the id of the proposal currently being considered
     * @param val the value agree on in phase one of paxos
     * @throws PaxosRoundFailureException if quorum cannot be reached in this phase
     */
    private void collectAcceptancesForProposal(final long seq, PaxosProposalId pid, PaxosValue val)
            throws PaxosRoundFailureException {
        final PaxosProposal proposal = new PaxosProposal(pid, val);
        List<PaxosResponse> responses = PaxosQuorumChecker.<PaxosAcceptor, PaxosResponse> collectQuorumResponses(
                allAcceptors,
                new Function<PaxosAcceptor, PaxosResponse>() {
                    @Override
                    @Nullable
                    public PaxosResponse apply(@Nullable PaxosAcceptor acceptor) {
                        return acceptor.accept(seq, proposal);
                    }
                },
                quorumSize,
                executor,
                PaxosQuorumChecker.DEFAULT_REMOTE_REQUESTS_TIMEOUT_IN_SECONDS);
        if (!PaxosQuorumChecker.hasQuorum(responses, quorumSize)) {
            throw new PaxosRoundFailureException("failed to acquire quorum in paxos phase two");
        }
    }

    private void broadcastLearnedValue(final long seq, final PaxosValue finalValue) {
        for (final PaxosLearner learner : allLearners) {
            // local learner is forced to update synchronously
            if (localLearner == learner) {
                continue;
            }

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        learner.learn(seq, finalValue);
                    } catch (Throwable e) {
                        log.warn("failed to teach learner", e);
                    }
                }
            });
        }

        // force local learner to update
        localLearner.learn(seq, finalValue);
    }

    @Override
    public int getQuorumSize() {
        return quorumSize;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

}
