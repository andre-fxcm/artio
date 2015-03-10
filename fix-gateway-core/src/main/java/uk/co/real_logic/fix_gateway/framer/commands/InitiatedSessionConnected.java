/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.fix_gateway.framer.commands;

import uk.co.real_logic.fix_gateway.FixGateway;
import uk.co.real_logic.fix_gateway.framer.session.InitiatorSession;

class InitiatedSessionConnected implements FixGatewayCommand
{
    private final InitiatorSession session;

    InitiatedSessionConnected(final InitiatorSession session)
    {
        this.session = session;
    }

    public void execute(final FixGateway gateway)
    {
        gateway.onInitiatorSessionConnected(session);
    }
}
