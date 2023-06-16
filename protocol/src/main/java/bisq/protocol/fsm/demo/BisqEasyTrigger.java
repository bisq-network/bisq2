/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.protocol.fsm.demo;

import bisq.protocol.fsm.Event;
import bisq.protocol.fsm.EventHandler;
import bisq.protocol.fsm.EventType;
import lombok.Getter;
import lombok.ToString;

import java.lang.reflect.InvocationTargetException;

@ToString
@Getter
public enum BisqEasyTrigger implements EventType {
    ON_TAKE_OFFER(TakeOfferEventHandler.class),
    ON_ACCOUNT_SENT(AccountSentEventHandler.class);


    private final Class<? extends EventHandler> eventHandlerClass;

    BisqEasyTrigger(Class<? extends EventHandler> eventHandlerClass) {
        this.eventHandlerClass = eventHandlerClass;
    }

    public EventHandler handle(Event event) {
        try {
            EventHandler handler = eventHandlerClass.getDeclaredConstructor(Event.class).newInstance(event);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return new TakeOfferEventHandler();
    }
}