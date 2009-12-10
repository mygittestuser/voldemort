/*
 * Copyright 2009 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.cluster.failuredetector;

import static voldemort.FailureDetectorTestUtils.recordException;
import static voldemort.FailureDetectorTestUtils.recordSuccess;
import static voldemort.MutableStoreResolver.createMutableStoreResolver;
import static voldemort.cluster.failuredetector.FailureDetectorUtils.create;

import org.junit.Test;

import voldemort.cluster.Node;

import com.google.common.collect.Iterables;

public class BannagePeriodFailureDetectorTest extends AbstractFailureDetectorTest {

    @Override
    public FailureDetector setUpFailureDetector() throws Exception {
        FailureDetectorConfig failureDetectorConfig = new FailureDetectorConfig().setImplementationClassName(BannagePeriodFailureDetector.class.getName())
                                                                                 .setBannagePeriod(BANNAGE_MILLIS)
                                                                                 .setNodes(cluster.getNodes())
                                                                                 .setStoreResolver(createMutableStoreResolver(cluster.getNodes()))
                                                                                 .setTime(time);
        return create(failureDetectorConfig);
    }

    @Test
    public void testTimeout() throws Exception {
        Node node = Iterables.get(cluster.getNodes(), 8);

        recordException(failureDetector, node);
        assertUnavailable(node);

        time.sleep(BANNAGE_MILLIS / 2);
        assertUnavailable(node);

        time.sleep((BANNAGE_MILLIS / 2) + 1);
        assertAvailable(node);
    }

    @Test
    public void testCumulativeFailures() throws Exception {
        Node node = Iterables.get(cluster.getNodes(), 8);

        recordException(failureDetector, node);
        assertUnavailable(node);

        time.sleep(BANNAGE_MILLIS / 2);

        // OK, now record another exception
        recordException(failureDetector, node);
        assertUnavailable(node);

        // If it's not cumulative, it would pass after sleeping the rest of the
        // initial period but it's still unavailable...
        time.sleep((BANNAGE_MILLIS / 2) + 1);
        assertUnavailable(node);

        // ...so sleep for the whole bannage period at which point it will
        // become available.
        time.sleep(BANNAGE_MILLIS);
        assertAvailable(node);
    }

    @Test
    public void testForceSuccess() throws Exception {
        Node node = Iterables.get(cluster.getNodes(), 8);

        recordException(failureDetector, node);
        assertUnavailable(node);

        recordSuccess(failureDetector, node, false);
        assertAvailable(node);
    }

}
