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

package bisq.network.p2p.services.data.storage;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class Result {
    private final boolean success;
    private boolean requestAlreadyReceived, payloadAlreadyStored, publicKeyHashInvalid, sequenceNrInvalid, signatureInvalid,
            dataInvalid, expired, noEntry, alreadyRemoved, maxMapSizeReached, isSevereFailure;
    private StorageData removedData;

    public Result(boolean success) {
        this.success = success;
    }

    public Result maxMapSizeReached() {
        maxMapSizeReached = true;
        return this;
    }

    public Result publicKeyHashInvalid() {
        publicKeyHashInvalid = true;
        isSevereFailure = true;
        return this;
    }

    public Result requestAlreadyReceived() {
        requestAlreadyReceived = true;
        return this;
    }

    public Result payloadAlreadyStored() {
        payloadAlreadyStored = true;
        return this;
    }

    public Result sequenceNrInvalid() {
        sequenceNrInvalid = true;
        return this;
    }

    public Result signatureInvalid() {
        signatureInvalid = true;
        isSevereFailure = true;
        return this;
    }

    public Result expired() {
        expired = true;
        return this;
    }

    public Result dataInvalid() {
        dataInvalid = true;
        isSevereFailure = true;
        return this;
    }


    public Result noEntry() {
        noEntry = true;
        return this;
    }

    public Result alreadyRemoved() {
        alreadyRemoved = true;
        return this;
    }

    public Result removedData(StorageData removedData) {
        this.removedData = removedData;
        return this;
    }
}
