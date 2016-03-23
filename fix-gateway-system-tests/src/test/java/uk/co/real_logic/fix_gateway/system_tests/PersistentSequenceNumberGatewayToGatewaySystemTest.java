/*
 * Copyright 2015-2016 Real Logic Ltd.
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
package uk.co.real_logic.fix_gateway.system_tests;

import org.junit.Before;
import org.junit.Test;
import uk.co.real_logic.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.library.LibraryConfiguration;
import uk.co.real_logic.fix_gateway.library.SessionConfiguration;

import static org.junit.Assert.assertEquals;
import static uk.co.real_logic.fix_gateway.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public class PersistentSequenceNumberGatewayToGatewaySystemTest extends AbstractGatewayToGatewaySystemTest
{
    @Before
    public void setUp()
    {
        delete(ACCEPTOR_LOGS);
        delete(CLIENT_LOGS);

        launch();
    }

    private void launch()
    {
        mediaDriver = launchMediaDriver();

        acceptingEngine = launchAcceptingEngineWithSameLogs(port, ACCEPTOR_ID, INITIATOR_ID);
        initiatingEngine = launchInitiatingGatewayWithSameLogs(initAeronPort);

        final LibraryConfiguration acceptingLibraryConfig =
            acceptingLibraryConfig(acceptingSessionHandler, ACCEPTOR_ID, INITIATOR_ID, "fix-acceptor");
        acceptingLibraryConfig.acceptorSequenceNumbersResetUponReconnect(false);
        acceptingLibrary = FixLibrary.connect(acceptingLibraryConfig);
        initiatingLibrary = newInitiatingLibrary(initAeronPort, initiatingSessionHandler, 1);

        connectPersistingSessions();
    }

    private void connectPersistingSessions()
    {
        final SessionConfiguration config = SessionConfiguration.builder()
            .address("localhost", port)
            .credentials("bob", "Uv1aegoh")
            .senderCompId(INITIATOR_ID)
            .targetCompId(ACCEPTOR_ID)
            .sequenceNumbersPersistent(true)
            .build();

        initiatedSession = initiatingLibrary.initiate(config, new SleepingIdleStrategy(10));

        assertConnected(initiatedSession);
        sessionLogsOn(initiatingLibrary, acceptingLibrary, initiatedSession);
        acceptingSession = acceptSession(acceptingSessionHandler, acceptingLibrary);
    }

    @Test(timeout = 10_000L)
    public void sequenceNumbersCanPersistOverRestarts()
    {
        sendTestRequest(initiatedSession);
        assertReceivedTestRequest(initiatingLibrary, acceptingLibrary, acceptingOtfAcceptor);
        assertSequenceFromInitToAcceptAt(2);

        final long initiatedSessionId = initiatedSession.id();
        final long acceptingSessionId = acceptingSession.id();

        initiatedSession.startLogout();
        assertSessionsDisconnected();

        close();

        launch();

        assertEquals("initiatedSessionId not stable over restarts", initiatedSessionId, initiatedSession.id());
        assertEquals("acceptingSessionId not stable over restarts", acceptingSessionId, acceptingSession.id());

        sendTestRequest(initiatedSession);
        assertReceivedTestRequest(initiatingLibrary, acceptingLibrary, acceptingOtfAcceptor, 4);
        assertSequenceFromInitToAcceptAt(4);
    }
}
