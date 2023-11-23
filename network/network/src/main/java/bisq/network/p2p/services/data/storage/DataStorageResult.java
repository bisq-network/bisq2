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

@Getter
public class DataStorageResult {
    private final boolean success;
    private boolean requestAlreadyReceived, payloadAlreadyStored, publicKeyHashInvalid, sequenceNrInvalid, signatureInvalid,
            dataInvalid, isNotAuthorized, expired, noEntry, alreadyRemoved, maxMapSizeReached, isSevereFailure, metaDataInvalid;
    private StorageData removedData;

    public DataStorageResult(boolean success) {
        this.success = success;
    }

    public DataStorageResult maxMapSizeReached() {
        maxMapSizeReached = true;
        return this;
    }

    public DataStorageResult publicKeyHashInvalid() {
        publicKeyHashInvalid = true;
        isSevereFailure = true;
        return this;
    }

    public DataStorageResult requestAlreadyReceived() {
        requestAlreadyReceived = true;
        return this;
    }

    public DataStorageResult payloadAlreadyStored() {
        payloadAlreadyStored = true;
        return this;
    }

    public DataStorageResult sequenceNrInvalid() {
        sequenceNrInvalid = true;
        return this;
    }

    public DataStorageResult signatureInvalid() {
        signatureInvalid = true;
        isSevereFailure = true;
        return this;
    }

    public DataStorageResult metaDataInvalid() {
        metaDataInvalid = true;
        return this;
    }

    public DataStorageResult expired() {
        expired = true;
        return this;
    }

    public DataStorageResult dataInvalid() {
        dataInvalid = true;
        isSevereFailure = true;
        return this;
    }

    public DataStorageResult isNotAuthorized() {
        isNotAuthorized = true;
        isSevereFailure = true;
        return this;
    }

    public DataStorageResult noEntry() {
        noEntry = true;
        return this;
    }

    public DataStorageResult alreadyRemoved() {
        alreadyRemoved = true;
        return this;
    }

    public DataStorageResult removedData(StorageData removedData) {
        this.removedData = removedData;
        return this;
    }

    @Override
    public String toString() {
        return "DataStorageResult{" +
                "success=" + success +
                ", requestAlreadyReceived=" + requestAlreadyReceived +
                ", payloadAlreadyStored=" + payloadAlreadyStored +
                ", publicKeyHashInvalid=" + publicKeyHashInvalid +
                ", sequenceNrInvalid=" + sequenceNrInvalid +
                ", signatureInvalid=" + signatureInvalid +
                ", dataInvalid=" + dataInvalid +
                ", isNotAuthorized=" + isNotAuthorized +
                ", expired=" + expired +
                ", noEntry=" + noEntry +
                ", alreadyRemoved=" + alreadyRemoved +
                ", maxMapSizeReached=" + maxMapSizeReached +
                ", isSevereFailure=" + isSevereFailure +
                ", metaDataInvalid=" + metaDataInvalid +
                ", removedData=" + removedData +
                '}';
    }
}
