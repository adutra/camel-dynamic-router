/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;

/**
 * @version 
 */
public class DynamicRouter4Test extends ContextTestSupport {

    /**
     * An abstract view of steps in a workflow.
     */
    public static enum Step {
        STEP_1, STEP_2;
    }

    /**
     * A message in initial state should be forwarded to step 1.
     * @throws Exception
     */
    public void test_msg_should_go_to_step1() throws Exception {

        getMockEndpoint("mock:step1a").expectedMessageCount(1);
        getMockEndpoint("mock:step1b").expectedMessageCount(1);
        getMockEndpoint("mock:step2").expectedMessageCount(0);

        template.sendBody("direct:start", "This message should go to step 1");

        assertMockEndpointsSatisfied();
    }

    /**
     * A message coming from step 1 should be forwarded to step 2.
     * @throws Exception
     */
    public void test_msg_should_go_to_step2() throws Exception {

        getMockEndpoint("mock:step1a").expectedMessageCount(0);
        getMockEndpoint("mock:step1b").expectedMessageCount(0);
        getMockEndpoint("mock:step2").expectedMessageCount(1);

        template.sendBodyAndHeader("direct:start", "This message should go to step 2", "STEP", Step.STEP_1);

        assertMockEndpointsSatisfied();
    }

    /**
     * A message coming from step 2 has reached the end of its lifecycle and should be ignored.
     * @throws Exception
     */
    public void test_msg_should_go_to_nowhere() throws Exception {

        getMockEndpoint("mock:step1a").expectedMessageCount(0);
        getMockEndpoint("mock:step1b").expectedMessageCount(0);
        getMockEndpoint("mock:step2").expectedMessageCount(0);

        template.sendBodyAndHeader("direct:start", "This message should go nowhere", "STEP", Step.STEP_2);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:start")
                // use a bean as the dynamic router
                .dynamicRouter().method(DynamicRouter4Test.class, "nextStep");


            }
        };
    }

    /*
     * Routing algorithm:
     * The message's current state is held by the header Exchange.SLIP_ENDPOINT.
     * 1) if a message has never been seen (no header set), forward to step 1 (mock:step1a and mock:step1b)
     * 2) if the message has already gone through step 1 and is coming back to the router for the 2nd time, forward to step 2 (mock:step2)
     * 3) if the message has already gone through step 2 and is coming back to the router for the 3rd time, return null to signal the end of processing.
     */
    public String nextStep(@Header("STEP") Step previousStep) {

        //messages that have never been previously processed
        if (previousStep == null) {

            //forward to both step1a and step1b
            return "mock:step1a,mock:step1b";

            //messages processed by step1, returning to the router for the 2nd time
        } else if (Step.STEP_1 == previousStep) {

            //forward to step2
            return "mock:step2";

            //messages processed by step2, returning to the router for the 3rd time
        } else if (Step.STEP_2 == previousStep) {

            //end of workflow
            return null;

        } else {

            //cannot handle messages with unexpected origin
            throw new IllegalStateException("Unexpected origin: " + previousStep);
        }

    }

}
